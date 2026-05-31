package com.itisuniqueofficial.lockify.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.itisuniqueofficial.lockify.core.security.CredentialHasher

/**
 * Repository for managing application preferences and settings.
 */
class PreferencesRepository(context: Context) {

    private val appLockPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME_APP_LOCK, Context.MODE_PRIVATE)

    private val settingsPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME_SETTINGS, Context.MODE_PRIVATE)

    fun setPassword(password: String) {
        appLockPrefs.edit { putString(KEY_PASSWORD, CredentialHasher.hash(password)) }
    }
    fun getPassword(): String? = appLockPrefs.getString(KEY_PASSWORD, null)
    /** Restores a previously-hashed credential verbatim (used by backup restore). */
    fun setRawPasswordHash(hash: String) = appLockPrefs.edit { putString(KEY_PASSWORD, hash) }
    fun validatePassword(inputPassword: String): Boolean {
        val stored = getPassword()
        if (CredentialHasher.verify(inputPassword, stored)) return true
        if (stored != null && !CredentialHasher.isHash(stored) && inputPassword == stored) {
            setPassword(inputPassword)
            return true
        }
        return false
    }

    fun setPattern(pattern: String) {
        appLockPrefs.edit { putString(KEY_PATTERN, CredentialHasher.hash(pattern)) }
    }
    fun getPattern(): String? = appLockPrefs.getString(KEY_PATTERN, null)
    fun setRawPatternHash(hash: String) = appLockPrefs.edit { putString(KEY_PATTERN, hash) }
    fun validatePattern(inputPattern: String): Boolean {
        val stored = getPattern()
        if (CredentialHasher.verify(inputPattern, stored)) return true
        if (stored != null && !CredentialHasher.isHash(stored) && inputPattern == stored) {
            setPattern(inputPattern)
            return true
        }
        return false
    }

    fun setLockType(lockType: String) { settingsPrefs.edit { putString(KEY_LOCK_TYPE, lockType) } }
    fun getLockType(): String = settingsPrefs.getString(KEY_LOCK_TYPE, LOCK_TYPE_PIN) ?: LOCK_TYPE_PIN

    fun setBiometricAuthEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_BIOMETRIC_AUTH_ENABLED, enabled) }
    }
    fun isBiometricAuthEnabled(): Boolean =
        settingsPrefs.getBoolean(KEY_BIOMETRIC_AUTH_ENABLED, false)

    fun setUseMaxBrightness(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_USE_MAX_BRIGHTNESS, enabled) }
    }
    fun shouldUseMaxBrightness(): Boolean =
        settingsPrefs.getBoolean(KEY_USE_MAX_BRIGHTNESS, false)

    fun setDisableHaptics(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_DISABLE_HAPTICS, enabled) }
    }
    fun shouldDisableHaptics(): Boolean = settingsPrefs.getBoolean(KEY_DISABLE_HAPTICS, false)

    fun setScrambleKeypadEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_SCRAMBLE_KEYPAD, enabled) }
    }
    fun isScrambleKeypadEnabled(): Boolean = settingsPrefs.getBoolean(KEY_SCRAMBLE_KEYPAD, false)

    fun setMinPinLength(length: Int) { settingsPrefs.edit { putInt(KEY_MIN_PIN_LENGTH, length) } }
    fun getMinPinLength(): Int = settingsPrefs.getInt(KEY_MIN_PIN_LENGTH, DEFAULT_MIN_PIN_LENGTH)

    fun setIntruderSelfieEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_INTRUDER_SELFIE, enabled) }
    }
    fun isIntruderSelfieEnabled(): Boolean = settingsPrefs.getBoolean(KEY_INTRUDER_SELFIE, false)

    fun setHideLockedAppNotifications(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_HIDE_NOTIFICATIONS, enabled) }
    }
    fun isHideLockedAppNotifications(): Boolean =
        settingsPrefs.getBoolean(KEY_HIDE_NOTIFICATIONS, false)

    fun setShakeToLockEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_SHAKE_TO_LOCK, enabled) }
    }
    fun isShakeToLockEnabled(): Boolean = settingsPrefs.getBoolean(KEY_SHAKE_TO_LOCK, false)

    fun setShowSystemApps(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_SHOW_SYSTEM_APPS, enabled) }
    }
    fun shouldShowSystemApps(): Boolean = settingsPrefs.getBoolean(KEY_SHOW_SYSTEM_APPS, false)

    fun setAntiUninstallEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_ANTI_UNINSTALL, enabled) }
    }
    fun isAntiUninstallEnabled(): Boolean = settingsPrefs.getBoolean(KEY_ANTI_UNINSTALL, false)

    fun setProtectEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_APPLOCK_ENABLED, enabled) }
    }
    fun isProtectEnabled(): Boolean =
        settingsPrefs.getBoolean(KEY_APPLOCK_ENABLED, DEFAULT_PROTECT_ENABLED)

    fun setUnlockTimeDuration(minutes: Int) {
        settingsPrefs.edit { putInt(KEY_UNLOCK_TIME_DURATION, minutes) }
    }
    fun getUnlockTimeDuration(): Int =
        settingsPrefs.getInt(KEY_UNLOCK_TIME_DURATION, DEFAULT_UNLOCK_DURATION)

    fun setAutoUnlockEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_AUTO_UNLOCK, enabled) }
    }
    fun isAutoUnlockEnabled(): Boolean = settingsPrefs.getBoolean(KEY_AUTO_UNLOCK, false)

    fun setBackendImplementation(backend: BackendImplementation) {
        settingsPrefs.edit { putString(KEY_BACKEND_IMPLEMENTATION, backend.name) }
    }
    fun getBackendImplementation(): BackendImplementation {
        val stored = settingsPrefs.getString(
            KEY_BACKEND_IMPLEMENTATION,
            BackendImplementation.ACCESSIBILITY.name
        )
        return try {
            BackendImplementation.valueOf(stored ?: BackendImplementation.ACCESSIBILITY.name)
        } catch (_: IllegalArgumentException) {
            // Migrate any old SHIZUKU value to ACCESSIBILITY
            BackendImplementation.ACCESSIBILITY
        }
    }

    fun isShowCommunityLink(): Boolean =
        !settingsPrefs.getBoolean(KEY_COMMUNITY_LINK_SHOWN, false)
    fun setCommunityLinkShown(shown: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_COMMUNITY_LINK_SHOWN, shown) }
    }

    fun isShowDonateLink(context: Context): Boolean =
        settingsPrefs.getBoolean(KEY_SHOW_DONATE_LINK, false)
    fun setShowDonateLink(context: Context, show: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_SHOW_DONATE_LINK, show) }
    }

    fun isAccessibilityDisclosureAccepted(): Boolean =
        settingsPrefs.getBoolean(KEY_ACCESSIBILITY_DISCLOSURE_ACCEPTED, false)

    fun setAccessibilityDisclosureAccepted(accepted: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_ACCESSIBILITY_DISCLOSURE_ACCEPTED, accepted) }
    }

    fun isLockOnMinimizeEnabled(): Boolean =
        settingsPrefs.getBoolean(KEY_LOCK_ON_MINIMIZE, DEFAULT_LOCK_ON_MINIMIZE)

    fun setLockOnMinimizeEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_LOCK_ON_MINIMIZE, enabled) }
    }

    fun isLoggingEnabled(): Boolean = settingsPrefs.getBoolean(KEY_LOGGING_ENABLED, false)
    fun setLoggingEnabled(enabled: Boolean) {
        settingsPrefs.edit { putBoolean(KEY_LOGGING_ENABLED, enabled) }
    }

    companion object {
        private const val PREFS_NAME_APP_LOCK = "app_lock_prefs"
        private const val PREFS_NAME_SETTINGS = "app_lock_settings"

        private const val KEY_PASSWORD = "password"
        private const val KEY_PATTERN = "pattern"
        private const val KEY_BIOMETRIC_AUTH_ENABLED = "use_biometric_auth"
        private const val KEY_DISABLE_HAPTICS = "disable_haptics"
        private const val KEY_SCRAMBLE_KEYPAD = "scramble_keypad"
        private const val KEY_MIN_PIN_LENGTH = "min_pin_length"
        private const val KEY_INTRUDER_SELFIE = "intruder_selfie"
        private const val KEY_HIDE_NOTIFICATIONS = "hide_locked_app_notifications"
        private const val KEY_SHAKE_TO_LOCK = "shake_to_lock"
        private const val DEFAULT_MIN_PIN_LENGTH = 4
        private const val KEY_USE_MAX_BRIGHTNESS = "use_max_brightness"
        private const val KEY_ANTI_UNINSTALL = "anti_uninstall"
        private const val KEY_UNLOCK_TIME_DURATION = "unlock_time_duration"
        private const val KEY_BACKEND_IMPLEMENTATION = "backend_implementation"
        private const val KEY_COMMUNITY_LINK_SHOWN = "community_link_shown"
        private const val KEY_SHOW_DONATE_LINK = "show_donate_link"
        private const val KEY_LOGGING_ENABLED = "logging_enabled"
        private const val KEY_APPLOCK_ENABLED = "applock_enabled"
        private const val KEY_AUTO_UNLOCK = "auto_unlock"
        private const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
        private const val KEY_LOCK_TYPE = "lock_type"
        private const val KEY_ACCESSIBILITY_DISCLOSURE_ACCEPTED = "accessibility_disclosure_accepted"

        private const val DEFAULT_PROTECT_ENABLED = true
        private const val DEFAULT_UNLOCK_DURATION = 0
        private const val DEFAULT_LOCK_ON_MINIMIZE = true

        private const val KEY_LOCK_ON_MINIMIZE = "lock_on_minimize"

        const val LOCK_TYPE_PIN = "pin"
        const val LOCK_TYPE_PATTERN = "pattern"
    }
}
