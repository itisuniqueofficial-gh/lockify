package com.itisuniqueofficial.lockify.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialHasherTest {
    @Test
    fun hashDoesNotStorePlainSecret() {
        val hash = CredentialHasher.hash("1234")

        assertNotEquals("1234", hash)
        assertTrue(CredentialHasher.isHash(hash))
    }

    @Test
    fun verifyAcceptsCorrectSecretOnly() {
        val hash = CredentialHasher.hash("159753")

        assertTrue(CredentialHasher.verify("159753", hash))
        assertFalse(CredentialHasher.verify("159752", hash))
    }

    @Test
    fun verifyRejectsMalformedOrLegacyValues() {
        assertFalse(CredentialHasher.verify("1234", null))
        assertFalse(CredentialHasher.verify("1234", "1234"))
        assertFalse(CredentialHasher.verify("1234", "pbkdf2_sha256\$bad\$payload"))
    }
}
