package com.itisuniqueofficial.lockify

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.itisuniqueofficial.lockify.core.navigation.AppNavHost
import com.itisuniqueofficial.lockify.core.navigation.NavigationManager
import com.itisuniqueofficial.lockify.core.navigation.Screen
import com.itisuniqueofficial.lockify.features.lockscreen.ui.PasswordOverlayActivity
import com.itisuniqueofficial.lockify.ui.theme.AppLockTheme

class MainActivity : FragmentActivity() {

    private lateinit var navigationManager: NavigationManager
    private var pendingResetNavigation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        navigationManager = NavigationManager(this)
        pendingResetNavigation = intent.getBooleanExtra(
            PasswordOverlayActivity.EXTRA_NAVIGATE_TO_RESET, false
        )

        setContent {
            AppLockTheme {
                val navController = rememberNavController()
                val startDestination = if (pendingResetNavigation) {
                    Screen.ResetPassword.route
                } else {
                    navigationManager.determineStartDestination()
                }

                AppNavHost(
                    navController = navController,
                    startDestination = startDestination
                )

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    if (!pendingResetNavigation) {
                        handleOnResume(navController)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Handled via fresh activity start with FLAG_ACTIVITY_CLEAR_TOP
    }

    private fun handleOnResume(navController: NavHostController) {
        val currentRoute = navController.currentDestination?.route

        if (navigationManager.shouldSkipPasswordCheck(currentRoute)) {
            return
        }

        if (currentRoute != Screen.PasswordOverlay.route &&
            currentRoute != Screen.SetPassword.route &&
            currentRoute != Screen.SetPasswordPattern.route &&
            currentRoute != Screen.ResetPassword.route
        ) {
            navController.navigate(Screen.PasswordOverlay.route)
        }
    }
}



