@file:Suppress("SpellCheckingInspection")

package com.itisuniqueofficial.lockify.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.getSystemService
import com.itisuniqueofficial.lockify.core.broadcast.DeviceAdmin
import com.itisuniqueofficial.lockify.core.utils.LogUtils
import com.itisuniqueofficial.lockify.core.utils.appLockRepository
import com.itisuniqueofficial.lockify.data.repository.AppLockRepository
import com.itisuniqueofficial.lockify.data.repository.BackendImplementation
import com.itisuniqueofficial.lockify.features.lockscreen.ui.PasswordOverlayActivity
import com.itisuniqueofficial.lockify.services.AppLockConstants.ACCESSIBILITY_SETTINGS_CLASSES
import com.itisuniqueofficial.lockify.services.AppLockConstants.EXCLUDED_APPS

@SuppressLint("AccessibilityPolicy")
class AppLockAccessibilityService : AccessibilityService() {
    private val appLockRepository: AppLockRepository by lazy { applicationContext.appLockRepository() }
    private val keyboardPackages: List<String> by lazy { getKeyboardPackageNames() }

    private var recentsOpen = false
    private var lastForegroundPackage = ""

    enum class BiometricState {
        IDLE, AUTH_STARTED
    }

    companion object {
        private const val TAG = "AppLockAccessibility"
        private const val DEVICE_ADMIN_SETTINGS_PACKAGE = "com.android.settings"
        private const val APP_PACKAGE_PREFIX = "com.itisuniqueofficial.lockify"

        @Volatile
        var isServiceRunning = false
    }

    private val screenStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            try {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    LogUtils.d(TAG, "Screen off detected. Resetting AppLock state.")
                    AppLockManager.isLockScreenShown.set(false)
                    AppLockManager.clearTemporarilyUnlockedApp()
                    AppLockManager.appUnlockTimes.clear()
                }
            } catch (e: Exception) {
                logError("Error in screenStateReceiver", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            isServiceRunning = true
            AppLockManager.currentBiometricState = BiometricState.IDLE
            AppLockManager.isLockScreenShown.set(false)
            startPrimaryBackendService()

            val filter = android.content.IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenStateReceiver, filter)
        } catch (e: Exception) {
            logError("Error in onCreate", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            serviceInfo = serviceInfo.apply {
                eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                packageNames = null
            }
            LogUtils.d(TAG, "Accessibility service connected")
            AppLockManager.resetRestartAttempts(TAG)
            appLockRepository.setActiveBackend(BackendImplementation.ACCESSIBILITY)
        } catch (e: Exception) {
            logError("Error in onServiceConnected", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            logUsefulEvent(event)
            handleAccessibilityEvent(event)
        } catch (e: Exception) {
            logError("Unhandled error in onAccessibilityEvent", e)
        }
    }

    private fun logUsefulEvent(event: AccessibilityEvent) {
        if (!LogUtils.isLoggingEnabled()) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            val packageName = event.packageName?.toString().orEmpty()
            val className = event.className?.toString().orEmpty()
            LogUtils.d(TAG, "event=${event.eventType} pkg=$packageName class=$className")
        }
    }

    private fun handleAccessibilityEvent(event: AccessibilityEvent) {
        // Anti-uninstall: intercept uninstall confirmation for protected apps
        if (appLockRepository.isAntiUninstallEnabled()) {
            // Guard Lockify's own Device Admin from being deactivated
            if (event.packageName == DEVICE_ADMIN_SETTINGS_PACKAGE) {
                checkForDeviceAdminDeactivation(event)
            }
            // Intercept uninstall confirmation dialogs for per-app protected packages
            interceptUninstallAttempt(event)
        }

        if (!appLockRepository.isProtectEnabled() || !isServiceRunning) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            try {
                handleWindowStateChanged(event)
            } catch (e: Exception) {
                logError("Error handling window state change", e)
                return
            }
        }

        if (recentsOpen) return

        val packageName = event.packageName?.toString() ?: return

        if (!isValidPackageForLocking(packageName)) return

        try {
            processPackageLocking(packageName)
        } catch (e: Exception) {
            logError("Error processing package locking for $packageName", e)
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val isRecentlyOpened = isRecentlyOpened(event)
        val isHomeScreen = isHomeScreen(event)

        when {
            isRecentlyOpened -> {
                LogUtils.d(TAG, "Entering recents")
                recentsOpen = true
                // If a protected app was in the foreground, show the lock screen immediately
                // so the recents thumbnail captures the lock screen, not the app content.
                val lastPkg = lastForegroundPackage
                if (lastPkg.isNotEmpty() &&
                    lastPkg in appLockRepository.getLockedApps() &&
                    AppLockManager.isAppTemporarilyUnlocked(lastPkg)
                ) {
                    LogUtils.d(TAG, "Protected app $lastPkg going to recents — triggering lock for privacy")
                    AppLockManager.clearTemporarilyUnlockedApp()
                    AppLockManager.appUnlockTimes.remove(lastPkg)
                }
            }
            isHomeScreenTransition(event) && recentsOpen -> {
                LogUtils.d(TAG, "Transitioning to home screen from recents")
                recentsOpen = false
                clearTemporarilyUnlockedAppIfNeeded()
            }
            isHomeScreen -> {
                LogUtils.d(TAG, "On home screen")
                recentsOpen = false
                clearTemporarilyUnlockedAppIfNeeded()
            }
            isAppSwitchedFromRecents(event) -> {
                LogUtils.d(TAG, "App switched from recents")
                recentsOpen = false
                clearTemporarilyUnlockedAppIfNeeded(event.packageName?.toString())
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun isRecentlyOpened(event: AccessibilityEvent): Boolean {
        return (event.packageName == getSystemDefaultLauncherPackageName() &&
                event.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED) ||
                (event.text.toString().lowercase().contains("recent apps"))
    }

    private fun isHomeScreen(event: AccessibilityEvent): Boolean {
        return event.packageName == getSystemDefaultLauncherPackageName() &&
                event.className == "com.android.launcher3.uioverrides.QuickstepLauncher" &&
                event.text.toString().lowercase().contains("home screen")
    }

    @SuppressLint("InlinedApi")
    private fun isHomeScreenTransition(event: AccessibilityEvent): Boolean {
        return event.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED &&
                event.packageName == getSystemDefaultLauncherPackageName()
    }

    private fun isAppSwitchedFromRecents(event: AccessibilityEvent): Boolean {
        return event.packageName != getSystemDefaultLauncherPackageName() && recentsOpen
    }

    private fun clearTemporarilyUnlockedAppIfNeeded(newPackage: String? = null) {
        val shouldClear = newPackage == null ||
                (newPackage != AppLockManager.temporarilyUnlockedApp &&
                        newPackage !in appLockRepository.getTriggerExcludedApps())
        if (shouldClear) {
            LogUtils.d(TAG, "Clearing temporarily unlocked app")
            AppLockManager.clearTemporarilyUnlockedApp()
        }
    }

    private fun isValidPackageForLocking(packageName: String): Boolean {
        if (applicationContext.isDeviceLocked()) {
            AppLockManager.appUnlockTimes.clear()
            AppLockManager.clearTemporarilyUnlockedApp()
            return false
        }
        if (!shouldAccessibilityHandleLocking()) return false
        if (packageName == APP_PACKAGE_PREFIX ||
            packageName in keyboardPackages ||
            packageName in EXCLUDED_APPS
        ) return false
        return true
    }

    private fun processPackageLocking(packageName: String) {
        val currentForegroundPackage = packageName
        val triggeringPackage = lastForegroundPackage
        lastForegroundPackage = currentForegroundPackage

        if (triggeringPackage in appLockRepository.getTriggerExcludedApps()) return

        val unlockedApp = AppLockManager.temporarilyUnlockedApp
        if (unlockedApp.isNotEmpty() &&
            unlockedApp != currentForegroundPackage &&
            currentForegroundPackage !in appLockRepository.getTriggerExcludedApps()
        ) {
            LogUtils.d(TAG, "Switched from unlocked app $unlockedApp to $currentForegroundPackage.")
            AppLockManager.setRecentlyLeftApp(unlockedApp)
            AppLockManager.clearTemporarilyUnlockedApp()
        }

        checkAndLockApp(currentForegroundPackage, triggeringPackage, System.currentTimeMillis())
    }

    private fun shouldAccessibilityHandleLocking(): Boolean {
        return when (appLockRepository.getBackendImplementation()) {
            BackendImplementation.ACCESSIBILITY -> true
            BackendImplementation.USAGE_STATS -> !applicationContext.isServiceRunning(
                ExperimentalAppLockService::class.java
            )
        }
    }

    private fun checkAndLockApp(packageName: String, triggeringPackage: String, currentTime: Long) {
        if (AppLockManager.isLockScreenShown.get() ||
            AppLockManager.currentBiometricState == BiometricState.AUTH_STARTED
        ) return

        if (packageName !in appLockRepository.getLockedApps()) return
        if (AppLockManager.isAppTemporarilyUnlocked(packageName)) return

        AppLockManager.clearTemporarilyUnlockedApp()

        val unlockDurationMinutes = appLockRepository.getUnlockTimeDuration()
        val unlockTimestamp = AppLockManager.appUnlockTimes[packageName] ?: 0L

        LogUtils.d(TAG, "checkAndLockApp: pkg=$packageName, duration=$unlockDurationMinutes min")

        if (unlockDurationMinutes > 0 && unlockTimestamp > 0) {
            if (unlockDurationMinutes >= 10_000) return

            val durationMillis = unlockDurationMinutes.toLong() * 60L * 1000L
            val elapsedMillis = currentTime - unlockTimestamp

            if (elapsedMillis < durationMillis) return

            LogUtils.d(TAG, "Unlock grace period expired for $packageName.")
            AppLockManager.appUnlockTimes.remove(packageName)
            AppLockManager.clearTemporarilyUnlockedApp()
        }

        if (AppLockManager.isLockScreenShown.get() ||
            AppLockManager.currentBiometricState == BiometricState.AUTH_STARTED
        ) return

        showLockScreenOverlay(packageName, triggeringPackage)
    }

    private fun showLockScreenOverlay(packageName: String, triggeringPackage: String) {
        LogUtils.d(TAG, "Locked app detected: $packageName. Showing overlay.")
        AppLockManager.isLockScreenShown.set(true)

        val intent = Intent(this, PasswordOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_FROM_BACKGROUND or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("locked_package", packageName)
            putExtra("triggering_package", triggeringPackage)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            logError("Failed to start password overlay", e)
            AppLockManager.isLockScreenShown.set(false)
        }
    }

    /**
     * Intercepts removal confirmation dialogs for apps in the protected list.
     *
     * Android routes app removal through the package installer. When the user
     * confirms removal, the package installer shows a dialog. We detect that dialog,
     * check if the target package is in our protected list, and if so block the action by
     * navigating away and showing the Lockify lock screen.
     *
     * Known package installer packages across Android versions and OEMs:
     * - com.android.packageinstaller (AOSP)
     * - com.google.android.packageinstaller (GMS)
     * - com.miui.packageinstaller (MIUI)
     * - com.samsung.android.packageinstaller (Samsung)
     */
    private fun interceptUninstallAttempt(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        val pkg = event.packageName?.toString() ?: return

        // Only care about package installer screens
        if (!isPackageInstallerPackage(pkg)) return

        val className = event.className?.toString() ?: ""
        val eventText = event.text.joinToString(" ").lowercase()

        // Detect app removal confirmation screen/dialog.
        // Be precise: only match the actual confirmation dialog/activity, NOT the app info
        // screen (which always shows an "Uninstall" button and would cause false triggers).
        val isRemovalScreen = className.contains("UninstallActivity", ignoreCase = true) ||
                className.contains("UninstallConfirm", ignoreCase = true) ||
                className.contains("UninstallAppProgress", ignoreCase = true) ||
                // Confirmation dialog text — must contain both "uninstall" AND a confirm action
                (eventText.contains("uninstall") && (
                    eventText.contains("ok") ||
                    eventText.contains("confirm") ||
                    eventText.contains("delete") ||
                    eventText.contains("clear data")
                )) ||
                eventText.contains("do you want to uninstall") ||
                eventText.contains("do you want to remove") ||
                eventText.contains("remove this app")

        if (!isRemovalScreen) return

        // Find which protected app is being removed by scanning the window content
        val protectedApps = appLockRepository.getAntiUninstallApps()
        if (protectedApps.isEmpty()) return

        val targetPackage = findProtectedPackageInWindow(protectedApps) ?: return

        LogUtils.d(TAG, "Removal attempt detected for protected app: $targetPackage")

        // Block: navigate away immediately, then show lock screen
        try {
            performGlobalAction(GLOBAL_ACTION_BACK)
        } catch (e: Exception) {
            logError("Error performing back action during removal block", e)
        }

        // Show Lockify lock screen so user must authenticate to proceed
        showLockScreenOverlay(targetPackage, pkg)
    }

    private fun isPackageInstallerPackage(packageName: String): Boolean {
        return packageName == "com.android.packageinstaller" ||
                packageName == "com.google.android.packageinstaller" ||
                packageName == "com.miui.packageinstaller" ||
                packageName == "com.samsung.android.packageinstaller" ||
                packageName.contains("packageinstaller", ignoreCase = true)
    }

    /**
     * Tries to identify which protected app is being removed by inspecting
     * the accessibility window content for matching package names or app labels.
     */
    private fun findProtectedPackageInWindow(protectedApps: Set<String>): String? {
        return try {
            val root = rootInActiveWindow ?: return null
            val windowText = buildString {
                collectNodeText(root, this)
            }.lowercase()

            // Check if any protected package name or its label appears in the window text
            for (pkg in protectedApps) {
                if (windowText.contains(pkg.lowercase())) return pkg
                // Also check app label
                try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    val label = packageManager.getApplicationLabel(appInfo).toString().lowercase()
                    if (label.isNotBlank() && windowText.contains(label)) return pkg
                } catch (_: Exception) { /* app may not be installed */ }
            }
            null
        } catch (e: Exception) {
            logError("Error scanning removal screen for protected package", e)
            null
        }
    }

    private fun collectNodeText(node: android.view.accessibility.AccessibilityNodeInfo, sb: StringBuilder) {
        try {
            node.text?.let { sb.append(it).append(' ') }
            node.contentDescription?.let { sb.append(it).append(' ') }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                collectNodeText(child, sb)
            }
        } catch (_: Exception) { /* ignore node access errors */ }
    }

    private fun checkForDeviceAdminDeactivation(event: AccessibilityEvent) {
        LogUtils.d(TAG, "Checking for device admin deactivation")

        if (isDeactivationAttempt(event)) {
            LogUtils.d(TAG, "Blocking accessibility service deactivation")
            blockDeactivationAttempt()
            return
        }

        val isDeviceAdminPage = isDeviceAdminPage(event)
        LogUtils.d(TAG, "User is on device admin page: $isDeviceAdminPage")

        if (!isDeviceAdminPage) return

        blockDeviceAdminDeactivation()
    }

    private fun isDeactivationAttempt(event: AccessibilityEvent): Boolean {
        val appName = "Lockify"
        val isAccessibilitySettings = event.className in ACCESSIBILITY_SETTINGS_CLASSES &&
                event.text.any { it.contains(appName, ignoreCase = true) }
        val isSubSettings = event.className == "com.android.settings.SubSettings" &&
                event.text.any { it.contains(appName, ignoreCase = true) }
        val isAlertDialog = event.packageName == "com.google.android.packageinstaller" &&
                event.className == "android.app.AlertDialog" &&
                event.text.toString().lowercase().contains(appName.lowercase())
        return isAccessibilitySettings || isSubSettings || isAlertDialog
    }

    @SuppressLint("InlinedApi")
    private fun blockDeactivationAttempt() {
        try {
            performGlobalAction(GLOBAL_ACTION_BACK)
            performGlobalAction(GLOBAL_ACTION_HOME)
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } catch (e: Exception) {
            logError("Error blocking deactivation attempt", e)
        }
    }

    private fun isDeviceAdminPage(event: AccessibilityEvent): Boolean {
        val hasDeviceAdminDescription = event.contentDescription?.toString()?.lowercase()
            ?.contains("device admin app") == true &&
                event.className == "android.widget.FrameLayout"
        val isAdminConfigClass = event.className?.contains("DeviceAdminAdd") == true ||
                event.className?.contains("DeviceAdminSettings") == true
        return hasDeviceAdminDescription || isAdminConfigClass
    }

    @SuppressLint("InlinedApi")
    private fun blockDeviceAdminDeactivation() {
        try {
            val dpm: DevicePolicyManager? = getSystemService()
            val component = ComponentName(this, DeviceAdmin::class.java)

            if (dpm?.isAdminActive(component) == true) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                performGlobalAction(GLOBAL_ACTION_BACK)
                performGlobalAction(GLOBAL_ACTION_HOME)
                Thread.sleep(100)
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                Toast.makeText(
                    this,
                    "Disable anti-uninstall from Lockify settings to remove this restriction.",
                    Toast.LENGTH_LONG
                ).show()
                LogUtils.d(TAG, "Blocked device admin deactivation attempt.")
            }
        } catch (e: Exception) {
            logError("Error blocking device admin deactivation", e)
        }
    }

    private fun getKeyboardPackageNames(): List<String> {
        return try {
            getSystemService<InputMethodManager>()?.enabledInputMethodList?.map { it.packageName }
                ?: emptyList()
        } catch (e: Exception) {
            logError("Error getting keyboard package names", e)
            emptyList()
        }
    }

    fun getSystemDefaultLauncherPackageName(): String {
        return try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(
                homeIntent, PackageManager.MATCH_DEFAULT_ONLY
            )
            val systemLauncher = resolveInfoList.find { resolveInfo ->
                val isSystemApp = (resolveInfo.activityInfo.applicationInfo.flags and
                        android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                val isOurApp = resolveInfo.activityInfo.packageName == packageName
                isSystemApp && !isOurApp
            }
            systemLauncher?.activityInfo?.packageName ?: ""
        } catch (e: Exception) {
            logError("Error getting system default launcher package", e)
            ""
        }
    }

    private fun startPrimaryBackendService() {
        try {
            AppLockManager.stopAllOtherServices(this, AppLockAccessibilityService::class.java)
            when (appLockRepository.getBackendImplementation()) {
                BackendImplementation.USAGE_STATS -> {
                    LogUtils.d(TAG, "Starting Usage Stats service as primary backend")
                    startService(Intent(this, ExperimentalAppLockService::class.java))
                }
                else -> {
                    LogUtils.d(TAG, "Accessibility service is the primary backend.")
                }
            }
        } catch (e: Exception) {
            logError("Error starting primary backend service", e)
        }
    }

    override fun onInterrupt() {
        try {
            LogUtils.d(TAG, "Accessibility service interrupted")
        } catch (e: Exception) {
            logError("Error in onInterrupt", e)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return try {
            LogUtils.d(TAG, "Accessibility service unbound")
            isServiceRunning = false
            AppLockManager.startFallbackServices(this, AppLockAccessibilityService::class.java)
            super.onUnbind(intent)
        } catch (e: Exception) {
            logError("Error in onUnbind", e)
            super.onUnbind(intent)
        }
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
            isServiceRunning = false
            LogUtils.d(TAG, "Accessibility service destroyed")
            try {
                unregisterReceiver(screenStateReceiver)
            } catch (_: IllegalArgumentException) {
                // already unregistered
            }
            AppLockManager.isLockScreenShown.set(false)
            AppLockManager.startFallbackServices(this, AppLockAccessibilityService::class.java)
        } catch (e: Exception) {
            logError("Error in onDestroy", e)
        }
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
