package com.itisuniqueofficial.lockify.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.concurrent.thread

@SuppressLint("StaticFieldLeak")
object LogUtils {
    private const val TAG = "LogUtils"
    private const val FILE_NAME = "app_logs.txt"
    private const val SECURITY_LOGS = "audit_log.txt"
    private lateinit var context: Context
    private var loggingEnabled = false

    fun initialize(application: Context) {
        context = application
    }

    fun setLoggingEnabled(enabled: Boolean) {
        loggingEnabled = enabled
    }

    fun d(tag: String, message: String) {
        if (!loggingEnabled) return

        Log.d(tag, message)

        runCatching {
            val file = File(context.filesDir, SECURITY_LOGS)
            if (!file.exists()) file.createNewFile()
            file.appendText(Instant.now().toString() + " D " + tag + ": " + message + "\n")
        }
    }

    fun isLoggingEnabled(): Boolean = loggingEnabled

    fun openExternalLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
            .onFailure { Log.e(TAG, "Error opening external link: $url", it) }
    }

    /**
     * Exports the security/audit log file.
     *
     * If logging was never enabled or the file is empty, a placeholder file is
     * created so the export always succeeds with a meaningful message.
     *
     * Returns a content URI via FileProvider, or null only on a hard I/O failure.
     */
    fun exportAuditLogs(): Uri? {
        return try {
            val file = File(context.filesDir, SECURITY_LOGS)

            if (!file.exists() || file.length() == 0L) {
                // Create a placeholder so the share sheet always opens
                file.parentFile?.mkdirs()
                file.writeText(
                    "Lockify Security Logs\n" +
                    "=====================\n" +
                    "No security events have been recorded yet.\n\n" +
                    "To capture events, enable Debug Logging in Settings > Advanced > Logging.\n\n" +
                    "Exported: ${Instant.now()}\n" +
                    "Package:  ${context.packageName}\n"
                )
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting audit logs", e)
            null
        }
    }

    /**
     * Exports the logcat output to a cache file.
     *
     * Must be called from a background thread — logcat reading is blocking I/O.
     * Returns a content URI via FileProvider, or null on failure.
     */
    fun exportLogs(): Uri? {
        val file = File(context.cacheDir, FILE_NAME)
        return try {
            file.delete()
            file.createNewFile()

            val process = Runtime.getRuntime().exec("logcat -d -v threadtime")
            process.inputStream.bufferedReader().use { reader ->
                file.bufferedWriter().use { writer ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        reader.transferTo(writer)
                    } else {
                        reader.forEachLine { line -> writer.write(line + "\n") }
                    }
                }
            }
            process.waitFor()

            if (file.length() == 0L) {
                file.writeText("No logcat output captured.\nExported: ${Instant.now()}\n")
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting logs", e)
            null
        }
    }

    fun clearAllLogs() {
        try {
            File(context.filesDir, SECURITY_LOGS).takeIf { it.exists() }?.delete()
            File(context.cacheDir, FILE_NAME).takeIf { it.exists() }?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing logs", e)
        }
    }

    fun purgeOldLogs() {
        thread(name = "LogPurgeThread", isDaemon = true) {
            purgeOldLogsFromFile(File(context.filesDir, SECURITY_LOGS), "audit")
            purgeOldLogsFromFile(File(context.cacheDir, FILE_NAME), "app")
        }
    }

    private fun purgeOldLogsFromFile(logFile: File, logType: String) {
        if (!logFile.exists()) return
        try {
            val tempFile = File(context.cacheDir, logFile.name + ".processing")
            val backupFile = File(context.cacheDir, logFile.name + ".backup")
            try {
                logFile.copyTo(backupFile, overwrite = true)
                val cutoff = Instant.now().minus(7, ChronoUnit.DAYS)
                var purged = 0
                var kept = 0
                backupFile.bufferedReader().use { reader ->
                    tempFile.bufferedWriter().use { writer ->
                        reader.forEachLine { line ->
                            val keep = try {
                                Instant.parse(line.substringBefore(" ")).isAfter(cutoff)
                            } catch (_: Exception) { true }
                            if (keep) { writer.write(line); writer.newLine(); kept++ }
                            else purged++
                        }
                    }
                }
                when {
                    kept == 0 -> { logFile.delete(); tempFile.delete() }
                    purged > 0 -> { tempFile.copyTo(logFile, overwrite = true); tempFile.delete() }
                    else -> tempFile.delete()
                }
            } finally {
                backupFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error purging old $logType logs", e)
        }
    }
}
