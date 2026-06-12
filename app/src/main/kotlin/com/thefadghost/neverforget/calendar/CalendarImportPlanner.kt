package com.thefadghost.neverforget.calendar

import java.time.LocalDate

data class ExistingCalendarEvent(
    val title: String,
    val date: LocalDate,
    val minuteOfDay: Int?,
)

data class CalendarImportPlan(
    val events: List<PortableCalendarEvent>,
    val skippedDuplicates: Int,
)

object CalendarImportPlanner {
    fun filterNew(
        candidates: List<PortableCalendarEvent>,
        existing: List<ExistingCalendarEvent>,
    ): CalendarImportPlan {
        val known = existing.mapTo(mutableSetOf(), ::identity)
        val accepted = buildList {
            candidates.forEach { event ->
                if (known.add(identity(event))) add(event)
            }
        }
        return CalendarImportPlan(
            events = accepted,
            skippedDuplicates = candidates.size - accepted.size,
        )
    }

    private fun identity(event: PortableCalendarEvent): String =
        identity(ExistingCalendarEvent(event.title, event.date, event.minuteOfDay))

    private fun identity(event: ExistingCalendarEvent): String =
        "${event.title.trim().lowercase()}|${event.date}|${event.minuteOfDay.orEmpty()}"

    private fun Int?.orEmpty(): String = this?.toString().orEmpty()
}
