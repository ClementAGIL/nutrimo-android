// Splash state shared between MainActivity (which removes the splash) and screens that animate on first display.
package com.clement.nutrimo.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

/** Same length as the bloom in res/drawable/splash_icon.xml (SPLASH_MS in tools/launcher_icon.py). */
const val SPLASH_ANIMATION_MS = 1000L

/** Safety net when the system shows no splash (warm start, some launchers): never wait longer than this. */
private const val SPLASH_WAIT_MAX_MS = 2500L

object SplashState {
    /** True once the splash has faded out (or was never shown in this process). */
    val gone = MutableStateFlow(false)
}

/** True when the screen can start its own animations without running under the fading splash. */
@Composable
fun splashGone(): Boolean {
    val gone by SplashState.gone.collectAsState()
    var timedOut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SPLASH_WAIT_MAX_MS)
        timedOut = true
    }
    return gone || timedOut
}
