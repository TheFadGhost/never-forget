package com.thefadghost.neverforget.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.thefadghost.neverforget.database.EventEntity
import com.thefadghost.neverforget.database.NeverForgetDatabase
import com.thefadghost.neverforget.database.OccurrenceStateEntity
import com.thefadghost.neverforget.database.ScheduledReminderEntity
import com.thefadghost.neverforget.model.ReminderIntensity
import com.thefadghost.neverforget.model.ReminderPolicy
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class AndroidReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val database = NeverForgetDatabase.get(context)
    private val planner = ReminderPlanner()

    suspend fun scheduleEvent(event: EventEntity, from: LocalDateTime = LocalDateTime.now()) {
        database.scheduledReminderDao().forEvent(event.id).forEach {
            cancel(it.requestCode)
            NotificationManagerCompat.from(context).cancel(it.requestCode)
        }
        database.scheduledReminderDao().deleteForEvent(event.id)
        val occurrence = nextOccurrence(event, from.toLocalDate())
        val occurrenceState = database.occurrenceStateDao().get(event.id, occurrence.toEpochDay())
        if (!shouldScheduleOccurrence(occurrenceState)) return
        val policy = ReminderPolicy(
            intensity = runCatching { ReminderIntensity.valueOf(event.reminderIntensity) }
                .getOrDefault(ReminderIntensity.AGGRESSIVE),
            leadDays = event.leadDays,
            dailyTime = minuteToTime(event.dailyMinute),
            finalEveningTime = minuteToTime(event.eveningMinute),
            overrideQuietHours = event.overrideQuietHours,
        )
        val horizon = from.plusDays(90)
        val reminders = planner.plan(event.id, occurrence, policy)
            .filter { it.at >= from && it.at <= horizon }

        val stored = reminders.map { reminder ->
            schedule(
                reminder = reminder,
                title = event.title,
            )
            ScheduledReminderEntity(
                requestCode = reminder.requestCode,
                eventId = event.id,
                occurrenceEpochDay = occurrence.toEpochDay(),
                triggerAtEpochMillis = reminder.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                kind = reminder.kind.name,
            )
        }
        database.scheduledReminderDao().upsertAll(stored)
    }

    suspend fun scheduleAll(from: LocalDateTime = LocalDateTime.now()) {
        val events = database.eventsDao().snapshot()
        val activeEventIds = events.mapTo(hashSetOf()) { it.id }
        database.scheduledReminderDao().snapshot()
            .filter { it.eventId !in activeEventIds }
            .groupBy { it.eventId }
            .forEach { (eventId, reminders) ->
                reminders.forEach { cancel(it.requestCode) }
                database.scheduledReminderDao().deleteForEvent(eventId)
            }
        database.scheduledReminderDao().deleteBefore(System.currentTimeMillis())
        events.forEach { scheduleEvent(it, from) }
    }

    fun schedule(reminder: PlannedReminder, title: String) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderIntents.ACTION_FIRE
            putExtra(ReminderIntents.EXTRA_EVENT_ID, reminder.eventId)
            putExtra(ReminderIntents.EXTRA_OCCURRENCE_DAY, reminder.occurrenceDate.toEpochDay())
            putExtra(ReminderIntents.EXTRA_TITLE, title)
            putExtra(ReminderIntents.EXTRA_REQUEST_CODE, reminder.requestCode)
            putExtra(ReminderIntents.EXTRA_KIND, reminder.kind.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val trigger = reminder.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        }
    }

    private fun cancel(requestCode: Int) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderIntents.ACTION_FIRE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun nextOccurrence(event: EventEntity, today: LocalDate): LocalDate {
        val source = LocalDate.ofEpochDay(event.startEpochDay)
        if (event.recurrenceType != "YEARLY") return source
        val thisYear = safeDate(today.year, source.monthValue, source.dayOfMonth)
        return if (thisYear >= today) thisYear else safeDate(today.year + 1, source.monthValue, source.dayOfMonth)
    }

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate =
        runCatching { LocalDate.of(year, month, day) }.getOrElse { LocalDate.of(year, 2, 28) }

    private fun minuteToTime(minute: Int): LocalTime =
        LocalTime.of((minute / 60).coerceIn(0, 23), (minute % 60).coerceIn(0, 59))
}

internal fun shouldScheduleOccurrence(state: OccurrenceStateEntity?): Boolean = state?.done != true

internal object ReminderIntents {
    const val ACTION_FIRE = "com.thefadghost.neverforget.reminders.FIRE"
    const val ACTION_DONE = "com.thefadghost.neverforget.reminders.DONE"
    const val ACTION_SNOOZE = "com.thefadghost.neverforget.reminders.SNOOZE"
    const val ACTION_GIFT = "com.thefadghost.neverforget.reminders.GIFT"
    const val ACTION_PLANS = "com.thefadghost.neverforget.reminders.PLANS"
    const val EXTRA_EVENT_ID = "event_id"
    const val EXTRA_OCCURRENCE_DAY = "occurrence_day"
    const val EXTRA_TITLE = "title"
    const val EXTRA_REQUEST_CODE = "request_code"
    const val EXTRA_KIND = "kind"
}
