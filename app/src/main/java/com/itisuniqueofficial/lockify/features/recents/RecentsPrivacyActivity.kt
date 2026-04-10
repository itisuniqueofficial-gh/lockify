package com.itisuniqueofficial.lockify.features.recents

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.itisuniqueofficial.lockify.R
import com.itisuniqueofficial.lockify.features.lockscreen.ui.PasswordOverlayActivity
import com.itisuniqueofficial.lockify.services.AppLockManager
import com.itisuniqueofficial.lockify.ui.theme.AppLockTheme

/**
 * Privacy placeholder activity shown in the Recent Apps / Overview screen instead of
 * the real protected app content — identical in concept to Chrome Incognito's recents card.
 *
 * How it works:
 *  1. When a protected app goes to background/recents, the accessibility service launches
 *     this activity into the protected app's task (same taskAffinity).
 *  2. Android snapshots *this* activity for the recents thumbnail — not the real app UI.
 *  3. FLAG_SECURE prevents any screenshot or screen-recording of this placeholder too.
 *  4. When the user taps the card in recents, this activity resumes and immediately
 *     launches the lock screen, then finishes itself.
 */
class RecentsPrivacyActivity : ComponentActivity() {

    private var lockedPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lockedPackage = intent.getStringExtra(EXTRA_LOCKED_PACKAGE)

        // Apply FLAG_SECURE so this placeholder itself cannot be captured
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        // Disable back navigation — user must go through auth
        onBackPressedDispatcher.addCallback(this) { /* consume back */ }

        enableEdgeToEdge()

        setContent {
            AppLockTheme {
                RecentsPrivacyPlaceholder()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Every time this activity comes to the foreground (i.e. user tapped it in recents),
        // clear any stale unlock state and show the lock screen immediately.
        val pkg = lockedPackage ?: run { finish(); return }

        AppLockManager.clearTemporarilyUnlockedApp()
        AppLockManager.appUnlockTimes.remove(pkg)

        if (!AppLockManager.isLockScreenShown.get()) {
            showLockScreen(pkg)
        }
    }

    private fun showLockScreen(packageName: String) {
        val intent = Intent(this, PasswordOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("locked_package", packageName)
            putExtra("triggering_package", packageName)
        }
        startActivity(intent)
        // Don't finish — keep this activity alive so the recents card stays as the placeholder.
        // The lock screen sits on top; when auth succeeds the overlay finishes and the real
        // app is revealed (or the user navigates away).
    }

    companion object {
        const val EXTRA_LOCKED_PACKAGE = "locked_package"

        /** Build the intent used to launch this placeholder into a protected app's task. */
        fun buildIntent(context: android.content.Context, lockedPackage: String): Intent =
            Intent(context, RecentsPrivacyActivity::class.java).apply {
                // NEW_TASK so it can be started from a service.
                // NO_ANIMATION so there is no visible transition flash.
                // REORDER_TO_FRONT so if already in the task it comes to front instantly.
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(EXTRA_LOCKED_PACKAGE, lockedPackage)
            }
    }
}

@Composable
private fun RecentsPrivacyPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                // Lock icon badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_shield_24),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.recents_privacy_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.recents_privacy_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
