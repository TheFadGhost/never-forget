package com.thefadghost.neverforget.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PeopleDao {
    @Insert suspend fun insert(person: PersonEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(people: List<PersonEntity>)
    @Update suspend fun update(person: PersonEntity)
    @Delete suspend fun delete(person: PersonEntity)
    @Query("SELECT * FROM people ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<PersonEntity>>
    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun get(id: Long): PersonEntity?
    @Query("SELECT * FROM people ORDER BY id")
    suspend fun snapshot(): List<PersonEntity>
    @Query("DELETE FROM people")
    suspend fun clear()
}

@Dao
interface EventsDao {
    @Insert suspend fun insert(event: EventEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)
    @Update suspend fun update(event: EventEntity)
    @Delete suspend fun delete(event: EventEntity)
    @Query("SELECT * FROM events ORDER BY startEpochDay, title COLLATE NOCASE")
    fun observeAll(): Flow<List<EventEntity>>
    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun get(id: Long): EventEntity?
    @Query("SELECT * FROM events WHERE normalizedTitle LIKE '%' || :query || '%' ORDER BY startEpochDay")
    fun search(query: String): Flow<List<EventEntity>>
    @Query("SELECT * FROM events ORDER BY startEpochDay")
    suspend fun snapshot(): List<EventEntity>
    @Query("DELETE FROM events WHERE type = 'COUNTRY_OBSERVANCE'")
    suspend fun clearCountryEvents()
    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: Long)
    @Transaction
    suspend fun replaceCountryEvents(events: List<EventEntity>) {
        clearCountryEvents()
        insertAll(events)
    }
    @Query("DELETE FROM events")
    suspend fun clear()
}

@Dao
interface OccurrenceStateDao {
    @Upsert suspend fun upsert(state: OccurrenceStateEntity)
    @Upsert suspend fun upsertAll(states: List<OccurrenceStateEntity>)
    @Query("SELECT * FROM occurrence_state WHERE eventId = :eventId AND occurrenceEpochDay = :epochDay")
    suspend fun get(eventId: Long, epochDay: Long): OccurrenceStateEntity?
    @Query("SELECT * FROM occurrence_state WHERE occurrenceEpochDay BETWEEN :start AND :end")
    fun observeRange(start: Long, end: Long): Flow<List<OccurrenceStateEntity>>
    @Query("SELECT * FROM occurrence_state")
    suspend fun snapshot(): List<OccurrenceStateEntity>
    @Query("DELETE FROM occurrence_state")
    suspend fun clear()
}

@Dao
interface ChecklistDao {
    @Upsert suspend fun upsert(item: ChecklistItemEntity): Long
    @Delete suspend fun delete(item: ChecklistItemEntity)
    @Query("SELECT * FROM checklist_items WHERE eventId = :eventId ORDER BY position, id")
    fun observeForEvent(eventId: Long): Flow<List<ChecklistItemEntity>>
}

@Dao
interface FollowedNameDayDao {
    @Upsert suspend fun upsert(item: FollowedNameDayEntity)
    @Query("DELETE FROM followed_name_days WHERE canonicalName = :canonicalName")
    suspend fun delete(canonicalName: String)
    @Query("SELECT * FROM followed_name_days WHERE enabled = 1 ORDER BY canonicalName COLLATE NOCASE")
    fun observeEnabled(): Flow<List<FollowedNameDayEntity>>
    @Query("SELECT * FROM followed_name_days")
    suspend fun snapshot(): List<FollowedNameDayEntity>
    @Query("DELETE FROM followed_name_days")
    suspend fun clear()
}

@Dao
interface ScheduledReminderDao {
    @Upsert suspend fun upsertAll(items: List<ScheduledReminderEntity>)
    @Query("SELECT * FROM scheduled_reminders WHERE eventId = :eventId")
    suspend fun forEvent(eventId: Long): List<ScheduledReminderEntity>
    @Query("SELECT * FROM scheduled_reminders")
    suspend fun snapshot(): List<ScheduledReminderEntity>
    @Query("DELETE FROM scheduled_reminders WHERE eventId = :eventId")
    suspend fun deleteForEvent(eventId: Long)
    @Query("DELETE FROM scheduled_reminders WHERE triggerAtEpochMillis < :before")
    suspend fun deleteBefore(before: Long)
    @Query("SELECT * FROM scheduled_reminders WHERE triggerAtEpochMillis >= :from ORDER BY triggerAtEpochMillis")
    suspend fun upcoming(from: Long): List<ScheduledReminderEntity>
}

@Dao
interface CountrySelectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(selection: CountrySelectionEntity)
    @Query("SELECT * FROM country_selections WHERE enabled = 1 ORDER BY countryCode, regionCode")
    fun observeEnabled(): Flow<List<CountrySelectionEntity>>
}
