// Material 3 theme: dynamic colors (Android 12+, always available with minSdk 31), light and dark.
package com.clement.nutrimo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Fixed chart / macro colors, readable on both light and dark backgrounds. */
object NutrimoColors {
    val protein = Color(0xFF4F8DF7)
    val carbs = Color(0xFFF2A93B)
    val fat = Color(0xFFE5677A)
    val weight = Color(0xFF2E9E6E)
    val objective = Color(0xFF8A8A8A)
    val trend = Color(0xFFB36AE2)
    val warning = Color(0xFFE08A00)
}

@Composable
fun NutrimoTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = scheme, content = content)
}
