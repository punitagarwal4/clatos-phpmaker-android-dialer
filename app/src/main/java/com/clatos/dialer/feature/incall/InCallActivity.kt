package com.clatos.dialer.feature.incall

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.clatos.dialer.ui.theme.ClatosTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Hosts the in-call UI. Shows over the lock screen and turns the screen on so a
 * ringing call is visible immediately (US-2.3). Finishes itself when the call
 * ends (the screen observes call state and calls finish()).
 */
@AndroidEntryPoint
class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        setContent {
            ClatosTheme {
                InCallScreen(onCallEnded = { finish() })
            }
        }
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }
}
