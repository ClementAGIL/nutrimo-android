// Horizontal photo timeline: the weight curve with one point per photo day, scrollable, months along the bottom.
// Long stretches without photos are collapsed into a short "…" break (see TimelineAxis).
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clement.nutrimo.data.db.WeightEntity
import com.clement.nutrimo.ui.theme.NutrimoColors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Wide enough to tell weeks apart; the timeline scrolls when the history is longer than the screen. */
private val DAY_WIDTH = 6.dp
private val HEIGHT = 190.dp
private val TAP_RADIUS = 28.dp
private val MONTH = DateTimeFormatter.ofPattern("MMM yy", Locale.FRANCE)
private const val BREAK_MARK = "…"

/** Maps dates (through the axis, breaks included) and weights to the canvas. */
private class TimelineScale(val axis: TimelineAxis, val width: Float, val top: Float, val bottom: Float, val min: Double, val max: Double) {
    /** Null for a date inside a collapsed stretch. */
    fun x(date: LocalDate): Float? = axis.position(date)?.let { at(it) }
    fun at(position: Long): Float = width * position / axis.totalDays
    fun y(kg: Double?) = if (kg == null || max <= min) (top + bottom) / 2 else bottom - (bottom - top) * ((kg - min) / (max - min)).toFloat()
}

@Composable
fun PhotoTimeline(state: PhotosUiState, onSelect: (LocalDate) -> Unit) {
    val days = state.days
    if (days.isEmpty()) return
    val axis = remember(days) { TimelineAxis(days.map { it.date }, LocalDate.now()) }
    // Only the weigh-ins of the kept periods: years of older history would flatten the curve.
    val shown = state.weights.filter { w -> axis.segments.any { w.date in it } }
    val kgs = shown.map { it.kg } + days.mapNotNull { it.kg }
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val ringColor = MaterialTheme.colorScheme.primary
    BoxWithConstraints {
        val width = maxOf(maxWidth, DAY_WIDTH * axis.totalDays.toInt())
        val scroll = rememberScrollState()
        // Opens on the latest photos.
        LaunchedEffect(Unit) { scroll.scrollTo(snapshotFlow { scroll.maxValue }.first { it > 0 }) }
        Box(Modifier.horizontalScroll(scroll)) {
            Canvas(
                Modifier.width(width).height(HEIGHT).pointerInput(days) {
                    detectTapGestures { tap ->
                        val scale = TimelineScale(axis, size.width.toFloat(), 0f, 0f, 0.0, 0.0)
                        val distance = { day: PhotoDay -> scale.x(day.date)?.let { abs(it - tap.x) } ?: Float.MAX_VALUE }
                        days.minByOrNull(distance)?.takeIf { distance(it) <= TAP_RADIUS.toPx() }?.let { onSelect(it.date) }
                    }
                },
            ) {
                val scale = TimelineScale(
                    axis, size.width, 28.dp.toPx(), size.height - 26.dp.toPx(),
                    (kgs.minOrNull() ?: 0.0) - 1, (kgs.maxOrNull() ?: 0.0) + 1,
                )
                drawMonths(scale, measurer, labelColor)
                drawBreaks(scale, measurer, labelColor)
                drawWeights(scale, shown)
                days.forEach { drawPhotoPoint(scale, it, state, ringColor, measurer) }
            }
        }
    }
}

/** A line and a label at each month start, inside the kept periods only. */
private fun DrawScope.drawMonths(scale: TimelineScale, measurer: TextMeasurer, color: Color) {
    val style = TextStyle(fontSize = 10.sp, color = color)
    scale.axis.segments.forEach { range ->
        var month = range.start.withDayOfMonth(1).plusMonths(1)
        while (!month.isAfter(range.endInclusive)) {
            val x = scale.x(month) ?: break
            drawLine(color.copy(alpha = 0.3f), Offset(x, 16.dp.toPx()), Offset(x, size.height - 18.dp.toPx()), strokeWidth = 1f)
            drawText(measurer.measure(month.format(MONTH), style), topLeft = Offset(x + 3.dp.toPx(), size.height - 16.dp.toPx()))
            month = month.plusMonths(1)
        }
    }
}

/** A dashed line with "…" under it where months without photos were left out. */
private fun DrawScope.drawBreaks(scale: TimelineScale, measurer: TextMeasurer, color: Color) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
    val mark = measurer.measure(BREAK_MARK, TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color))
    scale.axis.breaks.forEach { middle ->
        val x = scale.at(middle)
        drawLine(color.copy(alpha = 0.5f), Offset(x, 16.dp.toPx()), Offset(x, size.height - 18.dp.toPx()), strokeWidth = 1.5f, pathEffect = dash)
        drawText(mark, topLeft = Offset(x - mark.size.width / 2, size.height - 18.dp.toPx()))
    }
}

/** One stretch of curve per kept period: nothing is drawn across a break. */
private fun DrawScope.drawWeights(scale: TimelineScale, weights: List<WeightEntity>) {
    scale.axis.segments.forEach { range ->
        val inside = weights.filter { it.date in range }
        if (inside.isEmpty()) return@forEach
        val path = Path()
        inside.forEachIndexed { i, w ->
            val p = Offset(scale.x(w.date) ?: return@forEachIndexed, scale.y(w.kg))
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, NutrimoColors.weight.copy(alpha = 0.7f), style = Stroke(2.dp.toPx()))
    }
}

/** A blue point per photo day; the selected one is ringed, A and B get their letter above. */
private fun DrawScope.drawPhotoPoint(scale: TimelineScale, day: PhotoDay, state: PhotosUiState, ring: Color, measurer: TextMeasurer) {
    val center = Offset(scale.x(day.date) ?: return, scale.y(day.kg))
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
private fun maxOf(a: Dp, b: Dp) = if (a > b) a else b
