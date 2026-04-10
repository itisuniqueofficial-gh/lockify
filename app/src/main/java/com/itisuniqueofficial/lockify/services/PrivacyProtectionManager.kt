package com.itisuniqueofficial.lockify.services

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.WindowManager
import com.itisuniqueofficial.lockify.core.utils.LogUtils
import java.lang.ref.WeakReference

/**
 * Manages privacy protection for locked apps by preventing content leaks
 * in recent apps, screenshots, and screen recordings.
 * 
 * This manager provides centralized privacy protection to ensure protected
 * app content is never exposed through system UI or task previews.
 */
object PrivacyProtectionManager {
    private const val TAG = "PrivacyProtection"
    
    // Track activities that have been secured to avoid redundant operations
    private val securedActivities = mutableSetOf<WeakReference<Activity>>()
    
    /**
     * Apply FLAG_SECURE to an activity to prevent screenshots, screen recording,
     * and recent app preview leaks.
     */
    fun secureActivity(activity: Activity) {
        try {
            activity.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            securedActivities.add(WeakReference(activity))
            LogUtils.d(TAG, "Secured activity: ${activity.javaClass.simpleName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to secure activity", e)
        }
    }
    
    /**
     * Remove FLAG_SECURE from an activity.
     */
    fun unsecureActivity(activity: Activity) {
        try {
            activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            securedActivities.removeAll { it.get() == activity || it.get() == null }
            LogUtils.d(TAG, "Unsecured activity: ${activity.javaClass.simpleName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unsecure activity", e)
        }
    }
    
    /**
     * Exclude an app from recent tasks to prevent preview leaks.
     * This should be called when a protected app is detected.
     */
    fun excludeFromRecents(context: Context, packageName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                activityManager?.appTasks?.forEach { task ->
                    try {
                        val taskInfo = task.taskInfo
                        if (taskInfo.baseActivity?.packageName == packageName) {
                            // Move task to back to minimize preview exposure
                            task.moveToFront()
                        }
                    } catch (e: Exception) {
                        // Task may have been removed or inaccessible
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to exclude from recents: $packageName", e)
        }
    }
    
    /**
     * Clear all weak references to destroyed activities.
     */
    fun cleanup() {
        securedActivities.removeAll { it.get() == null }
    }
    
    /**
     * Check if an activity is currently secured.
     */
    fun isActivitySecured(activity: Activity): Boolean {
        return securedActivities.any { it.get() == activity }
    }
}
