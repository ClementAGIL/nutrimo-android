// Single activity hosting the Compose navigation graph; plays the splash animation to the end, then fades it out.
package com.clement.nutrimo

import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.window.SplashScreenView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.clement.nutrimo.ui.NutrimoRoot
import com.clement.nutrimo.ui.common.SPLASH_ANIMATION_MS
import com.clement.nutrimo.ui.common.SplashState
import com.clement.nutrimo.ui.theme.NutrimoTheme

private const val SPLASH_FADE_MS = 300L

/** The splash window appears a little after the process starts: keep it this much longer than the bloom. */
private const val SPLASH_MARGIN_MS = 250L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Without a splash (activity recreated), nothing will call the exit listener.
        if (savedInstanceState != null) SplashState.gone.value = true
        splashScreen.setOnExitAnimationListener(::exitSplash)
        enableEdgeToEdge()
        setContent { NutrimoTheme { NutrimoRoot() } }
    }

    /** The app is usually ready before the bloom ends: wait for the icon animation, then fade out. */
    private fun exitSplash(view: SplashScreenView) {
        view.animate().alpha(0f).setStartDelay(bloomLeftMs(view)).setDuration(SPLASH_FADE_MS)
            .withEndAction {
                view.remove()
                SplashState.gone.value = true
            }.start()
    }

    /**
     * Time left in the bloom. Some systems (HyperOS) report no start time or end the splash early,
     * so the time since the process started is used as well and the longer wait wins.
     */
    private fun bloomLeftMs(view: SplashScreenView): Long {
        val start = view.iconAnimationStart?.toEpochMilli()
        val duration = view.iconAnimationDuration?.toMillis()
        val reported = if (start != null && duration != null) start + duration - System.currentTimeMillis() else 0L
        val sinceStart = SystemClock.uptimeMillis() - Process.getStartUptimeMillis()
        val estimated = SPLASH_ANIMATION_MS + SPLASH_MARGIN_MS - sinceStart
        return maxOf(reported, estimated, 0L)
    }
}
