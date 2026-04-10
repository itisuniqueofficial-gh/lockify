package com.itisuniqueofficial.lockify.core.navigation

import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.itisuniqueofficial.lockify.AppLockApplication
import com.itisuniqueofficial.lockify.R
import com.itisuniqueofficial.lockify.core.utils.LogUtils
import com.itisuniqueofficial.lockify.core.utils.launchDeviceCredentialAuth
import com.itisuniqueofficial.lockify.data.repository.PreferencesRepository
import com.itisuniqueofficial.lockify.features.antiuninstall.ui.AntiUninstallScreen
import com.itisuniqueofficial.lockify.features.appintro.ui.AppIntroScreen
import com.itisuniqueofficial.lockify.features.applist.ui.MainScreen
import com.itisuniqueofficial.lockify.features.lockscreen.ui.PasswordOverlayScreen
import com.itisuniqueofficial.lockify.features.lockscreen.ui.PatternLockScreen
import com.itisuniqueofficial.lockify.features.setpassword.ui.PatternSetPasswordScreen
import com.itisuniqueofficial.lockify.features.setpassword.ui.SetPasswordScreen
import com.itisuniqueofficial.lockify.features.settings.ui.SettingsScreen
import com.itisuniqueofficial.lockify.features.triggerexclusions.ui.TriggerExclusionsScreen

@Composable
fun AppNavHost(navController: NavHostController, startDestination: String) {
    val application = LocalContext.current.applicationContext as AppLockApplication

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(ANIMATION_DURATION)) +
                    scaleIn(initialScale = SCALE_INITIAL, animationSpec = tween(ANIMATION_DURATION))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(ANIMATION_DURATION)) +
                    scaleIn(initialScale = SCALE_INITIAL, animationSpec = tween(ANIMATION_DURATION))
        },
    ) {
        composable(Screen.AppIntro.route) {
            AppIntroScreen(navController)
        }

        composable(Screen.SetPassword.route) {
            SetPasswordScreen(navController, isFirstTimeSetup = true)
        }

        composable(Screen.ChangePassword.route) {
            if (application.appLockRepository.getLockType() == PreferencesRepository.LOCK_TYPE_PATTERN) {
                PatternSetPasswordScreen(navController, false)
            } else {
                SetPasswordScreen(navController, isFirstTimeSetup = false)
            }
        }

        composable(Screen.ResetPassword.route) {
            // Arrived here after successful device credential verification — skip old-PIN check
            if (application.appLockRepository.getLockType() == PreferencesRepository.LOCK_TYPE_PATTERN) {
                PatternSetPasswordScreen(navController, isFirstTimeSetup = false, skipOldPasswordVerification = true)
            } else {
                SetPasswordScreen(navController, isFirstTimeSetup = false, skipOldPasswordVerification = true)
            }
        }

        composable(Screen.SetPasswordPattern.route) {
            PatternSetPasswordScreen(navController, isFirstTimeSetup = true)
        }

        composable(Screen.Main.route) {
            MainScreen(navController)
        }

        composable(Screen.PasswordOverlay.route) {
            val context = LocalActivity.current as FragmentActivity
            val lockType = application.appLockRepository.getLockType()

            val onForgotPassword: () -> Unit = {
                launchDeviceCredentialAuth(
                    activity = context,
                    title = context.getString(R.string.forgot_password_verify_title),
                    subtitle = context.getString(R.string.forgot_password_verify_subtitle),
                    onSuccess = {
                        navController.navigate(Screen.ResetPassword.route) {
                            popUpTo(Screen.PasswordOverlay.route) { inclusive = true }
                        }
                    },
                    onNoLock = {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.forgot_password_no_device_lock),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }

            when (lockType) {
                PreferencesRepository.LOCK_TYPE_PATTERN -> {
                    PatternLockScreen(
                        fromMainActivity = true,
                        onPatternAttempt = { pattern ->
                            val isValid = application.appLockRepository.validatePattern(pattern)
                            if (isValid) {
                                handleAuthenticationSuccess(navController)
                            }
                            isValid
                        },
                        onBiometricAuth = {
                            handleBiometricAuthentication(context, navController)
                        },
                        onForgotPassword = onForgotPassword
                    )
                }

                else -> {
                    PasswordOverlayScreen(
                        showBiometricButton = application.appLockRepository.isBiometricAuthEnabled(),
                        fromMainActivity = true,
                        onBiometricAuth = {
                            handleBiometricAuthentication(context, navController)
                        },
                        onAuthSuccess = {
                            handleAuthenticationSuccess(navController)
                        },
                        onForgotPassword = onForgotPassword
                    )
                }
            }
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }

        composable(Screen.TriggerExclusions.route) {
            TriggerExclusionsScreen(navController)
        }

        composable(Screen.AntiUninstall.route) {
            AntiUninstallScreen(navController)
        }
    }
}

private fun handleBiometricAuthentication(
    context: FragmentActivity,
    navController: NavHostController
) {
    try {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            context,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.w(TAG, "Biometric authentication error: $errString ($errorCode)")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    LogUtils.d(TAG, "Biometric authentication succeeded")
                    navigateToMain(navController)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "Biometric authentication failed (not recognized)")
                }
            }
        )

        val promptInfo = createBiometricPromptInfo(context)
        biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
        Log.e(TAG, "Error during biometric authentication", e)
    }
}

private fun createBiometricPromptInfo(context: android.content.Context): BiometricPrompt.PromptInfo {
    return BiometricPrompt.PromptInfo.Builder()
        .setTitle(context.getString(R.string.biometric_prompt_title))
        .setSubtitle(context.getString(R.string.confirm_biometric_subtitle))
        .setNegativeButtonText(context.getString(R.string.use_pin_button))
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
        )
        .setConfirmationRequired(false)
        .build()
}

private fun handleAuthenticationSuccess(navController: NavHostController) {
    if (navController.previousBackStackEntry != null) {
        navController.popBackStack()
    } else {
        navigateToMain(navController)
    }
}

private fun navigateToMain(navController: NavHostController) {
    navController.navigate(Screen.Main.route) {
        popUpTo(Screen.PasswordOverlay.route) { inclusive = true }
    }
}

private const val TAG = "AppNavHost"
private const val ANIMATION_DURATION = 400
private const val SCALE_INITIAL = 0.9f

