package com.itisuniqueofficial.lockify.core.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.itisuniqueofficial.lockify.core.utils.LogUtils
import com.itisuniqueofficial.lockify.services.AppLockManager

/**
 * Receives device unlock (ACTION_USER_PRESENT) and screen-off events.
 * Screen-off clearing is the authoritative path when this receiver is registered
 * by a component that doesn't have its own screen-state receiver.
 */
class DeviceUnlockReceiver(private val onDeviceUnlocked: () -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_USER_PRESENT -> {
                LogUtils.d("DeviceUnlockReceiver", "Device unlocked (ACTION_USER_PRESENT)")
                onDeviceUnlocked()
            }
            Intent.ACTION_SCREEN_OFF -> {
                LogUtils.d("DeviceUnlockReceiver", "Screen turned OFF — clearing all unlock state")
                AppLockManager.recordScreenOff()
                AppLockManager.clearAllUnlockState()
            }
        }
    }
}

