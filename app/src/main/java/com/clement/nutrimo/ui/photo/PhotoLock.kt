// Privacy of the photo screens: device unlock (fingerprint, face, PIN…) before showing them, and no screenshots.
package com.clement.nutrimo.ui.photo

import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager.Authenticators
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.R

/** Face unlock is "weak" on most phones; the PIN / pattern / password is always accepted as a fallback. */
private const val AUTHENTICATORS = Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL

object PhotoLock {
    /**
     * True during one visit of the photo screens (timeline → camera → comparison). Entering them from elsewhere
     * calls [lock], and so does the app going to the background (MainActivity.onStop).
     */
    var unlocked by mutableStateOf(false)
        private set

    fun lock() {
        unlocked = false
    }

    internal fun unlock() {
        unlocked = true
    }
}

/** Shows [content] once the user has unlocked the device; without any screen lock set up, shows it directly. */
@Composable
fun PhotoLockGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val secure = remember { context.getSystemService(KeyguardManager::class.java).isDeviceSecure }
    if (!secure || PhotoLock.unlocked) return content()
    var failed by remember { mutableStateOf<String?>(null) }
    val ask = { authenticate(context, onSuccess = PhotoLock::unlock, onError = { failed = it }) }
    LaunchedEffect(Unit) { ask() }
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Lock, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.photo_lock_vos_photos_sont_protegees), style = MaterialTheme.typography.titleMedium)
        Text(failed ?: stringResource(R.string.photo_lock_deverrouillez_avec_votre_empreinte_votre), textAlign = TextAlign.Center)
        Button(onClick = ask) { Text(stringResource(R.string.photo_lock_deverrouiller)) }
    }
}

private fun authenticate(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) = try {
    BiometricPrompt.Builder(context)
        .setTitle(context.getString(R.string.photo_lock_title))
        .setSubtitle(context.getString(R.string.photo_lock_subtitle))
        .setAllowedAuthenticators(AUTHENTICATORS)
        .build()
        .authenticate(CancellationSignal(), context.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errString.toString())
        })
} catch (e: RuntimeException) {
    onError(context.getString(R.string.photo_lock_error, e.message.orEmpty()))
}

/** Screens showing photos stay out of screenshots and the recent-apps preview; counted so nested screens don't clear it. */
private var secureScreens = 0

@Composable
fun SecureWindow() {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window) {
        secureScreens++
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            secureScreens--
            if (secureScreens == 0) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
