package com.thefadghost.neverforget.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "people")
data class PersonEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val normalizedName: String = displayName.trim().lowercase(),
    val relationship: String,
    val customRelationship: String? = null,
    val birthdayMonth: Int? = null,
    val birthdayDay: Int? = null,
    val birthYear: Int? = null,
    val leapDayPolicy: String = "FEBRUARY_28",
    val notes: String = "",
    val contactLookupKey: String? = null,
)

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("personId"), Index("startEpochDay"), Index("type")],
)
data class EventEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val normalizedTitle: String = title.trim().lowercase(),
    val type: String,
    val startEpochDay: Long,
    val startMinuteOfDay: Int? = null,
    val personId: Long? = null,
    val recurrenceType: String = "NONE",
    val recurrenceMonth: Int? = null,
    val recurrenceOrdinal: Int? = null,
    val recurrenceWeekday: Int? = null,
    val recurrenceOffsetDays: Int = 0,
    val classification: String = "PERSONAL",
    val countryCode: String? = null,
    val regionCode: String? = null,
    val notes: String = "",
    val giftIdeas: String = "",
    val giftBudgetMinor: Long? = null,
    val link: String = "",
    val address: String = "",
    val reminderIntensity: String = "AGGRESSIVE",
    val leadDays: Int = 7,
    val dailyMinute: Int = 540,
    val eveningMinute: Int = 1140,
    val overrideQuietHours: Boolean = false,
    val hidden: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "occurrence_state",
    primaryKeys = ["eventId", "occurrenceEpochDay"],
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("eventId")],
)
data class OccurrenceStateEntity(
    val eventId: Long,
    val occurrenceEpochDay: Long,
    val done: Boolean = false,
    val giftBought: Boolean = false,
    val plansMade: Boolean = false,
    val acknowledgedAtEpochMillis: Long? = null,
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("eventId")],
)
data class ChecklistItemEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val label: String,
    val checked: Boolean = false,
    val position: Int = 0,
)

@Entity(
    tableName = "followed_name_days",
    indices = [Index("personId")],
)
data class FollowedNameDayEntity(
    @androidx.room.PrimaryKey val canonicalName: String,
    val personId: Long? = null,
    val preferredMonth: Int,
    val preferredDay: Int,
    val enabled: Boolean = true,
)

@Entity(
    tableName = "scheduled_reminders",
    indices = [Index("eventId"), Index("triggerAtEpochMillis")],
)
data class ScheduledReminderEntity(
    @androidx.room.PrimaryKey val requestCode: Int,
    val eventId: Long,
    val occurrenceEpochDay: Long,
    val triggerAtEpochMillis: Long,
    val kind: String,
    val status: String = "SCHEDULED",
)

@Entity(tableName = "country_selections")
data class CountrySelectionEntity(
    @androidx.room.PrimaryKey val id: String,
    val countryCode: String,
    val regionCode: String? = null,
    val enabled: Boolean = true,
)

