package com.thefadghost.neverforget.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NeverForgetDatabaseTest {
    private lateinit var database: NeverForgetDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NeverForgetDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun personAndEventRoundTrip() = runTest {
        val personId = database.peopleDao().insert(
            PersonEntity(
                displayName = "Mila Petrova",
                relationship = "MOTHER",
                birthdayMonth = 6,
                birthdayDay = 8,
                birthYear = 1970,
            ),
        )
        database.eventsDao().insert(
            EventEntity(
                title = "Mila's birthday",
                type = "BIRTHDAY",
                startEpochDay = 20612,
                personId = personId,
                recurrenceType = "YEARLY",
                leadDays = 14,
            ),
        )

        assertEquals("Mila Petrova", database.peopleDao().observeAll().first().single().displayName)
        assertEquals(personId, database.eventsDao().observeAll().first().single().personId)
    }

    @Test
    fun occurrenceCompletionIsScopedToOneYear() = runTest {
        database.eventsDao().insert(
            EventEntity(
                id = 4,
                title = "Annual event",
                type = "CUSTOM",
                startEpochDay = 20612,
            ),
        )
        database.occurrenceStateDao().upsert(
            OccurrenceStateEntity(
                eventId = 4,
                occurrenceEpochDay = 20612,
                done = true,
            ),
        )

        assertEquals(true, database.occurrenceStateDao().get(4, 20612)?.done)
        assertEquals(null, database.occurrenceStateDao().get(4, 20977))
    }
}
