// Generic date/value chart drawn on a Canvas (no chart library): grid, axis labels, daily bars, lines and dots.
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.short
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val Y_TICKS = 5
private const val X_TICKS = 4
private const val MAX_DOTS = 60
/** Share of a day's width left empty on each side of a bar. */
private const val BAR_GAP = 0.15f

data class ChartPoint(val date: LocalDate, val value: Double)

data class ChartSeries(val points: List<ChartPoint>, val color: Color, val dashed: Boolean = false, val dots: Boolean = false)

/** Visible window: dates [start]..[end], values [min]..[max]. */
data class ChartWindow(val start: LocalDate, val end: LocalDate, val min: Double, val max: Double) {
    val days: Float get() = ChronoUnit.DAYS.between(start, end).coerceAtLeast(1).toFloat()
}

/** [bars] are drawn as one column per day, spanning [date, date + 1) — give the window one extra day. */
@Composable
fun LineChart(
    series: List<ChartSeries>,
    window: ChartWindow,
    modifier: Modifier = Modifier,
    bars: List<ChartPoint> = emptyList(),
    barColor: Color = Color.Gray,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val area = PlotArea(left = 36.dp.toPx(), top = 8.dp.toPx(), right = size.width - 8.dp.toPx(), bottom = size.height - 20.dp.toPx(), window)
        drawGrid(area, measurer, labelStyle, gridColor)
        clipRect(area.left, area.top, area.right, area.bottom) {
            bars.forEach { drawBar(area, it, barColor) }
            series.forEach { drawSeries(area, it) }
        }
    }
}

private class PlotArea(val left: Float, val top: Float, val right: Float, val bottom: Float, val w: ChartWindow) {
    fun x(date: LocalDate) = left + (right - left) * ChronoUnit.DAYS.between(w.start, date) / w.days
    fun y(value: Double) = bottom - (bottom - top) * ((value - w.min) / (w.max - w.min)).toFloat()
}

private fun DrawScope.drawGrid(area: PlotArea, measurer: TextMeasurer, style: TextStyle, color: Color) {
    for (i in 0..Y_TICKS) {
        val value = area.w.min + (area.w.max - area.w.min) * i / Y_TICKS
        val y = area.y(value)
        drawLine(color, Offset(area.left, y), Offset(area.right, y), strokeWidth = 1f)
        val label = measurer.measure(fmt(value), style)
        drawText(label, topLeft = Offset(area.left - label.size.width - 4.dp.toPx(), y - label.size.height / 2))
    }
    for (i in 0..X_TICKS) {
        val date = area.w.start.plusDays((area.w.days * i / X_TICKS).toLong())
        val x = area.x(date)
        val label = measurer.measure(date.short(), style)
        val left = (x - label.size.width / 2).coerceIn(0f, size.width - label.size.width)
        drawText(label, topLeft = Offset(left, area.bottom + 4.dp.toPx()))
    }
    val today = LocalDate.now()
    if (!today.isBefore(area.w.start) && !today.isAfter(area.w.end)) {
        drawLine(color, Offset(area.x(today), area.top), Offset(area.x(today), area.bottom), strokeWidth = 2f)
    }
}

private fun DrawScope.drawBar(area: PlotArea, p: ChartPoint, color: Color) {
    val left = area.x(p.date)
    val width = area.x(p.date.plusDays(1)) - left
    val top = area.y(p.value)
    drawRect(color, Offset(left + width * BAR_GAP, top), Size(width * (1 - 2 * BAR_GAP), area.bottom - top))
}

private fun DrawScope.drawSeries(area: PlotArea, s: ChartSeries) {
    if (s.points.isEmpty()) return
    val path = Path()
    s.points.forEachIndexed { i, p ->
        if (i == 0) path.moveTo(area.x(p.date), area.y(p.value)) else path.lineTo(area.x(p.date), area.y(p.value))
    }
    val effect = if (s.dashed) PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx())) else null
    drawPath(path, s.color, style = Stroke(width = 2.5.dp.toPx(), pathEffect = effect))
    if (s.dots && s.points.size <= MAX_DOTS) {
        s.points.forEach { drawCircle(s.color, 3.dp.toPx(), Offset(area.x(it.date), area.y(it.value))) }
    }
}
