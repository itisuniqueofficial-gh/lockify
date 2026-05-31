package com.itisuniqueofficial.lockify.core.location

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A circular geofence. [action] UNLOCK_WITHIN relaxes locking inside the radius. */
data class LocationRule(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val unlockWithin: Boolean = true,
    val enabled: Boolean = true
)

object GeoFence {
    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Great-circle distance between two coordinates, in metres. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun isWithin(rule: LocationRule, lat: Double, lon: Double): Boolean =
        distanceMeters(rule.latitude, rule.longitude, lat, lon) <= rule.radiusMeters
}

/** Persists location rules as JSON in SharedPreferences. */
class LocationRuleRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("location_rules", Context.MODE_PRIVATE)

    fun getAll(): List<LocationRule> {
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            LocationRule(
                o.getLong("id"), o.getString("name"), o.getDouble("lat"), o.getDouble("lon"),
                o.getDouble("radius").toFloat(), o.getBoolean("unlock"), o.getBoolean("enabled")
            )
        }
    }

    fun save(rule: LocationRule) = persist(getAll().filter { it.id != rule.id } + rule)
    fun delete(id: Long) = persist(getAll().filter { it.id != id })

    private fun persist(list: List<LocationRule>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("name", it.name).put("lat", it.latitude)
                    .put("lon", it.longitude).put("radius", it.radiusMeters.toDouble())
                    .put("unlock", it.unlockWithin).put("enabled", it.enabled)
            )
        }
        prefs.edit { putString(KEY, arr.toString()) }
    }

    private companion object {
        const val KEY = "rules"
    }
}
