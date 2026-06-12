package com.thefadghost.neverforget.data.country

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CountryPackTest {
    private val repository = BundledCountryRepository()

    @Test
    fun `all country events have source provenance`() {
        repository.packs().forEach { pack ->
            pack.events.forEach { event ->
                assertTrue("${event.id} authority", event.source.authority.isNotBlank())
                assertTrue("${event.id} url", event.source.url.startsWith("https://"))
                assertTrue("${event.id} verification date", event.source.verifiedOn.isNotBlank())
            }
        }
    }

    @Test
    fun `england and wales summer bank holiday is last monday in august`() {
        val dates = repository.occurrences("GB", "GB-EAW", 2026)
        assertEquals(
            LocalDate.of(2026, 8, 31),
            dates.single { it.eventId == "gb-summer-bank-holiday" }.date,
        )
    }

    @Test
    fun `scotland summer bank holiday is first monday in august`() {
        val dates = repository.occurrences("GB", "GB-SCT", 2026)
        assertEquals(
            LocalDate.of(2026, 8, 3),
            dates.single { it.eventId == "gb-summer-bank-holiday-scotland" }.date,
        )
    }

    @Test
    fun `northern ireland includes st patricks day`() {
        val dates = repository.occurrences("GB", "GB-NIR", 2026)
        assertEquals(
            LocalDate.of(2026, 3, 17),
            dates.single { it.eventId == "gb-st-patricks-day" }.date,
        )
    }

    @Test
    fun `bulgaria generates monday substitute for sunday unification day`() {
        val dates = repository.occurrences("BG", null, 2026)
        assertTrue(
            dates.any {
                it.eventId == "bg-unification-day-substitute" &&
                    it.date == LocalDate.of(2026, 9, 7)
            },
        )
    }

    @Test
    fun `name day aliases can be followed without a person`() {
        val names = repository.nameDays()
        val georgi = names.single { it.canonicalName == "Georgi" }
        assertEquals(LocalDate.of(2026, 5, 6), georgi.date.atYear(2026))
        assertTrue("Gergana" in georgi.aliases)
    }
}

