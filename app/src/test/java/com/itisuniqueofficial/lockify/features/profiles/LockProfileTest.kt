package com.itisuniqueofficial.lockify.features.profiles

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockProfileTest {
    @Test
    fun normalModeLocksOnlyListedPackages() {
        val p = LockProfile(1, "Personal", "", setOf("com.bank"), whitelistMode = false)
        assertTrue(p.shouldLock("com.bank"))
        assertFalse(p.shouldLock("com.calculator"))
    }

    @Test
    fun childModeLocksEverythingExceptWhitelist() {
        val p = LockProfile(2, "Kids", "", setOf("com.kidsgame", "com.draw"), whitelistMode = true)
        assertFalse(p.shouldLock("com.kidsgame"))
        assertTrue(p.shouldLock("com.bank"))
    }
}
