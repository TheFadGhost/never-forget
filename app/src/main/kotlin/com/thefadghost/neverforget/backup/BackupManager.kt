package com.thefadghost.neverforget.backup

import androidx.room.withTransaction
import com.thefadghost.neverforget.database.EventEntity
import com.thefadghost.neverforget.database.FollowedNameDayEntity
import com.thefadghost.neverforget.database.NeverForgetDatabase
import com.thefadghost.neverforget.database.OccurrenceStateEntity
import com.thefadghost.neverforget.database.PersonEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class BackupPayload(
    val version: Int = 1,
    val people: List<BackupPerson>,
    val events: List<BackupEvent>,
    val occurrenceStates: List<BackupOccurrenceState>,
    val followedNameDays: List<BackupNameDay>,
)

@Serializable
private data class BackupPerson(
    val id: Long,
    val displayName: String,
    val relationship: String,
    val customRelationship: String?,
    val birthdayMonth: Int?,
    val birthdayDay: Int?,
    val birthYear: Int?,
    val leapDayPolicy: String,
    val notes: String,
)

@Serializable
private data class BackupEvent(
    val id: Long,
    val title: String,
    val type: String,
    val startEpochDay: Long,
    val startMinuteOfDay: Int?,
    val personId: Long?,
    val recurrenceType: String,
    val classification: String,
    val countryCode: String?,
    val regionCode: String?,
    val notes: String,
    val giftIdeas: String,
    val giftBudgetMinor: Long?,
    val link: String,
    val address: String,
    val reminderIntensity: String,
    val leadDays: Int,
    val dailyMinute: Int,
    val eveningMinute: Int,
    val overrideQuietHours: Boolean,
    val hidden: Boolean,
)

@Serializable
private data class BackupOccurrenceState(
    val eventId: Long,
    val occurrenceEpochDay: Long,
    val done: Boolean,
    val giftBought: Boolean,
    val plansMade: Boolean,
    val acknowledgedAtEpochMillis: Long?,
)

@Serializable
private data class BackupNameDay(
    val canonicalName: String,
    val personId: Long?,
    val preferredMonth: Int,
    val preferredDay: Int,
    val enabled: Boolean,
)

class BackupManager(private val database: NeverForgetDatabase) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun create(password: CharArray): ByteArray {
        val payload = BackupPayload(
            people = database.peopleDao().snapshot().map {
                BackupPerson(
                    it.id,
                    it.displayName,
                    it.relationship,
                    it.customRelationship,
                    it.birthdayMonth,
                    it.birthdayDay,
                    it.birthYear,
                    it.leapDayPolicy,
                    it.notes,
                )
            },
            events = database.eventsDao().snapshot().map {
                BackupEvent(
                    it.id,
                    it.title,
                    it.type,
                    it.startEpochDay,
                    it.startMinuteOfDay,
                    it.personId,
                    it.recurrenceType,
                    it.classification,
                    it.countryCode,
                    it.regionCode,
                    it.notes,
                    it.giftIdeas,
                    it.giftBudgetMinor,
                    it.link,
                    it.address,
                    it.reminderIntensity,
                    it.leadDays,
                    it.dailyMinute,
                    it.eveningMinute,
                    it.overrideQuietHours,
                    it.hidden,
                )
            },
            occurrenceStates = database.occurrenceStateDao().snapshot().map {
                BackupOccurrenceState(
                    it.eventId,
                    it.occurrenceEpochDay,
                    it.done,
                    it.giftBought,
                    it.plansMade,
                    it.acknowledgedAtEpochMillis,
                )
            },
            followedNameDays = database.followedNameDayDao().snapshot().map {
                BackupNameDay(
                    it.canonicalName,
                    it.personId,
                    it.preferredMonth,
                    it.preferredDay,
                    it.enabled,
                )
            },
        )
        return BackupCrypto.encrypt(json.encodeToString(payload).encodeToByteArray(), password)
    }

    suspend fun restore(encrypted: ByteArray, password: CharArray) {
        val payload = json.decodeFromString<BackupPayload>(
            BackupCrypto.decrypt(encrypted, password).decodeToString(),
        )
        require(payload.version == 1) { "Unsupported backup version ${payload.version}" }
        database.withTransaction {
            database.occurrenceStateDao().clear()
            database.followedNameDayDao().clear()
            database.eventsDao().clear()
            database.peopleDao().clear()
            database.peopleDao().insertAll(payload.people.map {
                PersonEntity(
                    id = it.id,
                    displayName = it.displayName,
                    relationship = it.relationship,
                    customRelationship = it.customRelationship,
                    birthdayMonth = it.birthdayMonth,
                    birthdayDay = it.birthdayDay,
                    birthYear = it.birthYear,
                    leapDayPolicy = it.leapDayPolicy,
                    notes = it.notes,
                )
            })
            database.eventsDao().insertAll(payload.events.map {
                EventEntity(
                    id = it.id,
                    title = it.title,
                    type = it.type,
                    startEpochDay = it.startEpochDay,
                    startMinuteOfDay = it.startMinuteOfDay,
                    personId = it.personId,
                    recurrenceType = it.recurrenceType,
                    classification = it.classification,
                    countryCode = it.countryCode,
                    regionCode = it.regionCode,
                    notes = it.notes,
                    giftIdeas = it.giftIdeas,
                    giftBudgetMinor = it.giftBudgetMinor,
                    link = it.link,
                    address = it.address,
                    reminderIntensity = it.reminderIntensity,
                    leadDays = it.leadDays,
                    dailyMinute = it.dailyMinute,
                    eveningMinute = it.eveningMinute,
                    overrideQuietHours = it.overrideQuietHours,
                    hidden = it.hidden,
                )
            })
            database.occurrenceStateDao().upsertAll(payload.occurrenceStates.map {
                OccurrenceStateEntity(
                    it.eventId,
                    it.occurrenceEpochDay,
                    it.done,
                    it.giftBought,
                    it.plansMade,
                    it.acknowledgedAtEpochMillis,
                )
            })
            payload.followedNameDays.forEach {
                database.followedNameDayDao().upsert(
                    FollowedNameDayEntity(
                        it.canonicalName,
                        it.personId,
                        it.preferredMonth,
                        it.preferredDay,
                        it.enabled,
                    ),
                )
            }
        }
    }

    suspend fun exportPeopleCsv(): String = PeopleCsvCodec.encode(
        database.peopleDao().snapshot().mapNotNull {
            val month = it.birthdayMonth
            val day = it.birthdayDay
            if (month == null || day == null) {
                null
            } else {
                CsvPerson(it.displayName, it.relationship, month, day, it.birthYear)
            }
        },
    )

    suspend fun importPeopleCsv(csv: String): CsvDecodeResult {
        val result = PeopleCsvCodec.decode(csv)
        val existing = database.peopleDao().snapshot()
            .map { "${it.normalizedName}|${it.birthdayMonth}|${it.birthdayDay}" }
            .toSet()
        result.validRows.forEach {
            val key = "${it.name.trim().lowercase()}|${it.month}|${it.day}"
            if (key !in existing) {
                database.peopleDao().insert(
                    PersonEntity(
                        displayName = it.name,
                        relationship = it.relationship,
                        birthdayMonth = it.month,
                        birthdayDay = it.day,
                        birthYear = it.year,
                    ),
                )
            }
        }
        return result
    }
}

