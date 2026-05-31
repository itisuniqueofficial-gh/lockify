package com.itisuniqueofficial.lockify.core.schedule

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * A lock schedule. [daysOfWeek] is a bitmask with Mon=1, Tue=2, ... Sun=64.
 * Times are minutes-since-midnight. A window where start > end wraps past midnight.
 */
data class LockSchedule(
    val id: Long,
    val name: String,
    val daysOfWeek: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val enabled: Boolean = true
)

/** Pure, unit-testable evaluation of whether a schedule is active at a given moment. */
object ScheduleEvaluator {
    /** [isoDayOfWeek] is 1=Mon..7=Sun; [minuteOfDay] is 0..1439. */
    fun isActive(s: LockSchedule, isoDayOfWeek: Int, minuteOfDay: Int): Boolean {
        if (!s.enabled) return false
        if (s.daysOfWeek and (1 shl (isoDayOfWeek - 1)) == 0) return false
        return if (s.startMinutes <= s.endMinutes)
            minuteOfDay in s.startMinutes until s.endMinutes
        else
            minuteOfDay >= s.startMinutes || minuteOfDay < s.endMinutes
    }
}

/** Persists schedules as JSON in SharedPreferences (no DB dependency). */
class ScheduleRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("lock_schedules", Context.MODE_PRIVATE)

    fun getAll(): List<LockSchedule> {
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            LockSchedule(
                o.getLong("id"), o.getString("name"), o.getInt("days"),
                o.getInt("start"), o.getInt("end"), o.getBoolean("enabled")
            )
        }
    }

    fun save(schedule: LockSchedule) {
        val updated = getAll().filter { it.id != schedule.id } + schedule
        persist(updated)
    }

    fun delete(id: Long) = persist(getAll().filter { it.id != id })

    private fun persist(list: List<LockSchedule>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id).put("name", it.name).put("days", it.daysOfWeek)
                    .put("start", it.startMinutes).put("end", it.endMinutes)
                    .put("enabled", it.enabled)
            )
        }
        prefs.edit { putString(KEY, arr.toString()) }
    }

    private companion object {
        const val KEY = "schedules"
    }
}
