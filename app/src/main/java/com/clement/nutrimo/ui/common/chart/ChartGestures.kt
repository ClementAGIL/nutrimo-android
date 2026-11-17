// Pinch-zoom and drag-pan of the dates of a LineChart.
package com.clement.nutrimo.ui.common.chart

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Two fingers: zoom around their centre (and pan). One finger: pans once it moves sideways more than up/down, so a
 * vertical swipe still scrolls the screen. Works in fractional days for the whole gesture so slow drags are not lost to
 * rounding; the chart stays within [bounds] and never shows less than [MIN_SPAN_DAYS].
 */
@Composable
internal fun Modifier.zoomPan(window: ChartWindow, bounds: ClosedRange<LocalDate>, onZoom: (LocalDate, LocalDate) -> Unit): Modifier {
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
