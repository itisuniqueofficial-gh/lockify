package com.itisuniqueofficial.lockify

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.itisuniqueofficial.lockify.core.utils.LogUtils
import com.itisuniqueofficial.lockify.data.repository.AppLockRepository
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.concurrent.thread

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
        try {
            appLockRepository = AppLockRepository(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize application components", e)
        }

        LogUtils.initialize(this)
        LogUtils.setLoggingEnabled(appLockRepository.isLoggingEnabled())
        thread(start = true, name = "LogPurge") {
            LogUtils.purgeOldLogs()
        }
    }

    companion object {
        private const val TAG = "AppLockApplication"
    }
}
