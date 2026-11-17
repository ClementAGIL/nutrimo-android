// Generic date/value chart drawn on a Canvas (no chart library): the public types and the composable. Drawing is in
// ChartDrawing, pinch-zoom / drag-pan in ChartGestures.
package com.clement.nutrimo.ui.common.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
internal val PLOT_LEFT = 36.dp
internal val PLOT_RIGHT = 8.dp
internal val PLOT_TOP = 8.dp
internal val PLOT_BOTTOM = 20.dp
/** Height of one marker row under the plot. */
internal val MARKER_ROW = 14.dp
/** Narrowest zoom. */
internal const val MIN_SPAN_DAYS = 7.0

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
    /** Collapses long stretches without data into short breaks (not combined with [onTap] / zoom). */
    axis: GapAxis? = null,
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
        val area = plotArea(size.width, size.height, rows.size, window, axis)
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
                drawMarkerShape(Offset(area.x(m.date), y), m.ring, m.color, halo)
            }
        }
    }
}
