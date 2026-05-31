package com.itisuniqueofficial.lockify.features.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.itisuniqueofficial.lockify.data.repository.AppLockRepository

/**
 * Redacts notification content for locked apps: the original notification is cancelled and
 * replaced with a generic "New notification" on Lockify's own channel, so the lock screen /
 * shade never reveals sensitive previews. Requires the user to grant Notification Access.
 */
class LockifyNotificationService : NotificationListenerService() {

    private val repo by lazy { AppLockRepository(applicationContext) }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return
        if (!repo.isHideLockedAppNotifications() || !repo.isAppLocked(pkg)) return

        runCatching {
            postRedacted(pkg, sbn.id)
            cancelNotification(sbn.key)
        }
    }

    private fun postRedacted(pkg: String, id: Int) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Private notifications", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0))
        }.getOrDefault("App")
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(label)
            .setContentText("New notification")
            .build()
        nm.notify(pkg.hashCode() + id, notification)
    }

    private companion object {
        const val CHANNEL = "lockify_private_notifications"
    }
}
