// Drawing of a LineChart: plot area (date / value -> pixels), value zones, grid and axis labels, bars, lines,
// and the marker shapes (also used by the chart legend).
package com.clement.nutrimo.ui.common.chart

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.util.fmt
import com.clement.nutrimo.util.short
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToLong

private const val Y_TICKS = 5
private const val X_TICKS = 4
private const val MAX_DOTS = 60
/** Share of a day's width left empty on each side of a bar. */
private const val BAR_GAP = 0.15f
private const val BAND_ALPHA = 0.16f
/** Beyond this visible span the date labels show month and year instead of day and month. */
private const val LONG_SPAN_DAYS = 240f
private val MONTH_YEAR = DateTimeFormatter.ofPattern("MM/yyyy", Locale.FRANCE)
/** Where months without data were left out (gap axis). */
private const val BREAK_MARK = "…"

internal fun Density.plotArea(width: Float, height: Float, markerRows: Int, window: ChartWindow, axis: GapAxis? = null) = PlotArea(
    PLOT_LEFT.toPx(), PLOT_TOP.toPx(), width - PLOT_RIGHT.toPx(), height - PLOT_BOTTOM.toPx() - markerRows * MARKER_ROW.toPx(), window, axis,
)

/** With [axis], dates are placed on it (long gaps collapsed); otherwise linearly between the window's dates. */
internal class PlotArea(val left: Float, val top: Float, val right: Float, val bottom: Float, val w: ChartWindow, val axis: GapAxis? = null) {
    fun x(date: LocalDate): Float =
        if (axis != null) at(axis.place(date)) else left + (right - left) * ChronoUnit.DAYS.between(w.start, date) / w.days

    /** x of a position on [axis], in days from its left edge. */
    fun at(position: Long): Float = left + (right - left) * position / (axis?.totalDays ?: 1L)
    fun y(value: Double) = bottom - (bottom - top) * ((value - w.min) / (w.max - w.min)).toFloat()
    fun dateAt(x: Float): LocalDate = w.start.plusDays(((x - left) / (right - left) * w.days).roundToLong().coerceIn(0, w.days.toLong()))
}

internal fun DrawScope.drawBand(area: PlotArea, b: ChartBand, measurer: TextMeasurer, style: TextStyle) {
    val top = area.y(b.to.coerceIn(area.w.min, area.w.max))
    val bottom = area.y(b.from.coerceIn(area.w.min, area.w.max))
    if (bottom - top < 1f) return
    drawRect(b.color.copy(alpha = BAND_ALPHA), Offset(area.left, top), Size(area.right - area.left, bottom - top))
    if (b.label.isEmpty()) return
    val label = measurer.measure(b.label, style)
    val pad = 3.dp.toPx()
    // Only when the visible part of the zone is tall enough to hold its name.
    if (bottom - top >= label.size.height + 2 * pad) {
        drawText(label, topLeft = Offset(area.right - label.size.width - pad, top + pad))
    }
}

internal fun DrawScope.drawGrid(area: PlotArea, measurer: TextMeasurer, style: TextStyle, color: Color, laneHeight: Float) {
    for (i in 0..Y_TICKS) {
        val value = area.w.min + (area.w.max - area.w.min) * i / Y_TICKS
        val y = area.y(value)
        drawLine(color, Offset(area.left, y), Offset(area.right, y), strokeWidth = 1f)
        val label = measurer.measure(fmt(value), style)
        drawText(label, topLeft = Offset(area.left - label.size.width - 4.dp.toPx(), y - label.size.height / 2))
    }
    val labelTop = area.bottom + laneHeight + 4.dp.toPx()
    if (area.axis != null) drawGapAxis(area, area.axis, measurer, style, color, labelTop)
    else for (i in 0..X_TICKS) {
        val date = area.w.start.plusDays((area.w.days * i / X_TICKS).toLong())
        val label = measurer.measure(if (area.w.days > LONG_SPAN_DAYS) date.format(MONTH_YEAR) else date.short(), style)
        val left = (area.x(date) - label.size.width / 2).coerceIn(0f, size.width - label.size.width)
        drawText(label, topLeft = Offset(left, labelTop))
    }
    if (laneHeight > 0f) {
        val y = area.bottom + laneHeight
        drawLine(color, Offset(area.left, y), Offset(area.right, y), strokeWidth = 1f)
    }
    val today = LocalDate.now()
    val todayShown = area.axis?.position(today) != null || area.axis == null && !today.isBefore(area.w.start) && !today.isAfter(area.w.end)
    if (todayShown) {
        drawLine(color, Offset(area.x(today), area.top), Offset(area.x(today), area.bottom), strokeWidth = 2f)
    }
}

/**
 * Gap axis: a label at the start of each kept period (skipped when it would overlap the previous one), and a dashed
 * line with "…" at each break where months without data were left out.
 */
private fun DrawScope.drawGapAxis(area: PlotArea, axis: GapAxis, measurer: TextMeasurer, style: TextStyle, color: Color, labelTop: Float) {
    val long = axis.totalDays > LONG_SPAN_DAYS || axis.breaks.isNotEmpty()
    var lastRight = Float.NEGATIVE_INFINITY
    axis.segments.forEach { range ->
        val date = range.start
        val label = measurer.measure(if (long) date.format(MONTH_YEAR) else date.short(), style)
        val left = area.x(date).coerceIn(0f, size.width - label.size.width)
        if (left > lastRight + 4.dp.toPx()) {
            drawText(label, topLeft = Offset(left, labelTop))
            lastRight = left + label.size.width
        }
    }
    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
    val mark = measurer.measure(BREAK_MARK, style.copy(fontSize = style.fontSize * 1.4f))
    axis.breaks.forEach { middle ->
        val x = area.at(middle)
        drawLine(color, Offset(x, area.top), Offset(x, area.bottom), strokeWidth = 1.5f, pathEffect = dash)
        drawText(mark, topLeft = Offset(x - mark.size.width / 2, area.bottom - mark.size.height))
    }
}

internal fun DrawScope.drawBar(area: PlotArea, p: ChartPoint, color: Color) {
    val left = area.x(p.date)
    val width = area.x(p.date.plusDays(1)) - left
    val top = area.y(p.value)
    drawRect(color, Offset(left + width * BAR_GAP, top), Size(width * (1 - 2 * BAR_GAP), area.bottom - top))
}

internal fun DrawScope.drawSeries(area: PlotArea, s: ChartSeries) {
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

/**
 * A diamond, or a ring when [ring], centred on [center]; [halo] (the background color) outlines it so it stays visible
 * over a line. Without a halo (the legend) only the shape is drawn.
 */
internal fun DrawScope.drawMarkerShape(center: Offset, ring: Boolean, color: Color, halo: Color? = null) {
    if (ring) {
        halo?.let { drawCircle(it, 5.dp.toPx(), center) }
        drawCircle(color, 5.dp.toPx() - (if (halo == null) 1.dp.toPx() else 0f), center, style = Stroke(2.dp.toPx()))
    } else {
        val side = 7.dp.toPx()
        rotate(45f, center) {
            halo?.let { drawRect(it, center - Offset(side / 2 + 1.5f, side / 2 + 1.5f), Size(side + 3f, side + 3f)) }
            drawRect(color, center - Offset(side / 2, side / 2), Size(side, side))
        }
    }
}
