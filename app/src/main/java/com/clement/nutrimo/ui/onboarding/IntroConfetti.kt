// Confetti burst for the tour's streak scene: pieces shoot upward from a point, then fall back with gravity.
package com.clement.nutrimo.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.clement.nutrimo.ui.theme.NutrimoColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

const val CONFETTI_MS = 1800
private const val PIECES = 36
private const val GRAVITY_DP = 900f
private const val FADE_FROM = 0.7f
private val COLORS = listOf(NutrimoColors.caution, NutrimoColors.water, NutrimoColors.fat, NutrimoColors.sport, NutrimoColors.weight, Color.White)

private class Piece(val angle: Float, val speedDp: Float, val spin: Float, val color: Color, val round: Boolean, val sizeDp: Float)

/** Fixed seed: the same burst every time, so it looks designed rather than noisy. */
private fun pieces(): List<Piece> {
    val random = Random(7)
    return List(PIECES) {
        Piece(
            // Upward cone: from 150° to 30° above the horizon.
            angle = Math.toRadians(-150.0 + 120.0 * random.nextDouble()).toFloat(),
            speedDp = 260f + 300f * random.nextFloat(),
            spin = -720f + 1440f * random.nextFloat(),
            color = COLORS[it % COLORS.size],
            round = random.nextInt(3) == 0,
            sizeDp = 5f + 4f * random.nextFloat(),
        )
    }
}

/** Draws the burst at [progress] (0 → 1 over [CONFETTI_MS]) around the center of [modifier]'s area. */
@Composable
fun ConfettiBurst(progress: Float, modifier: Modifier) {
    val pieces = remember { pieces() }
    if (progress <= 0f || progress >= 1f) return
    Canvas(modifier) {
        val seconds = progress * CONFETTI_MS / 1000f
        val alpha = 1f - progress.phase(FADE_FROM, 1f)
        pieces.forEach { p ->
            val x = cos(p.angle) * p.speedDp * seconds
            val y = sin(p.angle) * p.speedDp * seconds + GRAVITY_DP * seconds * seconds / 2f
            val at = center + Offset(x.dp(this), y.dp(this))
            val s = p.sizeDp.dp(this)
            if (p.round) {
                drawCircle(p.color.copy(alpha = alpha), s / 2f, at)
            } else {
                rotate(p.spin * seconds, at) {
                    drawRoundRect(p.color.copy(alpha = alpha), at - Offset(s / 3f, s / 2f), Size(s * 2f / 3f, s), CornerRadius(s / 6f))
                }
            }
        }
    }
}

private fun Float.dp(scope: androidx.compose.ui.unit.Density): Float = with(scope) { this@dp * density }
