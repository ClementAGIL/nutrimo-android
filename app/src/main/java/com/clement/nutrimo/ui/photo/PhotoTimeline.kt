// Horizontal photo timeline: the weight curve with one point per photo day, scrollable, months along the bottom.
package com.clement.nutrimo.ui.photo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clement.nutrimo.data.db.WeightEntity
import com.clement.nutrimo.ui.theme.NutrimoColors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/** Wide enough to tell weeks apart; the timeline scrolls when the history is longer than the screen. */
private val DAY_WIDTH = 6.dp
private val HEIGHT = 190.dp
private val TAP_RADIUS = 28.dp
private const val MARGIN_DAYS = 10L
private val MONTH = DateTimeFormatter.ofPattern("MMM yy", Locale.FRANCE)

/** Maps dates and weights to the canvas. */
private class TimelineScale(val first: LocalDate, val span: Long, val width: Float, val top: Float, val bottom: Float, val min: Double, val max: Double) {
    fun x(date: LocalDate) = width * ChronoUnit.DAYS.between(first, date) / span
    fun y(kg: Double?) = if (kg == null || max <= min) (top + bottom) / 2 else bottom - (bottom - top) * ((kg - min) / (max - min)).toFloat()
}

@Composable
fun PhotoTimeline(state: PhotosUiState, onSelect: (LocalDate) -> Unit) {
    val days = state.days
    if (days.isEmpty()) return
    val today = LocalDate.now()
    val first = minOf(days.first().date, state.weights.firstOrNull()?.date ?: today).minusDays(MARGIN_DAYS)
    val last = maxOf(days.last().date, today).plusDays(MARGIN_DAYS)
    val span = ChronoUnit.DAYS.between(first, last).coerceAtLeast(1)
    val kgs = state.weights.map { it.kg } + days.mapNotNull { it.kg }
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val ringColor = MaterialTheme.colorScheme.primary
    BoxWithConstraints {
        val width = maxOf(maxWidth, DAY_WIDTH * span.toInt())
        val scroll = rememberScrollState()
        // Opens on the latest photos.
        LaunchedEffect(Unit) { scroll.scrollTo(snapshotFlow { scroll.maxValue }.first { it > 0 }) }
        Box(Modifier.horizontalScroll(scroll)) {
            Canvas(
                Modifier.width(width).height(HEIGHT).pointerInput(days) {
                    detectTapGestures { tap ->
                        val scale = TimelineScale(first, span, size.width.toFloat(), 0f, 0f, 0.0, 0.0)
                        days.minByOrNull { abs(scale.x(it.date) - tap.x) }
                            ?.takeIf { abs(scale.x(it.date) - tap.x) <= TAP_RADIUS.toPx() }
                            ?.let { onSelect(it.date) }
                    }
                },
            ) {
                val scale = TimelineScale(
                    first, span, size.width, 28.dp.toPx(), size.height - 26.dp.toPx(),
                    (kgs.minOrNull() ?: 0.0) - 1, (kgs.maxOrNull() ?: 0.0) + 1,
                )
                drawMonths(scale, last, measurer, labelColor)
                drawWeights(scale, state.weights)
                days.forEach { drawPhotoPoint(scale, it, state, ringColor, measurer) }
            }
        }
    }
}

private fun DrawScope.drawMonths(scale: TimelineScale, last: LocalDate, measurer: TextMeasurer, color: Color) {
    var month = scale.first.withDayOfMonth(1).plusMonths(1)
    val style = TextStyle(fontSize = 10.sp, color = color)
    while (!month.isAfter(last)) {
        val x = scale.x(month)
        drawLine(color.copy(alpha = 0.3f), Offset(x, 16.dp.toPx()), Offset(x, size.height - 18.dp.toPx()), strokeWidth = 1f)
        drawText(measurer.measure(month.format(MONTH), style), topLeft = Offset(x + 3.dp.toPx(), size.height - 16.dp.toPx()))
        month = month.plusMonths(1)
    }
}

private fun DrawScope.drawWeights(scale: TimelineScale, weights: List<WeightEntity>) {
    if (weights.isEmpty()) return
    val path = Path()
    weights.forEachIndexed { i, w ->
        val p = Offset(scale.x(w.date), scale.y(w.kg))
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    drawPath(path, NutrimoColors.weight.copy(alpha = 0.7f), style = Stroke(2.dp.toPx()))
}

/** A blue point per photo day; the selected one is ringed, A and B get their letter above. */
private fun DrawScope.drawPhotoPoint(scale: TimelineScale, day: PhotoDay, state: PhotosUiState, ring: Color, measurer: TextMeasurer) {
    val center = Offset(scale.x(day.date), scale.y(day.kg))
    val selected = day.date == state.selected
    if (selected) drawCircle(ring, 14.dp.toPx(), center, style = Stroke(3.dp.toPx()))
    drawCircle(Color.White, 10.dp.toPx(), center)
    drawCircle(NutrimoColors.water, 8.dp.toPx(), center)
    val letters = listOfNotNull("A".takeIf { state.a?.date == day.date }, "B".takeIf { state.b?.date == day.date }).joinToString("")
    if (letters.isEmpty()) return
    val text = measurer.measure(letters, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ring))
    drawText(text, topLeft = Offset(center.x - text.size.width / 2, center.y - 16.dp.toPx() - text.size.height))
}

/** Avoids a zero-width canvas when the history is a single day. */
private fun maxOf(a: androidx.compose.ui.unit.Dp, b: androidx.compose.ui.unit.Dp) = if (a > b) a else b
