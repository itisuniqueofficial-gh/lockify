package com.itisuniqueofficial.lockify.features.backup

import android.content.Context
import android.net.Uri
import com.itisuniqueofficial.lockify.data.repository.AppLockRepository
import org.json.JSONArray
import org.json.JSONObject

/**
 * Exports/imports an encrypted backup of lock configuration: the (already-hashed) credentials,
 * lock type and the locked-app list. Vault files themselves are intentionally not included.
 */
class BackupManager(private val context: Context) {

    private val repo = AppLockRepository(context)

    fun export(dest: Uri, password: CharArray) {
        val json = JSONObject()
            .put("version", 1)
            .put("lockType", repo.getLockType())
            .put("lockedApps", JSONArray(repo.getLockedApps().toList()))
        repo.getPassword()?.let { json.put("passwordHash", it) }
        repo.getPattern()?.let { json.put("patternHash", it) }
        val blob = BackupCrypto.encrypt(json.toString().toByteArray(Charsets.UTF_8), password)
        context.contentResolver.openOutputStream(dest)?.use { it.write(blob) }
            ?: error("Unable to open backup destination")
    }

    /** Decrypts and applies a backup. Throws if the password is wrong (GCM tag mismatch). */
    fun restore(source: Uri, password: CharArray) {
        val blob = context.contentResolver.openInputStream(source)?.use { it.readBytes() }
            ?: error("Unable to open backup file")
        val json = JSONObject(String(BackupCrypto.decrypt(blob, password), Charsets.UTF_8))

        repo.setLockType(json.getString("lockType"))
        if (json.has("passwordHash")) repo.setRawPasswordHash(json.getString("passwordHash"))
        if (json.has("patternHash")) repo.setRawPatternHash(json.getString("patternHash"))
        val apps = json.getJSONArray("lockedApps")
        (0 until apps.length()).forEach { repo.addLockedApp(apps.getString(it)) }
    }
}
