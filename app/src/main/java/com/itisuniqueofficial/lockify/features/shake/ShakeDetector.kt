package com.itisuniqueofficial.lockify.features.shake

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detects a deliberate shake from the accelerometer and invokes [onShake]. Register with
 * [start] from a running service and release with [stop].
 */
class ShakeDetector(context: Context, private val onShake: () -> Unit) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastShakeAt = 0L

    fun start() {
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() = sensorManager.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        val gForce = sqrt(
            (event.values[0] * event.values[0] +
                    event.values[1] * event.values[1] +
                    event.values[2] * event.values[2]).toDouble()
        ) / SensorManager.GRAVITY_EARTH
        val now = System.currentTimeMillis()
        if (gForce > SHAKE_THRESHOLD_G && now - lastShakeAt > SHAKE_COOLDOWN_MS) {
            lastShakeAt = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private companion object {
        const val SHAKE_THRESHOLD_G = 2.7
        const val SHAKE_COOLDOWN_MS = 1000L
    }
}
