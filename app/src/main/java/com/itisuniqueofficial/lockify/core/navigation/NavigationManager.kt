package com.itisuniqueofficial.lockify.core.navigation

import android.content.Context
import com.itisuniqueofficial.lockify.core.utils.appLockRepository
import com.itisuniqueofficial.lockify.features.appintro.domain.AppIntroManager

/**
 * Manages navigation logic and routing decisions for the application.
 * Centralizes navigation-related business logic for better maintainability.
 */
class NavigationManager(private val context: Context) {

    /**
     * Determines the appropriate starting destination based on app state.
     */
    fun determineStartDestination(): String {
        return when {
            shouldShowAppIntro() -> Screen.AppIntro.route
            !isPasswordSet() -> Screen.SetPassword.route
            else -> Screen.PasswordOverlay.route
        }
    }

    /**
     * Checks if password verification should be skipped for the given route.
     */
    fun shouldSkipPasswordCheck(currentRoute: String?): Boolean {
        return currentRoute in ROUTES_THAT_SKIP_PASSWORD_CHECK
    }

    private fun shouldShowAppIntro(): Boolean = AppIntroManager.shouldShowIntro(context)

    private fun isPasswordSet(): Boolean {
        val repo = context.appLockRepository()
        return repo.getPassword() != null || repo.getPattern() != null
    }

    companion object {
        private val ROUTES_THAT_SKIP_PASSWORD_CHECK = setOf(
            Screen.AppIntro.route,
            Screen.SetPassword.route,
            Screen.SetPasswordPattern.route,
            Screen.ChangePassword.route,
            Screen.ResetPassword.route,
            Screen.PasswordOverlay.route
        )
    }
}

