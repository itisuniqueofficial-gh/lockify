package com.itisuniqueofficial.lockify

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.itisuniqueofficial.lockify.core.utils.LogUtils
import com.itisuniqueofficial.lockify.data.repository.AppLockRepository
import org.lsposed.hiddenapibypass.HiddenApiBypass

class AppLockApplication : Application() {

    lateinit var appLockRepository: AppLockRepository
        private set

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.addHiddenApiExemptions("L")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize hidden API bypass", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        appLockRepository = AppLockRepository(this)
        LogUtils.initialize(this)
        LogUtils.setLoggingEnabled(appLockRepository.isLoggingEnabled())
        // purgeOldLogs already spawns its own daemon thread internally
        LogUtils.purgeOldLogs()
    }

    companion object {
        private const val TAG = "AppLockApplication"
    }
}
