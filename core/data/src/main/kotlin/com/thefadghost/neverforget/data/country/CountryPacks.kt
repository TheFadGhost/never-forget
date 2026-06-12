package com.thefadghost.neverforget.data.country

import com.thefadghost.neverforget.calendar.EasterCalculator
import com.thefadghost.neverforget.calendar.ObservanceRules
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.MonthDay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CountryPack(
    val id: String,
    val countryCode: String,
    val version: String,
    val displayName: String,
    val regions: List<CountryRegion> = emptyList(),
    val events: List<CountryEventDefinition>,
)

@Serializable
data class CountryRegion(
    val code: String,
    val name: String,
)

@Serializable
data class CountryEventDefinition(
    val id: String,
    val name: String,
    val localName: String? = null,
    val classification: String,
    val regions: List<String> = emptyList(),
    val leadDays: Int = 7,
    val rule: DateRuleDefinition,
    val source: CountrySource,
)

@Serializable
data class DateRuleDefinition(
    val type: String,
    val month: Int? = null,
    val day: Int? = null,
    val ordinal: Int? = null,
    val weekday: String? = null,
    val offsetDays: Int = 0,
    val substitute: String = "NONE",
)

@Serializable
data class CountrySource(
    val authority: String,
    val url: String,
    val verifiedOn: String,
)

@Serializable
data class NameDayDefinition(
    val canonicalName: String,
    val localName: String,
    val month: Int,
    val day: Int,
    val aliases: List<String> = emptyList(),
    val source: CountrySource,
) {
    val date: MonthDay get() = MonthDay.of(month, day)
}

data class CountryOccurrence(
    val eventId: String,
    val countryCode: String,
    val name: String,
    val localName: String?,
    val date: LocalDate,
    val classification: String,
    val leadDays: Int,
    val isSubstitute: Boolean = false,
)

class BundledCountryRepository(
    private val loader: (String) -> String = ::readBundledResource,
) {
    private val json = Json { ignoreUnknownKeys = false }
    private val loadedPacks by lazy {
        listOf(
            json.decodeFromString<CountryPack>(loader("countries/uk.json")),
            json.decodeFromString<CountryPack>(loader("countries/bg.json")),
        )
    }
    private val loadedNameDays by lazy {
        json.decodeFromString<List<NameDayDefinition>>(loader("countries/bg_name_days.json"))
    }

    fun packs(): List<CountryPack> = loadedPacks

    fun nameDays(): List<NameDayDefinition> = loadedNameDays

    fun occurrences(countryCode: String, regionCode: String?, year: Int): List<CountryOccurrence> {
        val pack = loadedPacks.single { it.countryCode == countryCode }
        val base = pack.events
            .asSequence()
            .filter { it.regions.isEmpty() || regionCode in it.regions }
            .map { event ->
                CountryOccurrence(
                    eventId = event.id,
                    countryCode = countryCode,
                    name = event.name,
                    localName = event.localName,
                    date = resolve(event.rule, year),
                    classification = event.classification,
                    leadDays = event.leadDays,
                ) to event
            }
            .sortedBy { it.first.date }
            .toList()

        val occupied = base.mapTo(linkedSetOf()) { it.first.date }
        val result = base.mapTo(mutableListOf()) { it.first }
        base.forEach { (occurrence, event) ->
            if (event.rule.substitute == "NEXT_WEEKDAY" &&
                occurrence.date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            ) {
                var substitute = occurrence.date.plusDays(1)
                while (substitute.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) ||
                    substitute in occupied
                ) {
                    substitute = substitute.plusDays(1)
                }
                occupied += substitute
                result += occurrence.copy(
                    eventId = "${event.id}-substitute",
                    name = "${event.name} (substitute day)",
                    date = substitute,
                    isSubstitute = true,
                )
            }
        }
        return result.sortedWith(compareBy<CountryOccurrence> { it.date }.thenBy { it.name })
    }

    private fun resolve(rule: DateRuleDefinition, year: Int): LocalDate = when (rule.type) {
        "FIXED" -> LocalDate.of(year, requireNotNull(rule.month), requireNotNull(rule.day))
        "NTH_WEEKDAY" -> ObservanceRules.nthWeekday(
            year = year,
            month = Month.of(requireNotNull(rule.month)),
            ordinal = requireNotNull(rule.ordinal),
            dayOfWeek = DayOfWeek.valueOf(requireNotNull(rule.weekday)),
        )
        "EASTER_RELATIVE" -> EasterCalculator.gregorian(year).plusDays(rule.offsetDays.toLong())
        "ORTHODOX_EASTER_RELATIVE" -> EasterCalculator.orthodox(year).plusDays(rule.offsetDays.toLong())
        "MOTHERING_SUNDAY" -> ObservanceRules.motheringSunday(year)
        "FATHERS_DAY" -> ObservanceRules.fathersDay(year)
        "UK_CHRISTMAS" -> ukChristmasObserved(year)
        "UK_BOXING_DAY" -> ukBoxingDayObserved(year)
        else -> error("Unknown date rule: ${rule.type}")
    }

    private fun ukChristmasObserved(year: Int): LocalDate = when (LocalDate.of(year, 12, 25).dayOfWeek) {
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> LocalDate.of(year, 12, 27)
        else -> LocalDate.of(year, 12, 25)
    }

    private fun ukBoxingDayObserved(year: Int): LocalDate = when (LocalDate.of(year, 12, 26).dayOfWeek) {
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> LocalDate.of(year, 12, 28)
        else -> LocalDate.of(year, 12, 26)
    }
}

private fun readBundledResource(path: String): String {
    val stream = BundledCountryRepository::class.java.classLoader?.getResourceAsStream(path)
        ?: error("Missing bundled country resource: $path")
    return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}
