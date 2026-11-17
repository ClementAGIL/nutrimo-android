// Generic date/value chart drawn on a Canvas (no chart library): grid, axis labels, tinted value zones, daily bars,
// lines, dots, a lane of day markers under the plot, taps, optional pinch-zoom / drag-pan of the dates.
package com.clement.nutrimo.ui.common.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.short
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private const val Y_TICKS = 5
private const val X_TICKS = 4
private const val MAX_DOTS = 60
/** Share of a day's width left empty on each side of a bar. */
private const val BAR_GAP = 0.15f

data class ChartPoint(val date: LocalDate, val value: Double)

data class ChartSeries(val points: List<ChartPoint>, val color: Color, val dashed: Boolean = false, val dots: Boolean = false)

/**
 * A symbol on a day of the chart (e.g. measurements, photos): a diamond, or a ring; [label] is for the legend.
 * Markers sit in a lane under the plot, one row per kind, so they never hide the curves.
 */
data class ChartMarker(val date: LocalDate, val color: Color, val ring: Boolean, val label: String = "")

/** A tinted horizontal zone from [from] to [to] (values, may be infinite) behind the curves, [label] at its right edge. */
data class ChartBand(val from: Double, val to: Double, val color: Color, val label: String = "")

/** Space around the plot, shared by drawing and tap handling. */
private val PLOT_LEFT = 36.dp
private val PLOT_RIGHT = 8.dp
private val PLOT_TOP = 8.dp
private val PLOT_BOTTOM = 20.dp
/** Height of one marker row under the plot. */
private val MARKER_ROW = 14.dp
private const val BAND_ALPHA = 0.16f
/** Narrowest zoom. */
private const val MIN_SPAN_DAYS = 7.0
/** Beyond this visible span the date labels show month and year instead of day and month. */
private const val LONG_SPAN_DAYS = 240f
private val MONTH_YEAR = DateTimeFormatter.ofPattern("MM/yyyy", Locale.FRANCE)

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
    markers: List<ChartMarker> = emptyList(),
    bands: List<ChartBand> = emptyList(),
    /** Draws a vertical line on this day. */
    selected: LocalDate? = null,
    /** Called with the tapped day and a function giving the x position (px) of any day, to place a bubble. */
    onTap: ((LocalDate, (LocalDate) -> Float) -> Unit)? = null,
    /** Enables pinch to zoom and horizontal drag to pan, within these dates; [onZoom] gets the new visible dates. */
    zoomBounds: ClosedRange<LocalDate>? = null,
    onZoom: ((LocalDate, LocalDate) -> Unit)? = null,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val halo = MaterialTheme.colorScheme.surface
    // Diamonds row first, then rings.
    val rows = markers.map { it.ring }.distinct().sorted()
    val tap = if (onTap == null) Modifier else Modifier.pointerInput(window, rows.size) {
        detectTapGestures { offset ->
            val area = plotArea(size.width.toFloat(), size.height.toFloat(), rows.size, window)
            onTap(area.dateAt(offset.x), area::x)
        }
    }
    val zoom = if (zoomBounds == null || onZoom == null) Modifier else Modifier.zoomPan(window, zoomBounds, onZoom)
    Canvas(modifier.then(zoom).then(tap)) {
        val area = plotArea(size.width, size.height, rows.size, window)
        clipRect(area.left, area.top, area.right, area.bottom) {
            bands.forEach { drawBand(area, it, measurer, labelStyle) }
        }
        drawGrid(area, measurer, labelStyle, gridColor, laneHeight = rows.size * MARKER_ROW.toPx())
        val laneBottom = area.bottom + rows.size * MARKER_ROW.toPx()
        selected?.let { drawLine(labelStyle.color, Offset(area.x(it), area.top), Offset(area.x(it), laneBottom), strokeWidth = 1.5.dp.toPx()) }
        clipRect(area.left, area.top, area.right, area.bottom) {
            bars.forEach { drawBar(area, it, barColor) }
            series.forEach { drawSeries(area, it) }
        }
        // Widened by a marker's size so symbols on the first and last day are not cut in half.
        clipRect(area.left - MARKER_ROW.toPx() / 2, area.bottom, area.right + MARKER_ROW.toPx() / 2, laneBottom) {
            markers.forEach { m ->
                val y = area.bottom + (rows.indexOf(m.ring) + 0.5f) * MARKER_ROW.toPx()
                drawMarker(Offset(area.x(m.date), y), m, halo)
            }
        }
    }
}

/**
 * Two fingers: zoom around their centre (and pan). One finger: pans once it moves sideways more than up/down, so a
 * vertical swipe still scrolls the screen. Works in fractional days for the whole gesture so slow drags are not lost to
 * rounding; the chart stays within [bounds] and never shows less than [MIN_SPAN_DAYS].
 */
@Composable
private fun Modifier.zoomPan(window: ChartWindow, bounds: ClosedRange<LocalDate>, onZoom: (LocalDate, LocalDate) -> Unit): Modifier {
    // Read through State: restarting pointerInput on every window change would cancel the gesture in progress.
    val current by rememberUpdatedState(window)
    val limits by rememberUpdatedState(bounds)
    val emit by rememberUpdatedState(onZoom)
    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val plotWidth = size.width - PLOT_LEFT.toPx() - PLOT_RIGHT.toPx()
            val min = limits.start.toEpochDay().toDouble()
            val max = limits.endInclusive.toEpochDay().toDouble()
            var start = current.start.toEpochDay().toDouble()
            var end = current.end.toEpochDay().toDouble()
            var active = false
            var dx = 0f
            var dy = 0f
            do {
                val event = awaitPointerEvent()
                val pressed = event.changes.count { it.pressed }
                val pan = event.calculatePan()
                var factor = 1f
                var anchor = 0.5f
                if (pressed >= 2) {
                    active = true
                    factor = event.calculateZoom()
                    anchor = ((event.calculateCentroid().x - PLOT_LEFT.toPx()) / plotWidth).coerceIn(0f, 1f)
                } else if (!active) {
                    dx += pan.x
                    dy += pan.y
                    active = abs(dx) > viewConfiguration.touchSlop && abs(dx) > abs(dy)
                }
                if (active && pressed > 0) {
                    val full = (max - min).coerceAtLeast(MIN_SPAN_DAYS)
                    val span = (end - start) / factor
                    val newSpan = span.coerceIn(minOf(MIN_SPAN_DAYS, full), full)
                    val pivot = start + (end - start) * anchor
                    var newStart = pivot - newSpan * anchor - pan.x / plotWidth * newSpan
                    newStart = newStart.coerceIn(min, (max - newSpan).coerceAtLeast(min))
                    start = newStart
                    end = newStart + newSpan
                    event.changes.forEach { it.consume() }
                    val s = LocalDate.ofEpochDay(start.roundToLong())
                    val e = LocalDate.ofEpochDay(end.roundToLong())
                    if (s != current.start || e != current.end) emit(s, e)
                }
            } while (event.changes.any { it.pressed })
        }
    }
}

private fun Density.plotArea(width: Float, height: Float, markerRows: Int, window: ChartWindow) = PlotArea(
    PLOT_LEFT.toPx(), PLOT_TOP.toPx(), width - PLOT_RIGHT.toPx(), height - PLOT_BOTTOM.toPx() - markerRows * MARKER_ROW.toPx(), window,
)

private class PlotArea(val left: Float, val top: Float, val right: Float, val bottom: Float, val w: ChartWindow) {
    fun x(date: LocalDate) = left + (right - left) * ChronoUnit.DAYS.between(w.start, date) / w.days
    fun y(value: Double) = bottom - (bottom - top) * ((value - w.min) / (w.max - w.min)).toFloat()
    fun dateAt(x: Float): LocalDate = w.start.plusDays(((x - left) / (right - left) * w.days).roundToLong().coerceIn(0, w.days.toLong()))
}

private fun DrawScope.drawBand(area: PlotArea, b: ChartBand, measurer: TextMeasurer, style: TextStyle) {
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

private fun DrawScope.drawGrid(area: PlotArea, measurer: TextMeasurer, style: TextStyle, color: Color, laneHeight: Float) {
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
        val label = measurer.measure(if (area.w.days > LONG_SPAN_DAYS) date.format(MONTH_YEAR) else date.short(), style)
        val left = (x - label.size.width / 2).coerceIn(0f, size.width - label.size.width)
        drawText(label, topLeft = Offset(left, area.bottom + laneHeight + 4.dp.toPx()))
    }
    if (laneHeight > 0f) {
        val y = area.bottom + laneHeight
        drawLine(color, Offset(area.left, y), Offset(area.right, y), strokeWidth = 1f)
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

private fun DrawScope.drawMarker(center: Offset, m: ChartMarker, halo: Color) {
    if (m.ring) {
        drawCircle(halo, 5.dp.toPx(), center)
        drawCircle(m.color, 5.dp.toPx(), center, style = Stroke(2.dp.toPx()))
    } else {
        val side = 7.dp.toPx()
        rotate(45f, center) {
            drawRect(halo, center - Offset(side / 2 + 1.5f, side / 2 + 1.5f), Size(side + 3f, side + 3f))
            drawRect(m.color, center - Offset(side / 2, side / 2), Size(side, side))
        }
    }
}
