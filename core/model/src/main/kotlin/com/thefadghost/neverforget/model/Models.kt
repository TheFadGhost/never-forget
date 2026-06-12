package com.thefadghost.neverforget.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.MonthDay

enum class EventType {
    BIRTHDAY,
    ANNIVERSARY,
    MEMORIAL,
    APPOINTMENT,
    TASK,
    REMINDER,
    CUSTOM,
    COUNTRY_OBSERVANCE,
    NAME_DAY,
}

enum class Relationship {
    PARTNER,
    MOTHER,
    FATHER,
    SIBLING,
    CHILD,
    GRANDPARENT,
    FRIEND,
    COLLEAGUE,
    RELATIVE,
    CUSTOM,
}

enum class EventClassification {
    PUBLIC_HOLIDAY,
    RELIGIOUS,
    CULTURAL,
    COMMERCIAL,
    PERSONAL,
}

enum class LeapDayPolicy {
    FEBRUARY_28,
    MARCH_1,
}

enum class ReminderIntensity {
    AGGRESSIVE,
    DAILY,
    OFF,
}

enum class ThemePreference {
    EMBER,
    SAGE,
    COBALT,
    MONOCHROME,
    SYSTEM,
    OLED,
}

data class Person(
    val id: Long = 0,
    val displayName: String,
    val relationship: Relationship = Relationship.FRIEND,
    val customRelationship: String? = null,
    val birthday: MonthDay? = null,
    val birthYear: Int? = null,
    val leapDayPolicy: LeapDayPolicy = LeapDayPolicy.FEBRUARY_28,
    val notes: String = "",
    val contactLookupKey: String? = null,
)

sealed interface RecurrenceRule {
    data object None : RecurrenceRule
    data object Yearly : RecurrenceRule
    data class YearlyNthWeekday(
        val month: Month,
        val ordinal: Int,
        val dayOfWeek: DayOfWeek,
    ) : RecurrenceRule

    data class EasterRelative(val days: Int, val orthodox: Boolean) : RecurrenceRule
}

data class ReminderPolicy(
    val intensity: ReminderIntensity = ReminderIntensity.AGGRESSIVE,
    val leadDays: Int = 7,
    val dailyTime: LocalTime = LocalTime.of(9, 0),
    val finalEveningTime: LocalTime = LocalTime.of(19, 0),
    val quietStart: LocalTime = LocalTime.of(21, 0),
    val quietEnd: LocalTime = LocalTime.of(9, 0),
    val overrideQuietHours: Boolean = false,
)

data class Event(
    val id: Long = 0,
    val title: String,
    val type: EventType,
    val date: LocalDate,
    val recurrence: RecurrenceRule = RecurrenceRule.None,
    val personId: Long? = null,
    val classification: EventClassification = EventClassification.PERSONAL,
    val countryCode: String? = null,
    val regionCode: String? = null,
    val notes: String = "",
    val giftIdeas: String = "",
    val giftBudgetMinor: Long? = null,
    val link: String = "",
    val address: String = "",
    val reminderPolicy: ReminderPolicy = ReminderPolicy(),
    val hidden: Boolean = false,
)

data class EventOccurrence(
    val eventId: Long,
    val date: LocalDate,
    val title: String,
    val done: Boolean = false,
    val giftBought: Boolean = false,
    val plansMade: Boolean = false,
)

data class SourceProvenance(
    val authority: String,
    val url: String,
    val verifiedOn: LocalDate,
    val notes: String = "",
)

