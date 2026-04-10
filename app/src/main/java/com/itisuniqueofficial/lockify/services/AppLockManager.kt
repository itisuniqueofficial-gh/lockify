package com.itisuniqueofficial.lockify.services

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.itisuniqueofficial.lockify.core.utils.LogUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object AppLockConstants {
    val KNOWN_RECENTS_CLASSES = setOf(
        "com.android.systemui.recents.RecentsActivity",
        "com.android.quickstep.RecentsActivity",
        "com.android.systemui.recents.RecentsView",
        "com.android.systemui.recents.RecentsPanelView",
    )

    val EXCLUDED_APPS = setOf(
        "com.android.systemui",
        "com.android.intentresolver",
        "com.google.android.permissioncontroller",
        "android.uid.system:1000",
        "com.google.android.googlequicksearchbox",
        "android",
        "com.google.android.gms",
        "com.google.android.webview"
    )

    val ACCESSIBILITY_SETTINGS_CLASSES = setOf(
        "com.android.settings.accessibility.AccessibilitySettings",
        "com.android.settings.accessibility.AccessibilityMenuActivity",
        "com.android.settings.accessibility.AccessibilityShortcutActivity",
        "com.android.settings.Settings\$AccessibilitySettingsActivity"
    )

    const val MAX_RESTART_ATTEMPTS = 3
    const val RESTART_COOLDOWN_MS = 30000L
    const val RESTART_INTERVAL_MS = 5000L
}

fun Context.isDeviceLocked(): Boolean {
    val keyguardManager = getSystemService(KeyguardManager::class.java)
    return keyguardManager?.isKeyguardLocked ?: false
}

@Suppress("DEPRECATION")
fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
    val manager = getSystemService(ActivityManager::class.java) ?: return false
    return manager.getRunningServices(Int.MAX_VALUE)
        .any { serviceClass.name == it.service.className }
}

object AppLockManager {
    private const val TAG = "AppLockManager"

    // Use AtomicReference for thread-safe string mutations
    private val _temporarilyUnlockedApp = java.util.concurrent.atomic.AtomicReference("")
    var temporarilyUnlockedApp: String
        get() = _temporarilyUnlockedApp.get()
        set(value) { _temporarilyUnlockedApp.set(value) }

    val appUnlockTimes = ConcurrentHashMap<String, Long>()
    val isLockScreenShown = AtomicBoolean(false)

    // Type-safe biometric state using the enum defined in AppLockAccessibilityService
    @Volatile
    var currentBiometricState: AppLockAccessibilityService.BiometricState =
        AppLockAccessibilityService.BiometricState.IDLE

    // Grace period tracking for quick app switching
    @Volatile private var recentlyLeftApp: String = ""
    @Volatile private var recentlyLeftTime: Long = 0L
    private const val GRACE_PERIOD_MS = 300L

    // Track last screen off time for enhanced privacy
    @Volatile private var lastScreenOffTime: Long = 0L
    private const val SCREEN_OFF_RELOCK_THRESHOLD_MS = 1000L

    // Sentinel value for "unlock forever until screen off"
    const val UNLOCK_DURATION_UNTIL_SCREEN_OFF = 10_000

    fun setRecentlyLeftApp(packageName: String) {
        recentlyLeftApp = packageName
        recentlyLeftTime = System.currentTimeMillis()
        LogUtils.d(TAG, "Left app $packageName at $recentlyLeftTime")
    }

    fun checkAndRestoreRecentlyLeftApp(packageName: String): Boolean {
        if (packageName == recentlyLeftApp && packageName.isNotEmpty()) {
            val elapsed = System.currentTimeMillis() - recentlyLeftTime
            if (elapsed <= GRACE_PERIOD_MS) {
                LogUtils.d(TAG, "Restoring unlock state for $packageName (elapsed: ${elapsed}ms)")
                temporarilyUnlockedApp = packageName
                recentlyLeftApp = ""
                recentlyLeftTime = 0L
                return true
            } else {
                recentlyLeftApp = ""
            }
        }
        return false
    }

    /** Record screen off event for privacy protection. */
    fun recordScreenOff() {
        lastScreenOffTime = System.currentTimeMillis()
        LogUtils.d(TAG, "Screen off recorded at $lastScreenOffTime")
    }

    /** Returns true if enough time has passed since screen off to require relock. */
    fun shouldRelockAfterScreenOff(): Boolean {
        if (lastScreenOffTime == 0L) return false
        return (System.currentTimeMillis() - lastScreenOffTime) >= SCREEN_OFF_RELOCK_THRESHOLD_MS
    }

    /**
     * Clear all unlock state for privacy protection.
     * Called on screen off, recents open, or explicit security events.
     */
    fun clearAllUnlockState() {
        _temporarilyUnlockedApp.set("")
        appUnlockTimes.clear()
        recentlyLeftApp = ""
        recentlyLeftTime = 0L
        LogUtils.d(TAG, "All unlock state cleared for privacy")
    }

    private val serviceRestartAttempts = ConcurrentHashMap<String, Int>()
    private val lastRestartTime = ConcurrentHashMap<String, Long>()

    private val ALL_APP_LOCK_SERVICES = setOf(
        ExperimentalAppLockService::class.java
    )

    fun unlockApp(packageName: String) {
        _temporarilyUnlockedApp.set(packageName)
        appUnlockTimes[packageName] = System.currentTimeMillis()
        LogUtils.d(TAG, "App $packageName unlocked at ${appUnlockTimes[packageName]}")
    }

    fun temporarilyUnlockAppWithBiometrics(packageName: String) {
        unlockApp(packageName)
        reportBiometricAuthFinished()
    }

    fun reportBiometricAuthStarted() {
        currentBiometricState = AppLockAccessibilityService.BiometricState.AUTH_STARTED
    }

    fun reportBiometricAuthFinished() {
        currentBiometricState = AppLockAccessibilityService.BiometricState.IDLE
    }

    fun isAppTemporarilyUnlocked(packageName: String): Boolean =
        _temporarilyUnlockedApp.get() == packageName

    fun clearTemporarilyUnlockedApp() {
        _temporarilyUnlockedApp.set("")
    }

    fun startFallbackServices(context: Context, failedService: Class<*>) {
        val serviceName = failedService.simpleName
        if (!shouldAttemptRestart(serviceName)) return

        // Only ExperimentalAppLockService (usage stats backend) can be restarted manually.
        // AppLockAccessibilityService is system-managed and cannot be started via startService.
        if (failedService == ExperimentalAppLockService::class.java) {
            if (!shouldAttemptRestart(serviceName)) return
            try {
                LogUtils.d(TAG, "Attempting to restart ExperimentalAppLockService.")
                context.startService(Intent(context, ExperimentalAppLockService::class.java))
                recordRestartAttempt(serviceName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart ExperimentalAppLockService", e)
            }
        } else {
            LogUtils.d(TAG, "${failedService.simpleName} stopped. System manages its lifecycle.")
        }
    }

    fun stopAllOtherServices(context: Context, excludeService: Class<*>) {
        ALL_APP_LOCK_SERVICES
            .filter { it != excludeService }
            .forEach { context.stopService(Intent(context, it)) }
        LogUtils.d(TAG, "Stopped all main app lock services except ${excludeService.simpleName}.")
    }

    fun resetRestartAttempts(serviceName: String) {
        serviceRestartAttempts.remove(serviceName)
        lastRestartTime.remove(serviceName)
    }

    private fun shouldAttemptRestart(serviceName: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val attempts = serviceRestartAttempts[serviceName] ?: 0
        val lastRestart = lastRestartTime[serviceName] ?: 0L

        if (currentTime - lastRestart < AppLockConstants.RESTART_INTERVAL_MS) return false

        if (attempts >= AppLockConstants.MAX_RESTART_ATTEMPTS) {
            if (currentTime - lastRestart > AppLockConstants.RESTART_COOLDOWN_MS) {
                serviceRestartAttempts[serviceName] = 0
                return true
            }
            return false
        }
        return true
    }

    private fun recordRestartAttempt(serviceName: String) {
        serviceRestartAttempts.compute(serviceName) { _, attempts -> (attempts ?: 0) + 1 }
        lastRestartTime[serviceName] = System.currentTimeMillis()
    }
}
