package com.itisuniqueofficial.lockify.core.utils

import android.app.KeyguardManager
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Launches Android's device credential confirmation (PIN/pattern/password/biometric).
 * Calls [onSuccess] if the user verifies successfully, [onFailure] otherwise.
 * Calls [onNoLock] if the device has no secure lock configured.
 */
fun launchDeviceCredentialAuth(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onFailure: () -> Unit = {},
    onNoLock: () -> Unit = {}
) {
    val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    if (!keyguardManager.isDeviceSecure) {
        onNoLock()
        return
    }

    val executor = ContextCompat.getMainExecutor(activity)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // ERROR_NEGATIVE_BUTTON / ERROR_USER_CANCELED = user cancelled, not a failure
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_USER_CANCELED
                ) {
                    onFailure()
                }
            }

            override fun onAuthenticationFailed() {
                // Individual attempt failed — BiometricPrompt handles retries internally
            }
        }
    )
    prompt.authenticate(promptInfo)
}
