package com.thefadghost.neverforget.calendar

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarImportPlannerTest {
    @Test
    fun `filter new removes existing and repeated events using normalized identity`() {
        val date = LocalDate.of(2026, 10, 3)
        val candidates = listOf(
            PortableCalendarEvent("1", " Dentist ", date, 9 * 60),
            PortableCalendarEvent("2", "dentist", date, 9 * 60),
            PortableCalendarEvent("3", "Dentist", date, 10 * 60),
            PortableCalendarEvent("4", "Lunch", date, 12 * 60),
        )
        val existing = listOf(
            ExistingCalendarEvent("DENTIST", date, 9 * 60),
        )

        val result = CalendarImportPlanner.filterNew(candidates, existing)

        assertEquals(listOf("3", "4"), result.events.map { it.uid })
        assertEquals(2, result.skippedDuplicates)
    }
}
