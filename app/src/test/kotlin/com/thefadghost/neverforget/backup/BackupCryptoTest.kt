package com.thefadghost.neverforget.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {
    @Test
    fun `encrypted backup round trips`() {
        val plaintext = """{"version":1,"people":["Mila"]}""".encodeToByteArray()
        val encrypted = BackupCrypto.encrypt(plaintext, "correct horse battery staple".toCharArray())

        assertArrayEquals(
            plaintext,
            BackupCrypto.decrypt(encrypted, "correct horse battery staple".toCharArray()),
        )
    }

    @Test
    fun `wrong password cannot decrypt backup`() {
        val encrypted = BackupCrypto.encrypt("private".encodeToByteArray(), "right".toCharArray())

        var failed = false
        try {
            BackupCrypto.decrypt(encrypted, "wrong".toCharArray())
        } catch (_: BackupDecryptionException) {
            failed = true
        }
        assertTrue(failed)
    }
}
