package com.itisuniqueofficial.lockify.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.itisuniqueofficial.lockify.R
import com.itisuniqueofficial.lockify.core.broadcast.DeviceAdmin
import com.itisuniqueofficial.lockify.core.utils.LogUtils
import com.itisuniqueofficial.lockify.core.utils.appLockRepository
import com.itisuniqueofficial.lockify.core.utils.hasUsagePermission
import com.itisuniqueofficial.lockify.data.repository.AppLockRepository
import com.itisuniqueofficial.lockify.data.repository.AppLockRepository.Companion.shouldStartService
import com.itisuniqueofficial.lockify.data.repository.BackendImplementation
import com.itisuniqueofficial.lockify.features.lockscreen.ui.PasswordOverlayActivity
import com.itisuniqueofficial.lockify.features.recents.RecentsPrivacyActivity
import java.util.Timer
import kotlin.concurrent.timerTask

class ExperimentalAppLockService : Service() {
    private val TAG = "ExperimentalAppLockService"
    private val NOTIFICATION_ID = 113
    private val CHANNEL_ID = "ExperimentalAppLockServiceChannel"

    private val appLockRepository: AppLockRepository by lazy { applicationContext.appLockRepository() }
    private val usageStatsManager: UsageStatsManager by lazy { getSystemService()!! }
    private val notificationManager: NotificationManager by lazy { getSystemService()!! }

    private var timer: Timer? = null
    private var previousForegroundPackage = ""

    // Cache keyboard packages to avoid querying InputMethodManager on every 100ms tick
    private var keyboardPackages: List<String> = emptyList()
    private var keyboardPackagesCachedAt: Long = 0L
    private val KEYBOARD_CACHE_TTL_MS = 60_000L

    private val screenStateReceiver = object: android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    LogUtils.d(
                        TAG,
                        "Screen off detected in Usage Stats backend. Clearing all unlock state for privacy."
                    )
                    AppLockManager.isLockScreenShown.set(false)
                    AppLockManager.recordScreenOff()
                    AppLockManager.clearAllUnlockState()
                    previousForegroundPackage = ""
                }
                Intent.ACTION_USER_PRESENT -> {
                    // Device unlocked — reset the screen-off state so the next app open
                    // doesn't incorrectly trigger a relock due to stale screen-off time.
                    LogUtils.d(TAG, "Device unlocked (ACTION_USER_PRESENT)")
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!shouldStartService(appLockRepository, this::class.java) || !hasUsagePermission()) {
            Log.e(TAG, "Permissions missing or service not needed. Falling back.")
            AppLockManager.startFallbackServices(this, this::class.java)
            stopSelf()
            return START_NOT_STICKY
        }

        AppLockManager.resetRestartAttempts(TAG)
        appLockRepository.setActiveBackend(BackendImplementation.USAGE_STATS)
        AppLockManager.stopAllOtherServices(this, this::class.java)
        AppLockManager.isLockScreenShown.set(false)

        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, filter)

        startMonitoringTimer()
        startForegroundService()

        return START_STICKY
    }

    override fun onDestroy() {
        timer?.cancel()
        LogUtils.d(TAG, "ExperimentalAppLockService destroyed.")

        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver not registered or already unregistered")
        }

        AppLockManager.isLockScreenShown.set(false)
        notificationManager.cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- Monitoring ---

    private fun startMonitoringTimer() {
        timer?.cancel()
        timer = Timer()
        timer?.schedule(timerTask {
            if (!appLockRepository.isProtectEnabled() || applicationContext.isDeviceLocked()) {
                if (applicationContext.isDeviceLocked()) {
                    // PRIVACY FIX: Clear all unlock state when device is locked
                    AppLockManager.clearAllUnlockState()
                    previousForegroundPackage = ""
                }
                return@timerTask
            }

            val foregroundApp = getCurrentForegroundAppPackage() ?: return@timerTask
            val currentPackage = foregroundApp.first
            val triggeringPackage = previousForegroundPackage
            previousForegroundPackage = currentPackage

            if (isExclusionApp(currentPackage)) return@timerTask

            if (triggeringPackage in appLockRepository.getTriggerExcludedApps()) {
                return@timerTask
            }

            if (currentPackage == triggeringPackage) return@timerTask

            // When a protected app leaves the foreground and lock-on-minimize is enabled,
            // launch the recents privacy placeholder so the system snapshots it instead of
            // the real app content for the recents thumbnail.
            if (appLockRepository.isLockOnMinimizeEnabled() &&
                triggeringPackage.isNotEmpty() &&
                triggeringPackage in appLockRepository.getLockedApps() &&
                !isExclusionApp(currentPackage)
            ) {
                // Only launch placeholder if the app was actually unlocked (user saw content)
                if (AppLockManager.isAppTemporarilyUnlocked(triggeringPackage) ||
                    AppLockManager.appUnlockTimes.containsKey(triggeringPackage)
                ) {
                    launchRecentsPrivacyPlaceholder(triggeringPackage)
                }
            }

            checkAndLockApp(currentPackage, triggeringPackage, System.currentTimeMillis())
        }, 0, 100)
    }

    private fun isExclusionApp(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        if (now - keyboardPackagesCachedAt > KEYBOARD_CACHE_TTL_MS) {
            keyboardPackages = getSystemService<InputMethodManager>()
                ?.enabledInputMethodList
                ?.map { it.packageName }
                ?: emptyList()
            keyboardPackagesCachedAt = now
        }

        return packageName == this.packageName ||
                packageName in keyboardPackages ||
                packageName in AppLockConstants.EXCLUDED_APPS
    }

    /**
     * Returns the foreground package name and class name, or null if filtered.
     */
    private fun getCurrentForegroundAppPackage(): Pair<String, String>? {
        val time = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(time - 1000 * 100, time)
        val event = UsageEvents.Event()
        var recentApp: Pair<String, String>? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            if (event.eventType != UsageEvents.Event.ACTIVITY_RESUMED) continue
            if (event.className == "com.itisuniqueofficial.lockify.features.lockscreen.ui.PasswordOverlayActivity") continue

            if (event.className in AppLockConstants.KNOWN_RECENTS_CLASSES
            ) {
                continue
            }

            recentApp = Pair(event.packageName, event.className)
        }
        return recentApp
    }

    private fun checkAndLockApp(packageName: String, triggeringPackage: String, currentTime: Long) {
        val lockedApps = appLockRepository.getLockedApps()
        if (packageName !in lockedApps) return

        val unlockDurationMinutes = appLockRepository.getUnlockTimeDuration()
        val unlockTimestamp = AppLockManager.appUnlockTimes[packageName] ?: 0L

        LogUtils.d(
            TAG,
            "checkAndLockApp: pkg=$packageName, duration=$unlockDurationMinutes min, unlockTime=$unlockTimestamp, currentTime=$currentTime, isLockScreenShown=${AppLockManager.isLockScreenShown.get()}"
        )

        if (unlockDurationMinutes > 0 && unlockTimestamp > 0) {
            if (unlockDurationMinutes >= AppLockManager.UNLOCK_DURATION_UNTIL_SCREEN_OFF) {
                return
            }

            val durationMillis = unlockDurationMinutes.toLong() * 60_000L

            val elapsedMillis = currentTime - unlockTimestamp

            LogUtils.d(
                TAG,
                "Grace period check: elapsed=${elapsedMillis}ms (${elapsedMillis / 1000}s), duration=${durationMillis}ms (${durationMillis / 1000}s)"
            )

            if (elapsedMillis < durationMillis) {
                return
            }

            LogUtils.d(TAG, "Unlock grace period expired for $packageName. Clearing timestamp.")
            AppLockManager.appUnlockTimes.remove(packageName)
        }

        if (AppLockManager.isLockScreenShown.get() ||
            AppLockManager.currentBiometricState == AppLockAccessibilityService.BiometricState.AUTH_STARTED
        ) {
            LogUtils.d(TAG, "Lock screen already shown or biometric auth in progress, skipping")
            return
        }

        LogUtils.d(TAG, "Locked app: $packageName. Showing overlay with privacy protection.")
        AppLockManager.isLockScreenShown.set(true)

        // PRIVACY FIX: Launch lock screen with highest priority flags to minimize content exposure
        val intent = Intent(this, PasswordOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_FROM_BACKGROUND or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("locked_package", packageName)
            putExtra("triggering_package", triggeringPackage)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting overlay for: $packageName", e)
            AppLockManager.isLockScreenShown.set(false)
        }
    }

    private fun launchRecentsPrivacyPlaceholder(lockedPackage: String) {
        try {
            val intent = RecentsPrivacyActivity.buildIntent(applicationContext, lockedPackage)
            startActivity(intent)
            LogUtils.d(TAG, "Launched recents privacy placeholder for $lockedPackage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch recents privacy placeholder for $lockedPackage", e)
        }
    }

    private fun startForegroundService() {
        createNotificationChannel()
        val notification = createNotification()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            determineForegroundServiceType()
        } else 0

        if (type != 0) {
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun determineForegroundServiceType(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val dpm: DevicePolicyManager? = getSystemService()
            val component = ComponentName(this, DeviceAdmin::class.java)

            return if (dpm?.isAdminActive(component) == true) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
        }
        return 0
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Lockify Service (Usage Stats)",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lockify")
            .setContentText("Protecting your apps")
            .setSmallIcon(R.drawable.baseline_shield_24)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }
}

