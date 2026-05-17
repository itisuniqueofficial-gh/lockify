package com.itisuniqueofficial.lockify.core.protection

import android.content.Context
import androidx.core.content.edit
import com.itisuniqueofficial.lockify.core.utils.hasUsagePermission
import com.itisuniqueofficial.lockify.core.utils.isAccessibilityServiceEnabled
import com.itisuniqueofficial.lockify.services.AppLockManager

enum class ProtectionState {
    ACTIVE,
    DEGRADED,
    UNSAFE,
    RECOVERING,
    REBOOTED
}

object ProtectionStateStore {
    private const val PREFS_NAME = "protection_state"
    private const val KEY_STATE = "state"
    private const val KEY_UPDATED_AT = "updated_at"
    private const val KEY_LAST_REASON = "last_reason"

    fun markActive(context: Context, reason: String = "accessibility_active") {
        setState(context, ProtectionState.ACTIVE, reason)
    }

    fun markDegraded(context: Context, reason: String = "usage_stats_only") {
        setState(context, ProtectionState.DEGRADED, reason)
    }

    fun markUnsafe(context: Context, reason: String) {
        AppLockManager.clearAllUnlockState()
        setState(context, ProtectionState.UNSAFE, reason)
    }

    fun markRebootedAndRelock(context: Context) {
        AppLockManager.clearAllUnlockState()
        setState(context, ProtectionState.REBOOTED, "device_rebooted")
    }

    fun validateAllPermissions(context: Context): ProtectionState {
        val state = when {
            context.isAccessibilityServiceEnabled() -> ProtectionState.ACTIVE
            context.hasUsagePermission() -> ProtectionState.DEGRADED
            else -> ProtectionState.UNSAFE
        }
        if (state != ProtectionState.ACTIVE) {
            AppLockManager.clearAllUnlockState()
        }
        setState(context, state, "permission_health_check")
        return state
    }

    fun getState(context: Context): ProtectionState {
        val stored = prefs(context).getString(KEY_STATE, ProtectionState.UNSAFE.name)
        return runCatching { ProtectionState.valueOf(stored ?: ProtectionState.UNSAFE.name) }
            .getOrDefault(ProtectionState.UNSAFE)
    }

    fun getLastReason(context: Context): String = prefs(context).getString(KEY_LAST_REASON, "") ?: ""

    fun getUpdatedAt(context: Context): Long = prefs(context).getLong(KEY_UPDATED_AT, 0L)

    private fun setState(context: Context, state: ProtectionState, reason: String) {
        prefs(context).edit {
            putString(KEY_STATE, state.name)
            putString(KEY_LAST_REASON, reason)
            putLong(KEY_UPDATED_AT, System.currentTimeMillis())
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
