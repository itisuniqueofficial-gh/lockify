package com.itisuniqueofficial.lockify.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object CredentialHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val FORMAT = "pbkdf2_sha256"
    private const val ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val HASH_BITS = 256
    private const val PARTS = 4

    fun hash(secret: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val digest = pbkdf2(secret, salt, ITERATIONS)
        return listOf(
            FORMAT,
            ITERATIONS.toString(),
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(digest)
        ).joinToString("\$")
    }

    fun isHash(value: String?): Boolean = value?.startsWith("$FORMAT\$") == true

    fun verify(secret: String, stored: String?): Boolean {
        if (stored.isNullOrBlank() || !isHash(stored)) return false

        return runCatching {
            val parts = stored.split('$')
            if (parts.size != PARTS || parts[0] != FORMAT) return false


            val iterations = parts[1].toInt()
            val salt = Base64.getDecoder().decode(parts[2])
            val expected = Base64.getDecoder().decode(parts[3])
            val actual = pbkdf2(secret, salt, iterations)

            MessageDigest.isEqual(actual, expected)
        }.getOrDefault(false)
    }

    private fun pbkdf2(secret: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, iterations, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
