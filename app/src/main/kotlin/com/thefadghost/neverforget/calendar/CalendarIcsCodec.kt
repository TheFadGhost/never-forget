package com.thefadghost.neverforget.calendar

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class PortableCalendarEvent(
    val uid: String,
    val title: String,
    val date: LocalDate,
    val minuteOfDay: Int? = null,
    val yearly: Boolean = false,
    val type: String = "CUSTOM",
    val notes: String = "",
    val leadDays: Int = 7,
)

data class CalendarIcsDecodeResult(
    val events: List<PortableCalendarEvent>,
    val errors: List<String>,
)

object CalendarIcsCodec {
    private val dateFormatter = DateTimeFormatter.BASIC_ISO_DATE
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    fun encode(events: List<PortableCalendarEvent>): String {
        val lines = buildList {
            add("BEGIN:VCALENDAR")
            add("VERSION:2.0")
            add("PRODID:-//Never Forget//Calendar Export//EN")
            add("CALSCALE:GREGORIAN")
            events.forEach { event ->
                add("BEGIN:VEVENT")
                add("UID:${escapeText(event.uid)}")
                if (event.minuteOfDay == null) {
                    add("DTSTART;VALUE=DATE:${event.date.format(dateFormatter)}")
                } else {
                    val dateTime = event.date.atStartOfDay().plusMinutes(event.minuteOfDay.toLong())
                    add("DTSTART:${dateTime.format(dateTimeFormatter)}")
                }
                add("SUMMARY:${escapeText(event.title)}")
                if (event.notes.isNotBlank()) add("DESCRIPTION:${escapeText(event.notes)}")
                if (event.yearly) add("RRULE:FREQ=YEARLY")
                add("X-NEVER-FORGET-TYPE:${escapeText(event.type)}")
                add("X-NEVER-FORGET-LEAD-DAYS:${event.leadDays}")
                add("END:VEVENT")
            }
            add("END:VCALENDAR")
        }
        return lines.flatMap(::foldLine).joinToString("\r\n", postfix = "\r\n")
    }

    fun decode(calendar: String): CalendarIcsDecodeResult {
        val events = mutableListOf<PortableCalendarEvent>()
        val errors = mutableListOf<String>()
        val eventLines = mutableListOf<String>()
        var insideEvent = false

        unfoldLines(calendar).forEach { line ->
            when (line.trim().uppercase()) {
                "BEGIN:VEVENT" -> {
                    insideEvent = true
                    eventLines.clear()
                }
                "END:VEVENT" -> {
                    if (insideEvent) {
                        decodeEvent(eventLines).fold(
                            onSuccess = events::add,
                            onFailure = { error ->
                                val uid = propertyValue(eventLines, "UID").orEmpty().ifBlank { "unknown UID" }
                                errors += "Could not import event $uid: ${error.message ?: "invalid event"}"
                            },
                        )
                    }
                    insideEvent = false
                    eventLines.clear()
                }
                else -> if (insideEvent) eventLines += line
            }
        }

        return CalendarIcsDecodeResult(events, errors)
    }

    private fun decodeEvent(lines: List<String>): Result<PortableCalendarEvent> = runCatching {
        val uid = unescapeText(propertyValue(lines, "UID").orEmpty()).ifBlank {
            "never-forget-${lines.hashCode()}"
        }
        val title = unescapeText(requireNotNull(propertyValue(lines, "SUMMARY")) {
            "Missing SUMMARY"
        }).trim()
        require(title.isNotBlank()) { "Empty SUMMARY" }

        val dateProperty = requireNotNull(property(lines, "DTSTART")) { "Missing DTSTART" }
        val rawDate = dateProperty.second.trim().removeSuffix("Z")
        val allDay = dateProperty.first.contains("VALUE=DATE", ignoreCase = true) || rawDate.length == 8
        val dateTime = if (allDay) null else parseDateTime(rawDate)
        val date = if (allDay) {
            LocalDate.parse(rawDate.take(8), dateFormatter)
        } else {
            requireNotNull(dateTime).toLocalDate()
        }
        val minuteOfDay = dateTime?.let { it.hour * 60 + it.minute }
        val yearly = propertyValue(lines, "RRULE")
            ?.split(';')
            ?.any { it.equals("FREQ=YEARLY", ignoreCase = true) }
            ?: false
        val type = unescapeText(propertyValue(lines, "X-NEVER-FORGET-TYPE").orEmpty())
            .ifBlank { "CUSTOM" }
        val notes = unescapeText(propertyValue(lines, "DESCRIPTION").orEmpty())
        val leadDays = propertyValue(lines, "X-NEVER-FORGET-LEAD-DAYS")
            ?.toIntOrNull()
            ?.coerceIn(0, 365)
            ?: 7

        PortableCalendarEvent(
            uid = uid,
            title = title,
            date = date,
            minuteOfDay = minuteOfDay,
            yearly = yearly,
            type = type,
            notes = notes,
            leadDays = leadDays,
        )
    }

    private fun parseDateTime(value: String): LocalDateTime {
        val normalized = when (value.length) {
            13 -> "${value}00"
            else -> value
        }
        return LocalDateTime.parse(normalized, dateTimeFormatter)
    }

    private fun property(lines: List<String>, name: String): Pair<String, String>? =
        lines.firstNotNullOfOrNull { line ->
            val separator = line.indexOf(':')
            if (separator < 0) return@firstNotNullOfOrNull null
            val key = line.substring(0, separator)
            if (key.substringBefore(';').equals(name, ignoreCase = true)) {
                key to line.substring(separator + 1)
            } else {
                null
            }
        }

    private fun propertyValue(lines: List<String>, name: String): String? = property(lines, name)?.second

    private fun unfoldLines(calendar: String): List<String> {
        val unfolded = mutableListOf<String>()
        calendar.replace("\r\n", "\n").replace('\r', '\n').lineSequence().forEach { line ->
            if ((line.startsWith(' ') || line.startsWith('\t')) && unfolded.isNotEmpty()) {
                unfolded[unfolded.lastIndex] += line.drop(1)
            } else {
                unfolded += line
            }
        }
        return unfolded
    }

    private fun foldLine(line: String): List<String> {
        if (line.length <= 75) return listOf(line)
        val parts = mutableListOf<String>()
        var remaining = line
        var first = true
        while (remaining.isNotEmpty()) {
            val width = if (first) 75 else 74
            val part = remaining.take(width)
            parts += if (first) part else " $part"
            remaining = remaining.drop(width)
            first = false
        }
        return parts
    }

    private fun escapeText(value: String): String = buildString {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                ',' -> append("\\,")
                ';' -> append("\\;")
                '\n' -> append("\\n")
                '\r' -> Unit
                else -> append(character)
            }
        }
    }

    private fun unescapeText(value: String): String = buildString {
        var escaped = false
        value.forEach { character ->
            if (escaped) {
                append(if (character == 'n' || character == 'N') '\n' else character)
                escaped = false
            } else if (character == '\\') {
                escaped = true
            } else {
                append(character)
            }
        }
        if (escaped) append('\\')
    }
}
