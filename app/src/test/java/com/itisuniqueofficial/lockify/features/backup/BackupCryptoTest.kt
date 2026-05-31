package com.itisuniqueofficial.lockify.features.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCryptoTest {
    @Test
    fun roundTripWithCorrectPassword() {
        val data = "{\"lockedApps\":[\"com.bank\"]}".toByteArray()
        val blob = BackupCrypto.encrypt(data, "hunter2".toCharArray())
        assertArrayEquals(data, BackupCrypto.decrypt(blob, "hunter2".toCharArray()))
    }

    @Test
    fun wrongPasswordIsRejected() {
        val blob = BackupCrypto.encrypt("secret".toByteArray(), "right".toCharArray())
        assertThrows(Exception::class.java) {
            BackupCrypto.decrypt(blob, "wrong".toCharArray())
        }
    }
}
