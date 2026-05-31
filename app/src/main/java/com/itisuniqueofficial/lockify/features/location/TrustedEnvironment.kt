package com.itisuniqueofficial.lockify.features.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.itisuniqueofficial.lockify.core.location.GeoFence
import com.itisuniqueofficial.lockify.core.location.LocationRuleRepository

/**
 * Decides whether locking should be relaxed for the current environment:
 * either connected to a trusted Wi-Fi SSID, or inside an "unlock-within" geofence.
 */
class TrustedEnvironment(private val context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("trusted_env", Context.MODE_PRIVATE)
    private val rules = LocationRuleRepository(context)

    fun trustedSsids(): Set<String> = prefs.getStringSet(KEY_SSIDS, emptySet()) ?: emptySet()
    fun setTrustedSsids(ssids: Set<String>) = prefs.edit { putStringSet(KEY_SSIDS, ssids) }

    fun shouldRelaxLocking(): Boolean = onTrustedWifi() || insideUnlockGeofence()

    private fun onTrustedWifi(): Boolean {
        val current = currentSsid() ?: return false
        return current in trustedSsids()
    }

    @Suppress("DEPRECATION")
    private fun currentSsid(): String? {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null
        // SSID is quoted by the framework, e.g. "MyNetwork"; strip the quotes.
        return wifi.connectionInfo?.ssid?.trim('"')?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
    }

    private fun insideUnlockGeofence(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return false

        val lm = context.getSystemService(LocationManager::class.java) ?: return false
        val loc = runCatching {
            lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
        }.getOrNull() ?: return false

        return rules.getAll().any {
            it.enabled && it.unlockWithin && GeoFence.isWithin(it, loc.latitude, loc.longitude)
        }
    }

    private companion object {
        const val KEY_SSIDS = "trusted_ssids"
    }
}
