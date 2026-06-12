package com.thefadghost.neverforget.reminders

import com.thefadghost.neverforget.model.ReminderIntensity
import com.thefadghost.neverforget.model.ReminderPolicy
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class ReminderKind {
    LEAD,
    FINAL_DAYS,
    EVENT_DAY,
    SNOOZE,
}

data class PlannedReminder(
    val eventId: Long,
    val occurrenceDate: LocalDate,
    val at: LocalDateTime,
    val kind: ReminderKind,
) {
    val requestCode: Int = stableRequestCode(eventId, occurrenceDate, at, kind)
}

class ReminderPlanner {
    fun plan(
        eventId: Long,
        occurrenceDate: LocalDate,
        policy: ReminderPolicy,
    ): List<PlannedReminder> {
        if (policy.intensity == ReminderIntensity.OFF) return emptyList()

        val start = occurrenceDate.minusDays(policy.leadDays.toLong())
        return when (policy.intensity) {
            ReminderIntensity.DAILY -> buildList {
                datesBetween(start, occurrenceDate).forEach { date ->
                    add(
                        planned(
                            eventId,
                            occurrenceDate,
                            date.atTime(policy.dailyTime),
                            if (date == occurrenceDate) ReminderKind.EVENT_DAY else ReminderKind.LEAD,
                        ),
                    )
                }
            }

            ReminderIntensity.AGGRESSIVE -> buildList {
                val finalStart = occurrenceDate.minusDays(3)
                datesBetween(start, occurrenceDate.minusDays(1)).forEach { date ->
                    val kind = if (date >= finalStart) ReminderKind.FINAL_DAYS else ReminderKind.LEAD
                    add(planned(eventId, occurrenceDate, date.atTime(policy.dailyTime), kind))
                    if (date >= finalStart) {
                        add(planned(eventId, occurrenceDate, date.atTime(policy.finalEveningTime), kind))
                    }
                }
                generateSequence(LocalTime.of(9, 0)) { time ->
                    time.plusHours(2).takeIf { it <= LocalTime.of(21, 0) }
                }.forEach { time ->
                    add(planned(eventId, occurrenceDate, occurrenceDate.atTime(time), ReminderKind.EVENT_DAY))
                }
            }

            ReminderIntensity.OFF -> emptyList()
        }.distinctBy { it.requestCode }.sortedBy { it.at }
    }

    private fun planned(
        eventId: Long,
        occurrenceDate: LocalDate,
        at: LocalDateTime,
        kind: ReminderKind,
    ) = PlannedReminder(eventId, occurrenceDate, at, kind)

    private fun datesBetween(start: LocalDate, endInclusive: LocalDate): Sequence<LocalDate> =
        if (start > endInclusive) {
            emptySequence()
        } else {
            generateSequence(start) { current ->
                current.plusDays(1).takeIf { it <= endInclusive }
            }
        }
}

enum class SnoozeOption {
    FIFTEEN_MINUTES,
    ONE_HOUR,
    THREE_HOURS,
    TOMORROW_MORNING;

    fun resolve(from: LocalDateTime): LocalDateTime = when (this) {
        FIFTEEN_MINUTES -> from.plusMinutes(15)
        ONE_HOUR -> from.plusHours(1)
        THREE_HOURS -> from.plusHours(3)
        TOMORROW_MORNING -> from.toLocalDate().plusDays(1).atTime(9, 0)
    }
}

private fun stableRequestCode(
    eventId: Long,
    occurrenceDate: LocalDate,
    at: LocalDateTime,
    kind: ReminderKind,
): Int = "$eventId|$occurrenceDate|$at|$kind".hashCode() and Int.MAX_VALUE

