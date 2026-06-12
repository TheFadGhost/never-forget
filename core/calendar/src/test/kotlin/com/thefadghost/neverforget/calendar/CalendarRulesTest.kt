package com.thefadghost.neverforget.calendar

import com.thefadghost.neverforget.model.LeapDayPolicy
import java.time.LocalDate
import java.time.Month
import java.time.MonthDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarRulesTest {
    @Test
    fun `age is omitted when birth year is missing`() {
        assertNull(AgeCalculator.ageOn(MonthDay.of(6, 8), null, LocalDate.of(2026, 6, 8)))
    }

    @Test
    fun `age increments on birthday`() {
        assertEquals(30, AgeCalculator.ageOn(MonthDay.of(6, 8), 1996, LocalDate.of(2026, 6, 8)))
    }

    @Test
    fun `february 29 birthday falls on february 28 by default`() {
        assertEquals(
            LocalDate.of(2027, 2, 28),
            BirthdayResolver.dateInYear(MonthDay.of(2, 29), 2027, LeapDayPolicy.FEBRUARY_28),
        )
    }

    @Test
    fun `february 29 birthday can fall on march 1`() {
        assertEquals(
            LocalDate.of(2027, 3, 1),
            BirthdayResolver.dateInYear(MonthDay.of(2, 29), 2027, LeapDayPolicy.MARCH_1),
        )
    }

    @Test
    fun `gregorian easter is correct for 2026`() {
        assertEquals(LocalDate.of(2026, 4, 5), EasterCalculator.gregorian(2026))
    }

    @Test
    fun `orthodox easter is correct for 2026`() {
        assertEquals(LocalDate.of(2026, 4, 12), EasterCalculator.orthodox(2026))
    }

    @Test
    fun `mothering sunday is fourth sunday of lent`() {
        assertEquals(LocalDate.of(2026, 3, 15), ObservanceRules.motheringSunday(2026))
    }

    @Test
    fun `fathers day is third sunday of june`() {
        assertEquals(LocalDate.of(2026, 6, 21), ObservanceRules.fathersDay(2026))
    }

    @Test
    fun `nth weekday rule resolves the expected date`() {
        assertEquals(
            LocalDate.of(2026, 8, 31),
            ObservanceRules.nthWeekday(
                year = 2026,
                month = Month.AUGUST,
                ordinal = -1,
                dayOfWeek = java.time.DayOfWeek.MONDAY,
            ),
        )
    }
}
