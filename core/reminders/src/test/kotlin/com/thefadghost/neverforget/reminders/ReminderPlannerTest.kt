package com.thefadghost.neverforget.reminders

import com.thefadghost.neverforget.model.ReminderIntensity
import com.thefadghost.neverforget.model.ReminderPolicy
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlannerTest {
    private val planner = ReminderPlanner()
    private val eventDate = LocalDate.of(2026, 6, 21)

    @Test
    fun `aggressive plan starts at lead date and escalates for final three days`() {
        val plan = planner.plan(
            eventId = 42,
            occurrenceDate = eventDate,
            policy = ReminderPolicy(leadDays = 14),
        )

        assertTrue(plan.any { it.at == LocalDateTime.of(2026, 6, 7, 9, 0) })
        assertTrue(plan.any { it.at == LocalDateTime.of(2026, 6, 18, 19, 0) })
        assertTrue(plan.any { it.at == LocalDateTime.of(2026, 6, 20, 19, 0) })
    }

    @Test
    fun `event day repeats every two hours through nine pm`() {
        val eventDay = planner.plan(42, eventDate, ReminderPolicy())
            .filter { it.kind == ReminderKind.EVENT_DAY }
            .map { it.at.toLocalTime() }

        assertEquals(
            listOf(
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                LocalTime.of(13, 0),
                LocalTime.of(15, 0),
                LocalTime.of(17, 0),
                LocalTime.of(19, 0),
                LocalTime.of(21, 0),
            ),
            eventDay,
        )
    }

    @Test
    fun `daily intensity omits evening and repeated event day alerts`() {
        val plan = planner.plan(
            42,
            eventDate,
            ReminderPolicy(intensity = ReminderIntensity.DAILY),
        )

        assertTrue(plan.all { it.at.toLocalTime() == LocalTime.of(9, 0) })
        assertEquals(1, plan.count { it.kind == ReminderKind.EVENT_DAY })
    }

    @Test
    fun `off intensity produces no reminders`() {
        assertTrue(
            planner.plan(
                42,
                eventDate,
                ReminderPolicy(intensity = ReminderIntensity.OFF),
            ).isEmpty(),
        )
    }

    @Test
    fun `alarm identifiers are stable and distinct`() {
        val plan = planner.plan(42, eventDate, ReminderPolicy())
        assertEquals(plan.size, plan.map { it.requestCode }.distinct().size)
        assertEquals(plan.first().requestCode, planner.plan(42, eventDate, ReminderPolicy()).first().requestCode)
    }

    @Test
    fun `snooze tomorrow morning resolves to nine am`() {
        assertEquals(
            LocalDateTime.of(2026, 6, 19, 9, 0),
            SnoozeOption.TOMORROW_MORNING.resolve(LocalDateTime.of(2026, 6, 18, 19, 20)),
        )
    }
}

