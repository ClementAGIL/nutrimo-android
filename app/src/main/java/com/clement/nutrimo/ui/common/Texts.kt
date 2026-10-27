// Text styles repeated across screens: intro paragraph, grey hint, warning.
package com.clement.nutrimo.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.clement.nutrimo.ui.theme.NutrimoColors

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
