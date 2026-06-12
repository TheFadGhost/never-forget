package com.thefadghost.neverforget.calendar

import com.thefadghost.neverforget.model.LeapDayPolicy
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.MonthDay
import java.time.Year
import java.time.temporal.TemporalAdjusters

object AgeCalculator {
    fun ageOn(birthday: MonthDay, birthYear: Int?, onDate: LocalDate): Int? {
        if (birthYear == null) return null
        val birthdayThisYear = BirthdayResolver.dateInYear(
            birthday,
            onDate.year,
            LeapDayPolicy.FEBRUARY_28,
        )
        return onDate.year - birthYear - if (onDate < birthdayThisYear) 1 else 0
    }
}

object BirthdayResolver {
    fun dateInYear(
        birthday: MonthDay,
        year: Int,
        leapDayPolicy: LeapDayPolicy,
    ): LocalDate {
        if (birthday == MonthDay.of(2, 29) && !Year.isLeap(year.toLong())) {
            return when (leapDayPolicy) {
                LeapDayPolicy.FEBRUARY_28 -> LocalDate.of(year, 2, 28)
                LeapDayPolicy.MARCH_1 -> LocalDate.of(year, 3, 1)
            }
        }
        return birthday.atYear(year)
    }
}

object EasterCalculator {
    fun gregorian(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    fun orthodox(year: Int): LocalDate {
        val a = year % 4
        val b = year % 7
        val c = year % 19
        val d = (19 * c + 15) % 30
        val e = (2 * a + 4 * b - d + 34) % 7
        val month = (d + e + 114) / 31
        val day = (d + e + 114) % 31 + 1
        val julianDate = LocalDate.of(year, month, day)
        return julianDate.plusDays(julianGregorianOffset(year).toLong())
    }

    private fun julianGregorianOffset(year: Int): Int = year / 100 - year / 400 - 2
}

object ObservanceRules {
    fun motheringSunday(year: Int): LocalDate = EasterCalculator.gregorian(year).minusDays(21)

    fun fathersDay(year: Int): LocalDate = nthWeekday(
        year = year,
        month = Month.JUNE,
        ordinal = 3,
        dayOfWeek = DayOfWeek.SUNDAY,
    )

    fun nthWeekday(
        year: Int,
        month: Month,
        ordinal: Int,
        dayOfWeek: DayOfWeek,
    ): LocalDate {
        require(ordinal in -5..5 && ordinal != 0) { "Ordinal must be between -5 and 5, excluding zero" }
        return if (ordinal > 0) {
            LocalDate.of(year, month, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(ordinal, dayOfWeek))
        } else {
            LocalDate.of(year, month, month.length(Year.isLeap(year.toLong())))
                .with(TemporalAdjusters.lastInMonth(dayOfWeek))
                .minusWeeks((-ordinal - 1).toLong())
        }
    }
}

