package com.itisuniqueofficial.lockify.features.vault

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.itisuniqueofficial.lockify.core.security.VaultCipher
import java.io.File

/** Stores user files encrypted with [VaultCipher] inside app-private storage. */
class VaultRepository(private val context: Context) {

    private val dir = File(context.filesDir, "vault").apply { mkdirs() }

    data class VaultEntry(val file: File, val displayName: String, val sizeBytes: Long)

    fun list(): List<VaultEntry> =
        (dir.listFiles { f -> f.extension == EXT } ?: emptyArray())
            .map { VaultEntry(it, it.name.substringAfter('_').removeSuffix(".$EXT"), it.length()) }
            .sortedByDescending { it.file.lastModified() }

    /** Encrypts the content behind [source] into the vault. */
    fun import(source: Uri) {
        val name = queryName(source)
        val target = File(dir, "${System.currentTimeMillis()}_$name.$EXT")
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { out -> VaultCipher.encrypt(input, out) }
        } ?: error("Unable to open source")
    }

    /** Decrypts [entry] to the location behind [dest]. */
    fun export(entry: File, dest: Uri) {
        entry.inputStream().use { input ->
            context.contentResolver.openOutputStream(dest)?.use { out ->
                VaultCipher.decrypt(input, out)
            } ?: error("Unable to open destination")
        }
    }

    /** Overwrites the ciphertext with zeros before unlinking (best-effort secure delete). */
    fun delete(entry: File) {
        runCatching {
            val len = entry.length()
            entry.outputStream().use { out ->
                val buf = ByteArray(8192)
                var written = 0L
                while (written < len) {
                    val n = minOf(buf.size.toLong(), len - written).toInt()
                    out.write(buf, 0, n)
                    written += n
                }
                out.flush()
            }
        }
        entry.delete()
    }

    private fun queryName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0)?.let { return it } }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "file"
    }

    companion object {
        private const val EXT = "enc"
    }
}
