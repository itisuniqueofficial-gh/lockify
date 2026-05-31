package com.itisuniqueofficial.lockify.features.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-based encryption for local backups. A 256-bit key is derived from the user's
 * backup password with PBKDF2-HMAC-SHA256, then AES-256-GCM protects the payload.
 *
 * Blob layout: [16-byte salt][12-byte IV][GCM ciphertext+tag]. Pure JVM, unit-testable.
 */
object BackupCrypto {
    private const val ITERATIONS = 120_000
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_BITS = 128

    fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(TAG_BITS, iv))
        }
        return salt + iv + cipher.doFinal(plaintext)
    }

    fun decrypt(blob: ByteArray, password: CharArray): ByteArray {
        require(blob.size > SALT_LEN + IV_LEN) { "Backup file is truncated" }
        val salt = blob.copyOfRange(0, SALT_LEN)
        val iv = blob.copyOfRange(SALT_LEN, SALT_LEN + IV_LEN)
        val ct = blob.copyOfRange(SALT_LEN + IV_LEN, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(TAG_BITS, iv))
        }
        return cipher.doFinal(ct)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        return try {
            val bits = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            SecretKeySpec(bits, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
