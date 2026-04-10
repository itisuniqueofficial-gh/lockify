package com.itisuniqueofficial.lockify.core.navigation

sealed class Screen(val route: String) {
    object AppIntro : Screen("app_intro")
    object SetPassword : Screen("set_password")
    object SetPasswordPattern : Screen("set_password_pattern")
    object ChangePassword : Screen("change_password")
    object ResetPassword : Screen("reset_password")   // post device-auth reset, skips old-PIN check
    object Main : Screen("main")
    object PasswordOverlay : Screen("password_overlay")
    object Settings : Screen("settings")
    object TriggerExclusions : Screen("trigger_exclusions")
    object AntiUninstall: Screen("anti_uninstall")
}

