package com.thefadghost.neverforget.calendar

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarIcsCodecTest {
    @Test
    fun `round trip preserves portable event fields and escapes text`() {
        val event = PortableCalendarEvent(
            uid = "birthday-42",
            title = "Alex, Sam; and family",
            date = LocalDate.of(2026, 8, 14),
            minuteOfDay = 19 * 60 + 30,
            yearly = true,
            type = "BIRTHDAY",
            notes = "Bring cake\\candles\nConfirm venue",
            leadDays = 14,
        )

        val encoded = CalendarIcsCodec.encode(listOf(event))
        val decoded = CalendarIcsCodec.decode(encoded)

        assertTrue(encoded.contains("SUMMARY:Alex\\, Sam\\; and family"))
        assertTrue(encoded.contains("RRULE:FREQ=YEARLY"))
        assertEquals(emptyList<String>(), decoded.errors)
        assertEquals(listOf(event), decoded.events)
    }

    @Test
    fun `decode imports folded all-day events and reports malformed neighbors`() {
        val calendar = """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            UID:valid-1
            DTSTART;VALUE=DATE:20261224
            SUMMARY:Christmas Eve planning with a long
             folded title
            DESCRIPTION:Family plans
            X-NEVER-FORGET-TYPE:REMINDER
            X-NEVER-FORGET-LEAD-DAYS:21
            END:VEVENT
            BEGIN:VEVENT
            UID:broken-1
            SUMMARY:Missing date
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val decoded = CalendarIcsCodec.decode(calendar)

        assertEquals(1, decoded.events.size)
        assertEquals("Christmas Eve planning with a longfolded title", decoded.events.single().title)
        assertEquals(LocalDate.of(2026, 12, 24), decoded.events.single().date)
        assertEquals(null, decoded.events.single().minuteOfDay)
        assertEquals(21, decoded.events.single().leadDays)
        assertEquals(1, decoded.errors.size)
        assertTrue(decoded.errors.single().contains("broken-1"))
    }

    @Test
    fun `decode accepts UTC date time and converts it to a stable local minute`() {
        val calendar = """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            UID:utc-1
            DTSTART:20261105T093000Z
            SUMMARY:Dentist
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val event = CalendarIcsCodec.decode(calendar).events.single()

        assertEquals(LocalDate.of(2026, 11, 5), event.date)
        assertEquals(9 * 60 + 30, event.minuteOfDay)
        assertEquals("CUSTOM", event.type)
        assertEquals(7, event.leadDays)
    }
}
