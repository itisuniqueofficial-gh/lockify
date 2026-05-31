package com.itisuniqueofficial.lockify.features.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.itisuniqueofficial.lockify.R
import com.itisuniqueofficial.lockify.services.AppLockManager

/** A home-screen widget whose single button immediately re-locks all protected apps. */
class LockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, LockWidgetProvider::class.java).setAction(ACTION_LOCK_ALL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_lock).apply {
                setOnClickPendingIntent(R.id.widget_lock_button, pending)
            }
            manager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_LOCK_ALL) {
            AppLockManager.clearAllUnlockState()
            Toast.makeText(context, "Apps locked", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ACTION_LOCK_ALL = "com.itisuniqueofficial.lockify.LOCK_ALL"
    }
}
