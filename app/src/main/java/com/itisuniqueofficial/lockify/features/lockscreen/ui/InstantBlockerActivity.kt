package com.itisuniqueofficial.lockify.features.lockscreen.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.itisuniqueofficial.lockify.services.PrivacyProtectionManager
import com.itisuniqueofficial.lockify.ui.theme.AppLockTheme

/**
 * Instant blocker activity that appears immediately when a protected app is detected.
 * This provides an immediate privacy barrier while the full lock screen loads.
 * 
 * This activity is intentionally minimal and fast to launch, preventing any
 * glimpse of protected app content during the transition.
 */
class InstantBlockerActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply maximum security and privacy flags immediately
        setupSecureWindow()
        
        enableEdgeToEdge()
        
        // Show minimal blocking UI instantly
        setContent {
            AppLockTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
        
        // Immediately launch the full lock screen
        launchFullLockScreen()
    }
    
    private fun setupSecureWindow() {
        // Apply FLAG_SECURE to prevent screenshots and recents preview
        PrivacyProtectionManager.secureActivity(this)
        
        val flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SECURE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE // Prevent interaction during transition
        
        @Suppress("DEPRECATION")
        val legacyFlags = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        } else 0
        
        window.addFlags(flags or legacyFlags)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setHideOverlayWindows(true)
        }
        
        window.attributes = window.attributes.apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
    }
    
    private fun launchFullLockScreen() {
        val lockedPackage = intent.getStringExtra("locked_package")
        val triggeringPackage = intent.getStringExtra("triggering_package")
        
        if (lockedPackage != null) {
            val lockIntent = android.content.Intent(this, PasswordOverlayActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        android.content.Intent.FLAG_FROM_BACKGROUND or
                        android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra("locked_package", lockedPackage)
                putExtra("triggering_package", triggeringPackage)
            }
            
            startActivity(lockIntent)
        }
        
        // Finish this blocker activity after launching the full lock screen
        finish()
    }
    
    override fun onDestroy() {
        PrivacyProtectionManager.unsecureActivity(this)
        super.onDestroy()
    }
}
