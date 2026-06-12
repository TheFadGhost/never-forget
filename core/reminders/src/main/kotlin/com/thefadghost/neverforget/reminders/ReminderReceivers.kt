package com.thefadghost.neverforget.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.thefadghost.neverforget.database.NeverForgetDatabase
import com.thefadghost.neverforget.database.OccurrenceStateEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        createChannel(context)
        val eventId = intent.getLongExtra(ReminderIntents.EXTRA_EVENT_ID, -1)
        val occurrenceDay = intent.getLongExtra(ReminderIntents.EXTRA_OCCURRENCE_DAY, 0)
        val title = intent.getStringExtra(ReminderIntents.EXTRA_TITLE) ?: "Important date"
        val requestCode = intent.getIntExtra(ReminderIntents.EXTRA_REQUEST_CODE, title.hashCode())
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val open = PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText("This is still unresolved. Open Never Forget to prepare or mark it done.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "This is still unresolved. Open Never Forget to check plans, gifts, and details.",
                ),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(0, "Done", action(context, requestCode, ReminderIntents.ACTION_DONE, eventId, occurrenceDay, title))
            .addAction(0, "Snooze 1h", action(context, requestCode, ReminderIntents.ACTION_SNOOZE, eventId, occurrenceDay, title))
            .addAction(0, "Gift bought", action(context, requestCode, ReminderIntents.ACTION_GIFT, eventId, occurrenceDay, title))
            .addAction(0, "Plans made", action(context, requestCode, ReminderIntents.ACTION_PLANS, eventId, occurrenceDay, title))
            .build()

        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (canNotify) {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
        }
    }

    private fun action(
        context: Context,
        requestCode: Int,
        action: String,
        eventId: Long,
        occurrenceDay: Long,
        title: String,
    ): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderIntents.EXTRA_EVENT_ID, eventId)
            putExtra(ReminderIntents.EXTRA_OCCURRENCE_DAY, occurrenceDay)
            putExtra(ReminderIntents.EXTRA_TITLE, title)
            putExtra(ReminderIntents.EXTRA_REQUEST_CODE, requestCode)
        }
        return PendingIntent.getBroadcast(
            context,
            "$requestCode|$action".hashCode() and Int.MAX_VALUE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Brutal reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Persistent reminders for important dates"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "important_dates"
    }
}

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val eventId = intent.getLongExtra(ReminderIntents.EXTRA_EVENT_ID, -1)
                val occurrenceDay = intent.getLongExtra(ReminderIntents.EXTRA_OCCURRENCE_DAY, 0)
                val requestCode = intent.getIntExtra(ReminderIntents.EXTRA_REQUEST_CODE, 0)
                val database = NeverForgetDatabase.get(context)
                val existing = database.occurrenceStateDao().get(eventId, occurrenceDay)
                    ?: OccurrenceStateEntity(eventId, occurrenceDay)
                when (intent.action) {
                    ReminderIntents.ACTION_DONE -> {
                        database.occurrenceStateDao().upsert(
                            existing.copy(done = true, acknowledgedAtEpochMillis = System.currentTimeMillis()),
                        )
                        database.eventsDao().get(eventId)?.let { event ->
                            AndroidReminderScheduler(context).scheduleEvent(event)
                        }
                    }
                    ReminderIntents.ACTION_GIFT -> database.occurrenceStateDao().upsert(existing.copy(giftBought = true))
                    ReminderIntents.ACTION_PLANS -> database.occurrenceStateDao().upsert(existing.copy(plansMade = true))
                    ReminderIntents.ACTION_SNOOZE -> {
                        val title = intent.getStringExtra(ReminderIntents.EXTRA_TITLE) ?: "Important date"
                        val at = LocalDateTime.now().plusHours(1)
                        AndroidReminderScheduler(context).schedule(
                            PlannedReminder(
                                eventId = eventId,
                                occurrenceDate = LocalDate.ofEpochDay(occurrenceDay),
                                at = at,
                                kind = ReminderKind.SNOOZE,
                            ),
                            title,
                        )
                    }
                }
                if (intent.action == ReminderIntents.ACTION_DONE || intent.action == ReminderIntents.ACTION_SNOOZE) {
                    NotificationManagerCompat.from(context).cancel(requestCode)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

class SystemRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in supportedActions) return
        ReminderRepairWorker.enqueue(context)
    }

    private companion object {
        val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}

class ReminderRepairWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        AndroidReminderScheduler(applicationContext).scheduleAll()
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        fun enqueue(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(
                "repair-reminders",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ReminderRepairWorker>().build(),
            )
            workManager.enqueueUniquePeriodicWork(
                "periodic-repair-reminders",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ReminderRepairWorker>(1, TimeUnit.DAYS).build(),
            )
        }
    }
}
