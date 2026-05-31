package com.itisuniqueofficial.lockify.features.profiles

import android.content.Context
import androidx.core.content.edit
import com.itisuniqueofficial.lockify.core.security.CredentialHasher
import org.json.JSONArray
import org.json.JSONObject

/**
 * A lock profile (e.g. Work, Personal, Kids). Each profile has its own hashed PIN and its own
 * set of packages. When [whitelistMode] is true (child mode) every app is locked EXCEPT the
 * listed packages; otherwise only the listed packages are locked.
 */
data class LockProfile(
    val id: Long,
    val name: String,
    val pinHash: String,
    val packages: Set<String>,
    val whitelistMode: Boolean = false
) {
    /** Pure decision: should [pkg] be locked under this profile? */
    fun shouldLock(pkg: String): Boolean =
        if (whitelistMode) pkg !in packages else pkg in packages

    fun verifyPin(input: String): Boolean = CredentialHasher.verify(input, pinHash)
}

/** Persists profiles (and the active profile id) as JSON in SharedPreferences. */
class ProfileRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("lock_profiles", Context.MODE_PRIVATE)

    fun getAll(): List<LockProfile> {
        val arr = JSONArray(prefs.getString(KEY_PROFILES, "[]"))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val pkgs = o.getJSONArray("packages")
            LockProfile(
                o.getLong("id"), o.getString("name"), o.getString("pin"),
                (0 until pkgs.length()).map { pkgs.getString(it) }.toSet(),
                o.getBoolean("whitelist")
            )
        }
    }

    fun activeProfile(): LockProfile? {
        val id = prefs.getLong(KEY_ACTIVE, -1L)
        return getAll().firstOrNull { it.id == id }
    }

    fun setActive(id: Long) = prefs.edit { putLong(KEY_ACTIVE, id) }

    /** Creates/updates a profile, hashing [rawPin]. */
    fun save(id: Long, name: String, rawPin: String, packages: Set<String>, whitelistMode: Boolean) {
        val profile = LockProfile(id, name, CredentialHasher.hash(rawPin), packages, whitelistMode)
        persist(getAll().filter { it.id != id } + profile)
    }

    fun delete(id: Long) = persist(getAll().filter { it.id != id })

    private fun persist(list: List<LockProfile>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id).put("name", p.name).put("pin", p.pinHash)
                    .put("packages", JSONArray(p.packages.toList()))
                    .put("whitelist", p.whitelistMode)
            )
        }
        prefs.edit { putString(KEY_PROFILES, arr.toString()) }
    }

    private companion object {
        const val KEY_PROFILES = "profiles"
        const val KEY_ACTIVE = "active_profile"
    }
}
