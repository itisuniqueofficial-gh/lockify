package com.itisuniqueofficial.lockify.features.stats

import android.app.usage.UsageStatsManager
import android.content.Context

data class AppUsage(val packageName: String, val totalTimeMs: Long)

/** Aggregates per-app foreground time from [UsageStatsManager] (needs Usage Access). */
object UsageStatsProvider {
    fun topApps(context: Context, days: Int = 7, limit: Int = 10): List<AppUsage> {
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val end = System.currentTimeMillis()
        val start = end - days * 24L * 60L * 60L * 1000L
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end) ?: return emptyList()
        return stats
            .filter { it.totalTimeInForeground > 0 }
            .groupBy { it.packageName }
            .map { (pkg, list) -> AppUsage(pkg, list.sumOf { it.totalTimeInForeground }) }
            .sortedByDescending { it.totalTimeMs }
            .take(limit)
    }
}
