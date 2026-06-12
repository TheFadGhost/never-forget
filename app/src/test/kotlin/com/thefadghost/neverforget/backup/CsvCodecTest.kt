package com.thefadghost.neverforget.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvCodecTest {
    @Test
    fun `csv round trip preserves commas and optional years`() {
        val rows = listOf(
            CsvPerson("Mila, Petrova", "MOTHER", 6, 8, 1970),
            CsvPerson("Viktor", "FRIEND", 12, 2, null),
        )

        assertEquals(rows, PeopleCsvCodec.decode(PeopleCsvCodec.encode(rows)).validRows)
    }

    @Test
    fun `invalid rows are reported without discarding valid rows`() {
        val result = PeopleCsvCodec.decode(
            "name,relationship,month,day,year\nMila,MOTHER,6,8,1970\nBad,FRIEND,13,70,\n",
        )

        assertEquals(1, result.validRows.size)
        assertTrue(result.errors.single().contains("line 3"))
    }
}

