// Shared pieces for line charts over time: period filter (chips), visible window from the data, and the legend.
package com.clement.nutrimo.ui.common.chart

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.R
import com.clement.nutrimo.ui.common.ChoiceChips
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor

/** Extra units (kg or cm) above and below the data so lines don't touch the edges. */
private const val Y_MARGIN = 1.0

enum class ChartPeriod(@StringRes val label: Int, val months: Long?) {
    M3(R.string.chart_period_3m, 3), M6(R.string.chart_period_6m, 6), Y1(R.string.chart_period_1y, 12), ALL(R.string.chart_period_all, null);

    /** First visible day, counted back from [from], or null for the whole history. */
    fun start(from: LocalDate = LocalDate.now()): LocalDate? = months?.let { from.minusMonths(it) }
}

/** [period] null = none highlighted (e.g. a zoomed custom view). */
@Composable
fun PeriodChips(period: ChartPeriod?, onSelect: (ChartPeriod) -> Unit) {
    ChoiceChips(ChartPeriod.entries, period, { stringResource(it.label) }, onSelect)
}

/** Window over [period] with a 1-unit margin rounded to whole units (kg, cm); [series] must not be empty. */
fun windowOf(series: List<ChartSeries>, period: ChartPeriod): ChartWindow {
    val points = series.flatMap { it.points }
    val last = maxOf(points.maxOf { it.date }, LocalDate.now())
    val first = period.start() ?: points.minOf { it.date }
    val visible = points.filter { !it.date.isBefore(first) }.ifEmpty { points }
    return ChartWindow(
        start = first,
        end = if (last.isAfter(first)) last else first.plusDays(1),
        // Whole units keep the axis labels readable.
        min = floor(visible.minOf { it.value } - Y_MARGIN),
        max = ceil(visible.maxOf { it.value } + Y_MARGIN),
    )
}

/**
 * Window over [start]..[end] (a zoomed view), the values framed on the points inside it plus where each line crosses
 * the two edges, so a line entering or leaving the view is never cut off; [series] must not be empty.
 */
fun windowOf(series: List<ChartSeries>, start: LocalDate, end: LocalDate): ChartWindow {
    val visible = series.flatMap { s ->
        val points = s.points.sortedBy { it.date }
        points.filter { !it.date.isBefore(start) && !it.date.isAfter(end) } + listOfNotNull(valueAt(points, start), valueAt(points, end))
    }.ifEmpty { series.flatMap { it.points } }
    return ChartWindow(
        start = start,
        end = if (end.isAfter(start)) end else start.plusDays(1),
        min = floor(visible.minOf { it.value } - Y_MARGIN),
        max = ceil(visible.maxOf { it.value } + Y_MARGIN),
    )
}

/** The line's value on [date] between its two surrounding points, null outside the line; [points] sorted by date. */
private fun valueAt(points: List<ChartPoint>, date: LocalDate): ChartPoint? {
    val after = points.indexOfFirst { !it.date.isBefore(date) }
    if (after <= 0) return null
    val a = points[after - 1]
    val b = points[after]
    val t = ChronoUnit.DAYS.between(a.date, date).toDouble() / ChronoUnit.DAYS.between(a.date, b.date)
    return ChartPoint(date, a.value + (b.value - a.value) * t)
}

/** One colored square + label per series, then one symbol per marker kind ([ChartMarker.ring] = ring, else diamond). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartLegend(items: List<Pair<Color, String>>, markers: List<ChartMarker> = emptyList()) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (color, label) ->
            LegendItem(label) { Box(Modifier.size(12.dp).background(color, MaterialTheme.shapes.small)) }
        }
        markers.forEach { m -> LegendItem(m.label) { MarkerSymbol(m) } }
    }
}

@Composable
private fun LegendItem(label: String, symbol: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        symbol()
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

/** Same shape as on the chart. */
@Composable
private fun MarkerSymbol(m: ChartMarker) {
    Canvas(Modifier.size(12.dp)) { drawMarkerShape(center, m.ring, m.color) }
}
