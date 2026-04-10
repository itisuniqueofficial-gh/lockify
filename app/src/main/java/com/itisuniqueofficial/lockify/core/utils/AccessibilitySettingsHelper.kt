package com.itisuniqueofficial.lockify.core.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings

fun Context.isAccessibilityServiceEnabled(): Boolean {
    val accessibilityServiceName =
        "$packageName/$packageName.services.AppLockAccessibilityService"
    val enabledServices = Settings.Secure.getString(
        contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    if (enabledServices?.contains(accessibilityServiceName) == true) return true
    if (enabledServices?.contains("$packageName/.services.AppLockAccessibilityService") == true) return true
    return false
}

/**
 * Opens Android Accessibility Settings directly.
 * Only call this AFTER the prominent disclosure has been accepted by the user.
 */
fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}
