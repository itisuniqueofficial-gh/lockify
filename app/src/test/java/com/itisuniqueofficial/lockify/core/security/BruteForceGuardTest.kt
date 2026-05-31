package com.itisuniqueofficial.lockify.core.security

import org.junit.Assert.assertEquals
import org.junit.Test

class BruteForceGuardTest {
    @Test
    fun noCooldownBelowThreshold() {
        assertEquals(0L, BruteForceGuard.cooldownMsForAttempts(0))
        assertEquals(0L, BruteForceGuard.cooldownMsForAttempts(2))
    }

    @Test
    fun tieredCooldownsMatchSpec() {
        assertEquals(30_000L, BruteForceGuard.cooldownMsForAttempts(3))
        assertEquals(30_000L, BruteForceGuard.cooldownMsForAttempts(4))
        assertEquals(120_000L, BruteForceGuard.cooldownMsForAttempts(5))
        assertEquals(120_000L, BruteForceGuard.cooldownMsForAttempts(9))
        assertEquals(600_000L, BruteForceGuard.cooldownMsForAttempts(10))
        assertEquals(600_000L, BruteForceGuard.cooldownMsForAttempts(25))
    }
}
