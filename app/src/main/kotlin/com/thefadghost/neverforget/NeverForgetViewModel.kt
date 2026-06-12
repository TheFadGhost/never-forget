package com.thefadghost.neverforget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thefadghost.neverforget.backup.BackupManager
import com.thefadghost.neverforget.backup.CsvDecodeResult
import com.thefadghost.neverforget.calendar.CalendarIcsCodec
import com.thefadghost.neverforget.calendar.CalendarImportPlanner
import com.thefadghost.neverforget.calendar.ExistingCalendarEvent
import com.thefadghost.neverforget.calendar.PortableCalendarEvent
import com.thefadghost.neverforget.data.country.BundledCountryRepository
import com.thefadghost.neverforget.data.country.CountryOccurrence
import com.thefadghost.neverforget.data.country.NameDayDefinition
import com.thefadghost.neverforget.database.EventEntity
import com.thefadghost.neverforget.database.FollowedNameDayEntity
import com.thefadghost.neverforget.database.NeverForgetDatabase
import com.thefadghost.neverforget.database.OccurrenceStateEntity
import com.thefadghost.neverforget.database.PersonEntity
import com.thefadghost.neverforget.feature.onboarding.OnboardingSelection
import com.thefadghost.neverforget.model.EventType
import com.thefadghost.neverforget.importing.DeviceImporter
import com.thefadghost.neverforget.importing.ImportSummary
import com.thefadghost.neverforget.model.Relationship
import com.thefadghost.neverforget.model.ThemePreference
import com.thefadghost.neverforget.reminders.ReminderRepairWorker
import com.thefadghost.neverforget.settings.AppSettings
import com.thefadghost.neverforget.settings.SettingsRepository
import java.time.LocalDate
import java.time.MonthDay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalendarImportSummary(
    val imported: Int,
    val skippedDuplicates: Int,
    val errors: List<String>,
)

class NeverForgetViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NeverForgetDatabase.get(application)
    private val countryRepository = BundledCountryRepository()
    private val settingsRepository = SettingsRepository(application)
    private val backupManager = BackupManager(database)
    private val deviceImporter = DeviceImporter(application, database)

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )
    val people = database.peopleDao().observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val events = database.eventsDao().observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val followedNameDays = database.followedNameDayDao().observeEnabled().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val nameDays: List<NameDayDefinition> = countryRepository.nameDays()

    init {
        ReminderRepairWorker.enqueue(application)
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { appSettings ->
                if (appSettings.onboardingCompleted) {
                    materializeCountryEvents(appSettings)
                }
            }
        }
    }

    fun completeOnboarding(selection: OnboardingSelection) {
        viewModelScope.launch {
            settingsRepository.completeOnboarding(
                ukEnabled = selection.ukEnabled,
                bulgariaEnabled = selection.bulgariaEnabled,
                primaryCountry = selection.primaryCountry,
            )
        }
    }

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setCountry(country: String, enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCountry(country, enabled) }
    }

    fun setPrimaryCountry(country: String) {
        viewModelScope.launch { settingsRepository.setPrimaryCountry(country) }
    }

    fun setUkRegion(region: String, enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setUkRegion(region, enabled) }
    }

    fun addPerson(
        name: String,
        relationship: Relationship,
        birthday: MonthDay?,
        birthYear: Int?,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            database.peopleDao().insert(
                PersonEntity(
                    displayName = name.trim(),
                    relationship = relationship.name,
                    birthdayMonth = birthday?.monthValue,
                    birthdayDay = birthday?.dayOfMonth,
                    birthYear = birthYear,
                ),
            )
        }
    }

    fun addEvent(
        title: String,
        type: EventType,
        date: LocalDate,
        personId: Long? = null,
        leadDays: Int = defaultLeadDays(type, title),
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            database.eventsDao().insert(
                EventEntity(
                    title = title.trim(),
                    type = type.name,
                    startEpochDay = date.toEpochDay(),
                    personId = personId,
                    recurrenceType = if (type in yearlyTypes) "YEARLY" else "NONE",
                    leadDays = leadDays,
                ),
            )
            ReminderRepairWorker.enqueue(getApplication())
        }
    }

    fun addBirthday(
        name: String,
        relationship: Relationship,
        birthday: MonthDay,
        birthYear: Int?,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val personId = database.peopleDao().insert(
                PersonEntity(
                    displayName = name.trim(),
                    relationship = relationship.name,
                    birthdayMonth = birthday.monthValue,
                    birthdayDay = birthday.dayOfMonth,
                    birthYear = birthYear,
                ),
            )
            val sourceYear = birthYear ?: 2000
            database.eventsDao().insert(
                EventEntity(
                    title = "${name.trim()}'s birthday",
                    type = EventType.BIRTHDAY.name,
                    startEpochDay = birthday.atYear(sourceYear).toEpochDay(),
                    personId = personId,
                    recurrenceType = "YEARLY",
                    leadDays = 14,
                ),
            )
            ReminderRepairWorker.enqueue(getApplication())
        }
    }

    fun followNameDay(nameDay: NameDayDefinition, follow: Boolean) {
        viewModelScope.launch {
            val eventId = generatedEventId("name-day|${nameDay.canonicalName}")
            if (follow) {
                database.followedNameDayDao().upsert(
                    FollowedNameDayEntity(
                        canonicalName = nameDay.canonicalName,
                        preferredMonth = nameDay.month,
                        preferredDay = nameDay.day,
                    ),
                )
                database.eventsDao().insert(
                    EventEntity(
                        id = eventId,
                        title = "${nameDay.canonicalName}'s Name Day",
                        type = EventType.NAME_DAY.name,
                        startEpochDay = nameDay.date.atYear(LocalDate.now().year).toEpochDay(),
                        recurrenceType = "YEARLY",
                        classification = "CULTURAL",
                        countryCode = "BG",
                        notes = nameDay.localName,
                        leadDays = 7,
                    ),
                )
            } else {
                database.followedNameDayDao().delete(nameDay.canonicalName)
                database.eventsDao().deleteById(eventId)
            }
            ReminderRepairWorker.enqueue(getApplication())
        }
    }

    fun setOccurrenceState(
        eventId: Long,
        date: LocalDate,
        done: Boolean? = null,
        giftBought: Boolean? = null,
        plansMade: Boolean? = null,
    ) {
        viewModelScope.launch {
            val existing = database.occurrenceStateDao().get(eventId, date.toEpochDay())
                ?: OccurrenceStateEntity(eventId, date.toEpochDay())
            database.occurrenceStateDao().upsert(
                existing.copy(
                    done = done ?: existing.done,
                    giftBought = giftBought ?: existing.giftBought,
                    plansMade = plansMade ?: existing.plansMade,
                    acknowledgedAtEpochMillis = if (done == true) System.currentTimeMillis() else existing.acknowledgedAtEpochMillis,
                ),
            )
        }
    }

    fun createBackup(password: String, onResult: (Result<ByteArray>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { backupManager.create(password.toCharArray()) })
        }
    }

    fun restoreBackup(bytes: ByteArray, password: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { backupManager.restore(bytes, password.toCharArray()) }
            if (result.isSuccess) ReminderRepairWorker.enqueue(getApplication())
            onResult(result)
        }
    }

    fun exportPeopleCsv(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch { onResult(runCatching { backupManager.exportPeopleCsv() }) }
    }

    fun importPeopleCsv(csv: String, onResult: (Result<CsvDecodeResult>) -> Unit) {
        viewModelScope.launch { onResult(runCatching { backupManager.importPeopleCsv(csv) }) }
    }

    fun exportCalendarIcs(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            onResult(
                runCatching {
                    CalendarIcsCodec.encode(
                        database.eventsDao().snapshot()
                            .filterNot { it.hidden || it.type == EventType.COUNTRY_OBSERVANCE.name }
                            .map { event ->
                                PortableCalendarEvent(
                                    uid = "never-forget-${event.id}@local",
                                    title = event.title,
                                    date = LocalDate.ofEpochDay(event.startEpochDay),
                                    minuteOfDay = event.startMinuteOfDay,
                                    yearly = event.recurrenceType == "YEARLY",
                                    type = event.type,
                                    notes = event.notes,
                                    leadDays = event.leadDays,
                                )
                            },
                    )
                },
            )
        }
    }

    fun importCalendarIcs(ics: String, onResult: (Result<CalendarImportSummary>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val decoded = CalendarIcsCodec.decode(ics)
                val existing = database.eventsDao().snapshot().map {
                    ExistingCalendarEvent(
                        title = it.title,
                        date = LocalDate.ofEpochDay(it.startEpochDay),
                        minuteOfDay = it.startMinuteOfDay,
                    )
                }
                val plan = CalendarImportPlanner.filterNew(decoded.events, existing)
                plan.events.forEach { event ->
                    val eventType = EventType.entries
                        .firstOrNull { it.name == event.type.uppercase() }
                        ?: EventType.CUSTOM
                    database.eventsDao().insert(
                        EventEntity(
                            title = event.title,
                            type = eventType.name,
                            startEpochDay = event.date.toEpochDay(),
                            startMinuteOfDay = event.minuteOfDay,
                            recurrenceType = if (event.yearly) "YEARLY" else "NONE",
                            notes = event.notes,
                            leadDays = event.leadDays,
                        ),
                    )
                }
                if (plan.events.isNotEmpty()) ReminderRepairWorker.enqueue(getApplication())
                CalendarImportSummary(
                    imported = plan.events.size,
                    skippedDuplicates = plan.skippedDuplicates,
                    errors = decoded.errors,
                )
            }
            onResult(result)
        }
    }

    fun importContactBirthdays(onResult: (Result<ImportSummary>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { deviceImporter.importContactBirthdays() }
            if (result.isSuccess) ReminderRepairWorker.enqueue(getApplication())
            onResult(result)
        }
    }

    fun importDeviceCalendar(onResult: (Result<ImportSummary>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { deviceImporter.importCalendarEvents() }
            if (result.isSuccess) ReminderRepairWorker.enqueue(getApplication())
            onResult(result)
        }
    }

    fun countryOccurrences(year: Int, appSettings: AppSettings = settings.value): List<CountryOccurrence> =
        buildList {
            if (appSettings.ukEnabled) {
                if (appSettings.englandWalesEnabled) addAll(countryRepository.occurrences("GB", "GB-EAW", year))
                if (appSettings.scotlandEnabled) addAll(countryRepository.occurrences("GB", "GB-SCT", year))
                if (appSettings.northernIrelandEnabled) addAll(countryRepository.occurrences("GB", "GB-NIR", year))
            }
            if (appSettings.bulgariaEnabled) addAll(countryRepository.occurrences("BG", null, year))
        }.distinctBy { "${it.eventId}|${it.date}" }.sortedBy { it.date }

    private suspend fun materializeCountryEvents(appSettings: AppSettings) {
        val currentYear = LocalDate.now().year
        val generated = (currentYear..currentYear + 1)
            .flatMap { year -> countryOccurrences(year, appSettings) }
            .map { occurrence ->
                EventEntity(
                    id = generatedEventId(
                        "country|${occurrence.countryCode}|${occurrence.eventId}|${occurrence.date}",
                    ),
                    title = occurrence.name,
                    type = EventType.COUNTRY_OBSERVANCE.name,
                    startEpochDay = occurrence.date.toEpochDay(),
                    recurrenceType = "NONE",
                    classification = occurrence.classification,
                    countryCode = occurrence.countryCode,
                    notes = occurrence.localName.orEmpty(),
                    leadDays = occurrence.leadDays,
                )
            }
        database.eventsDao().replaceCountryEvents(generated)
        ReminderRepairWorker.enqueue(getApplication())
    }

    companion object {
        private val yearlyTypes = setOf(
            EventType.BIRTHDAY,
            EventType.ANNIVERSARY,
            EventType.MEMORIAL,
        )

        private fun defaultLeadDays(type: EventType, title: String): Int = when {
            type == EventType.BIRTHDAY -> 14
            title.contains("Christmas", ignoreCase = true) -> 21
            title.contains("Valentine", ignoreCase = true) -> 21
            else -> 7
        }

        private fun generatedEventId(key: String): Long {
            var hash = -3750763034362895579L
            key.forEach { character ->
                hash = (hash xor character.code.toLong()) * 1099511628211L
            }
            return hash or Long.MIN_VALUE
        }
    }
}
