// Shared pieces for line charts over time: period filter (chips), visible window from the data, and the legend.
package com.clement.nutrimo.ui.common.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.ui.common.ChoiceChips
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor

/** Extra units (kg or cm) above and below the data so lines don't touch the edges. */
private const val Y_MARGIN = 1.0

enum class ChartPeriod(val label: String, val months: Long?) {
    M1("1 mois", 1), M3("3 mois", 3), M6("6 mois", 6), ALL("Tout", null);

    /** First visible day, or null for the whole history. */
    fun start(): LocalDate? = months?.let { LocalDate.now().minusMonths(it) }
}

@Composable
fun PeriodChips(period: ChartPeriod, onSelect: (ChartPeriod) -> Unit) {
    ChoiceChips(ChartPeriod.entries, period, { it.label }, onSelect)
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

/** One colored square + label per series. */
@Composable
fun ChartLegend(items: List<Pair<Color, String>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items.forEach { (color, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(12.dp).background(color, MaterialTheme.shapes.small))
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
