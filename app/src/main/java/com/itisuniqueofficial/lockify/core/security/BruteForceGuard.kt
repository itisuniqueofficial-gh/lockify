package com.itisuniqueofficial.lockify.core.security

import android.content.Context
import androidx.core.content.edit

/**
 * Brute-force protection: applies a tiered cooldown after repeated failed unlock
 * attempts. State is persisted so a cooldown survives process death / app restart.
 *
 * Tiers (per spec §5.4): 3 fails -> 30s, 5 fails -> 2min, 10 fails -> 10min.
 */
class BruteForceGuard(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Remaining lockout in milliseconds, or 0 if an unlock attempt is allowed now. */
    fun remainingLockoutMs(now: Long = System.currentTimeMillis()): Long =
        (prefs.getLong(KEY_UNTIL, 0L) - now).coerceAtLeast(0L)

    /** Records a failed attempt and arms the next cooldown tier. Returns the new cooldown ms. */
    fun recordFailure(now: Long = System.currentTimeMillis()): Long {
        val attempts = prefs.getInt(KEY_ATTEMPTS, 0) + 1
        val cooldown = cooldownMsForAttempts(attempts)
        prefs.edit {
            putInt(KEY_ATTEMPTS, attempts)
            putLong(KEY_UNTIL, if (cooldown > 0) now + cooldown else 0L)
        }
        return cooldown
    }

    /** Clears all failure state after a successful unlock. */
    fun recordSuccess() = prefs.edit { remove(KEY_ATTEMPTS); remove(KEY_UNTIL) }

    companion object {
        private const val PREFS = "brute_force_guard"
        private const val KEY_ATTEMPTS = "failed_attempts"
        private const val KEY_UNTIL = "lockout_until"

        /** Pure, unit-testable cooldown tier mapping. */
        fun cooldownMsForAttempts(attempts: Int): Long = when {
            attempts >= 10 -> 10 * 60_000L
            attempts >= 5 -> 2 * 60_000L
            attempts >= 3 -> 30_000L
            else -> 0L
        }
    }
}
