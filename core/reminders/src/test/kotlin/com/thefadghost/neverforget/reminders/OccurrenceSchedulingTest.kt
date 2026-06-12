package com.thefadghost.neverforget.reminders

import com.thefadghost.neverforget.database.OccurrenceStateEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OccurrenceSchedulingTest {
    @Test
    fun `completed occurrence is not scheduled again`() {
        val completed = OccurrenceStateEntity(
            eventId = 42,
            occurrenceEpochDay = 20_612,
            done = true,
        )

        assertFalse(shouldScheduleOccurrence(completed))
        assertTrue(shouldScheduleOccurrence(completed.copy(done = false)))
        assertTrue(shouldScheduleOccurrence(null))
    }
}
