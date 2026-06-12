package com.thefadghost.neverforget.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PersonEntity::class,
        EventEntity::class,
        OccurrenceStateEntity::class,
        ChecklistItemEntity::class,
        FollowedNameDayEntity::class,
        ScheduledReminderEntity::class,
        CountrySelectionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NeverForgetDatabase : RoomDatabase() {
    abstract fun peopleDao(): PeopleDao
    abstract fun eventsDao(): EventsDao
    abstract fun occurrenceStateDao(): OccurrenceStateDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun followedNameDayDao(): FollowedNameDayDao
    abstract fun scheduledReminderDao(): ScheduledReminderDao
    abstract fun countrySelectionDao(): CountrySelectionDao

    companion object {
        @Volatile private var instance: NeverForgetDatabase? = null

        fun get(context: Context): NeverForgetDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                NeverForgetDatabase::class.java,
                "never-forget.db",
            ).build().also { instance = it }
        }
    }
}

