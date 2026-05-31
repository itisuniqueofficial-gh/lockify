package com.itisuniqueofficial.lockify.features.intruder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.content.ContextCompat
import com.itisuniqueofficial.lockify.core.security.VaultCipher
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Silently captures a single front-camera still after repeated failed unlocks and stores it
 * AES-256-GCM encrypted in app-private storage. No preview, no shutter sound, and — by design —
 * no off-device transmission. Opt-in only.
 */
class IntruderCapture(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun capture() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val cm = context.getSystemService(CameraManager::class.java) ?: return
        val frontId = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        } ?: return

        val reader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)
        val thread = HandlerThread("intruder-capture").apply { start() }
        val handler = Handler(thread.looper)

        reader.setOnImageAvailableListener({ r ->
            r.acquireLatestImage()?.use { image ->
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                save(bytes)
            }
            r.close()
            thread.quitSafely()
        }, handler)

        runCatching {
            cm.openCamera(frontId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        .apply { addTarget(reader.surface) }
                    @Suppress("DEPRECATION")
                    camera.createCaptureSession(
                        listOf(reader.surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                session.capture(request.build(), object :
                                    CameraCaptureSession.CaptureCallback() {
                                    override fun onCaptureCompleted(
                                        s: CameraCaptureSession,
                                        req: android.hardware.camera2.CaptureRequest,
                                        result: android.hardware.camera2.TotalCaptureResult
                                    ) {
                                        camera.close()
                                    }
                                }, handler)
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) =
                                camera.close()
                        },
                        handler
                    )
                }

                override fun onDisconnected(camera: CameraDevice) = camera.close()
                override fun onError(camera: CameraDevice, error: Int) = camera.close()
            }, handler)
        }.onFailure { Log.w("IntruderCapture", "capture failed", it) }
    }

    private fun save(jpeg: ByteArray) {
        val dir = File(context.filesDir, "intruders").apply { mkdirs() }
        val target = File(dir, "${System.currentTimeMillis()}.enc")
        ByteArrayInputStream(jpeg).use { input ->
            target.outputStream().use { VaultCipher.encrypt(input, it) }
        }
    }
}
