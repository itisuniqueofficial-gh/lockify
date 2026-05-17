package com.itisuniqueofficial.lockify.core.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.itisuniqueofficial.lockify.core.protection.ProtectionStateStore
import com.itisuniqueofficial.lockify.core.utils.LogUtils
import com.itisuniqueofficial.lockify.core.utils.appLockRepository
import com.itisuniqueofficial.lockify.core.workers.ProtectionHealthWorker
import com.itisuniqueofficial.lockify.data.repository.BackendImplementation
import com.itisuniqueofficial.lockify.services.ExperimentalAppLockService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repository = context.appLockRepository()

        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                LogUtils.clearAllLogs()
                ProtectionStateStore.validateAllPermissions(context)
                ProtectionHealthWorker.schedule(context)
                try {
                    startAppropriateServices(context, repository)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting services after app update", e)
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                ProtectionStateStore.markRebootedAndRelock(context)
                ProtectionHealthWorker.schedule(context)
                try {
                    startAppropriateServices(context, repository)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting services on boot", e)
                }
            }
            ACTION_QUICKBOOT_POWERON,
            ACTION_HTC_QUICKBOOT_POWERON -> {
                ProtectionStateStore.markRebootedAndRelock(context)
                ProtectionHealthWorker.schedule(context)
            }
        }
    }

    private fun startAppropriateServices(
        context: Context,
        repository: com.itisuniqueofficial.lockify.data.repository.AppLockRepository
    ) {
        when (repository.getBackendImplementation()) {
            BackendImplementation.USAGE_STATS -> {
                // Usage stats backend: start the foreground service.
                // The accessibility service is bound by the system — we cannot start it manually.
                startService(context, ExperimentalAppLockService::class.java)
            }
            BackendImplementation.ACCESSIBILITY -> {
                // Accessibility service is bound by the system automatically when enabled.
                // Nothing to start here — the system will bind it on boot if enabled.
                LogUtils.d(TAG, "Accessibility backend selected — system will bind service automatically.")
            }
        }
    }

    private fun startService(context: Context, serviceClass: Class<*>) {
        try {
            ContextCompat.startForegroundService(context, Intent(context, serviceClass))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service: ${serviceClass.simpleName}", e)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        private const val ACTION_HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"
    }
}
