package com.itisuniqueofficial.lockify.core.schedule

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleEvaluatorTest {
    private fun schedule(days: Int, start: Int, end: Int, enabled: Boolean = true) =
        LockSchedule(1, "s", days, start, end, enabled)

    @Test
    fun activeInsideDaytimeWindowOnSelectedDay() {
        val mondayWork = schedule(days = 1, start = 9 * 60, end = 17 * 60) // Mon, 09:00-17:00
        assertTrue(ScheduleEvaluator.isActive(mondayWork, isoDayOfWeek = 1, minuteOfDay = 10 * 60))
        assertFalse(ScheduleEvaluator.isActive(mondayWork, isoDayOfWeek = 1, minuteOfDay = 8 * 60))
        assertFalse(ScheduleEvaluator.isActive(mondayWork, isoDayOfWeek = 2, minuteOfDay = 10 * 60))
    }

    @Test
    fun overnightWindowWrapsMidnight() {
        val bedtime = schedule(days = 127, start = 22 * 60, end = 6 * 60) // every day 22:00-06:00
        assertTrue(ScheduleEvaluator.isActive(bedtime, isoDayOfWeek = 3, minuteOfDay = 23 * 60))
        assertTrue(ScheduleEvaluator.isActive(bedtime, isoDayOfWeek = 3, minuteOfDay = 2 * 60))
        assertFalse(ScheduleEvaluator.isActive(bedtime, isoDayOfWeek = 3, minuteOfDay = 12 * 60))
    }

    @Test
    fun disabledScheduleNeverActive() {
        val s = schedule(days = 127, start = 0, end = 1439, enabled = false)
        assertFalse(ScheduleEvaluator.isActive(s, isoDayOfWeek = 1, minuteOfDay = 600))
    }
}
