package com.thefadghost.neverforget.backup

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupDecryptionException(cause: Throwable? = null) :
    IllegalArgumentException("Backup password is incorrect or the file is damaged", cause)

object BackupCrypto {
    private val magic = byteArrayOf('N'.code.toByte(), 'F'.code.toByte(), 'G'.code.toByte(), 1)
    private const val saltLength = 16
    private const val nonceLength = 12
    private const val iterations = 210_000
    private const val keyBits = 256

    fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray {
        require(password.isNotEmpty()) { "Backup password cannot be empty" }
        val random = SecureRandom()
        val salt = ByteArray(saltLength).also(random::nextBytes)
        val nonce = ByteArray(nonceLength).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, nonce))
        cipher.updateAAD(magic)
        val ciphertext = cipher.doFinal(plaintext)
        return ByteBuffer.allocate(magic.size + salt.size + nonce.size + ciphertext.size)
            .put(magic)
            .put(salt)
            .put(nonce)
            .put(ciphertext)
            .array()
    }

    fun decrypt(encrypted: ByteArray, password: CharArray): ByteArray {
        if (encrypted.size <= magic.size + saltLength + nonceLength) throw BackupDecryptionException()
        val buffer = ByteBuffer.wrap(encrypted)
        val readMagic = ByteArray(magic.size).also(buffer::get)
        if (!readMagic.contentEquals(magic)) throw BackupDecryptionException()
        val salt = ByteArray(saltLength).also(buffer::get)
        val nonce = ByteArray(nonceLength).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, nonce))
            cipher.updateAAD(magic)
            cipher.doFinal(ciphertext)
        } catch (error: AEADBadTagException) {
            throw BackupDecryptionException(error)
        } catch (error: Exception) {
            throw BackupDecryptionException(error)
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, keyBits)
        return try {
            val encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
            SecretKeySpec(encoded, "AES")
        } finally {
            spec.clearPassword()
            password.fill('\u0000')
        }
    }
}

