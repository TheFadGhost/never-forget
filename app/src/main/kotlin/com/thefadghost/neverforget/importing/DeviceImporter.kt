package com.thefadghost.neverforget.importing

import android.content.Context
import android.provider.CalendarContract
import android.provider.ContactsContract
import com.thefadghost.neverforget.database.EventEntity
import com.thefadghost.neverforget.database.NeverForgetDatabase
import com.thefadghost.neverforget.database.PersonEntity
import com.thefadghost.neverforget.model.EventType
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportSummary(
    val imported: Int,
    val skippedDuplicates: Int,
    val failed: Int,
)

class DeviceImporter(
    private val context: Context,
    private val database: NeverForgetDatabase,
) {
    suspend fun importContactBirthdays(): ImportSummary = withContext(Dispatchers.IO) {
        val existing = database.peopleDao().snapshot()
            .map { "${it.normalizedName}|${it.birthdayMonth}|${it.birthdayDay}" }
            .toMutableSet()
        var imported = 0
        var duplicates = 0
        var failed = 0
        val projection = arrayOf(
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Event.START_DATE,
            ContactsContract.Data.LOOKUP_KEY,
        )
        val selection =
            "${ContactsContract.Data.MIMETYPE}=? AND ${ContactsContract.CommonDataKinds.Event.TYPE}=?"
        val args = arrayOf(
            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString(),
        )
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            args,
            null,
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE)
            val lookupColumn = cursor.getColumnIndexOrThrow(ContactsContract.Data.LOOKUP_KEY)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn).orEmpty().trim()
                val parsed = parseBirthday(cursor.getString(dateColumn))
                if (name.isBlank() || parsed == null) {
                    failed++
                    continue
                }
                val (monthDay, year) = parsed
                val key = "${name.lowercase()}|${monthDay.monthValue}|${monthDay.dayOfMonth}"
                if (!existing.add(key)) {
                    duplicates++
                    continue
                }
                val personId = database.peopleDao().insert(
                    PersonEntity(
                        displayName = name,
                        relationship = "FRIEND",
                        birthdayMonth = monthDay.monthValue,
                        birthdayDay = monthDay.dayOfMonth,
                        birthYear = year,
                        contactLookupKey = cursor.getString(lookupColumn),
                    ),
                )
                database.eventsDao().insert(
                    EventEntity(
                        title = "$name's birthday",
                        type = EventType.BIRTHDAY.name,
                        startEpochDay = monthDay.atYear(year ?: 2000).toEpochDay(),
                        personId = personId,
                        recurrenceType = "YEARLY",
                        leadDays = 14,
                    ),
                )
                imported++
            }
        }
        ImportSummary(imported, duplicates, failed)
    }

    suspend fun importCalendarEvents(): ImportSummary = withContext(Dispatchers.IO) {
        val existing = database.eventsDao().snapshot()
            .map { "${it.normalizedTitle}|${it.startEpochDay}" }
            .toMutableSet()
        var imported = 0
        var duplicates = 0
        var failed = 0
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.RRULE,
        )
        val selection = "${CalendarContract.Events.DELETED}=0"
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            null,
            "${CalendarContract.Events.DTSTART} ASC",
        )?.use { cursor ->
            val titleColumn = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val startColumn = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val rruleColumn = cursor.getColumnIndexOrThrow(CalendarContract.Events.RRULE)
            while (cursor.moveToNext() && imported < 500) {
                val title = cursor.getString(titleColumn).orEmpty().trim()
                val millis = cursor.getLong(startColumn)
                if (title.isBlank() || millis <= 0) {
                    failed++
                    continue
                }
                val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                val key = "${title.lowercase()}|${date.toEpochDay()}"
                if (!existing.add(key)) {
                    duplicates++
                    continue
                }
                val rrule = cursor.getString(rruleColumn).orEmpty()
                database.eventsDao().insert(
                    EventEntity(
                        title = title,
                        type = EventType.CUSTOM.name,
                        startEpochDay = date.toEpochDay(),
                        recurrenceType = if ("FREQ=YEARLY" in rrule) "YEARLY" else "NONE",
                        leadDays = 7,
                        notes = "Imported once from Android calendar",
                    ),
                )
                imported++
            }
        }
        ImportSummary(imported, duplicates, failed)
    }

    private fun parseBirthday(value: String?): Pair<MonthDay, Int?>? {
        val text = value?.trim().orEmpty()
        val match = Regex("""(?:(\d{4})-)?-?(\d{1,2})-(\d{1,2})""").matchEntire(text) ?: return null
        val year = match.groupValues[1].takeIf { it.isNotBlank() }?.toIntOrNull()
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val day = match.groupValues[3].toIntOrNull() ?: return null
        val monthDay = runCatching { MonthDay.of(month, day) }.getOrNull() ?: return null
        return monthDay to year
    }
}

