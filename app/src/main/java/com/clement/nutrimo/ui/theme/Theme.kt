// Material 3 theme: a fixed fresh-green brand palette (light and dark), white rounded cards on a tinted background.
package com.clement.nutrimo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Fixed chart / macro colors, readable on both light and dark backgrounds. */
object NutrimoColors {
    val protein = Color(0xFF4F8DF7)
    val carbs = Color(0xFFF2A93B)
    val fat = Color(0xFFE5677A)
    val weight = Color(0xFF2E9E6E)
    val objective = Color(0xFF8A8A8A)
    val trend = Color(0xFFB36AE2)
    val warning = Color(0xFFE08A00)
    val water = Color(0xFF29A8E0)
    /** Over the calorie goal but still under maintenance: still losing, just slower. */
    val caution = Color(0xFFE8B923)

    // Daily goals keep the same color in the journal and the history calendar.
    val steps = fat
    val sport = trend
}

// Cards use `surface` and screens `background`: white cards on a light green-grey page (dark: lifted cards).
private val LightColors = lightColorScheme(
    primary = Color(0xFF1F7A55),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4F1D2),
    onPrimaryContainer = Color(0xFF002113),
    secondary = Color(0xFF4C6358),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCEE9DA),
    onSecondaryContainer = Color(0xFF092016),
    tertiary = Color(0xFF3D6374),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC1E8FC),
    onTertiaryContainer = Color(0xFF001F2A),
    background = Color(0xFFF1F6F3),
    onBackground = Color(0xFF171D1A),
    surface = Color.White,
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDCE5DE),
    onSurfaceVariant = Color(0xFF404943),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F9F6),
    surfaceContainer = Color(0xFFEAF1EC),
    surfaceContainerHigh = Color(0xFFE4EBE6),
    surfaceContainerHighest = Color(0xFFDEE5E0),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFC0C9C2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD8B1),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF005237),
    onPrimaryContainer = Color(0xFFA7F4CC),
    secondary = Color(0xFFB2CCBE),
    onSecondary = Color(0xFF1E352A),
    secondaryContainer = Color(0xFF354B40),
    onSecondaryContainer = Color(0xFFCEE9DA),
    tertiary = Color(0xFFA5CCE0),
    onTertiary = Color(0xFF073544),
    tertiaryContainer = Color(0xFF244B5B),
    onTertiaryContainer = Color(0xFFC1E8FC),
    background = Color(0xFF0E1411),
    onBackground = Color(0xFFDEE4DF),
    surface = Color(0xFF19201C),
    onSurface = Color(0xFFDEE4DF),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C2),
    surfaceContainerLowest = Color(0xFF090F0C),
    surfaceContainerLow = Color(0xFF171D1A),
    surfaceContainer = Color(0xFF1B211E),
    surfaceContainerHigh = Color(0xFF252B28),
    surfaceContainerHighest = Color(0xFF303633),
    outline = Color(0xFF8A938C),
    outlineVariant = Color(0xFF404943),
)

private val NutrimoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
)

private val NutrimoTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun NutrimoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = NutrimoShapes,
        typography = NutrimoTypography,
        content = content,
    )
}
