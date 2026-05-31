package com.itisuniqueofficial.lockify.core.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build

/** Schedules exact alarms with graceful fallback across API 26..35. */
object ScheduleAlarm {
    fun scheduleExact(context: Context, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // No exact-alarm permission: fall back to an inexact-but-idle-safe alarm.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            return
        }
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
}
