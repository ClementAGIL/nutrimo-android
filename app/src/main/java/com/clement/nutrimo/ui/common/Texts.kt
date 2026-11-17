// Text styles repeated across screens: intro paragraph, grey hint, warning.
package com.clement.nutrimo.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.clement.nutrimo.ui.theme.NutrimoColors
import com.clement.nutrimo.util.UiText

/** Explanation at the top of a settings screen. */
@Composable
fun IntroText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

/** Secondary explanation under a value or a control. */
@Composable
fun HintText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun WarningText(text: String) {
    Text(text, color = NutrimoColors.warning, style = MaterialTheme.typography.bodySmall)
}

/** Resolves a [UiText] from a ViewModel or the domain. */
@Composable
fun UiText.asString(): String {
    // Read so a configuration (language) change recomposes with the new resources.
    LocalConfiguration.current
    return resolve(LocalContext.current.resources)
}
