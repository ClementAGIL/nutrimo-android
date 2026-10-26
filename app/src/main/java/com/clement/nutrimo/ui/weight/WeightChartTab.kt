// Graph tab: actual weight, objective line (start → goal date) and trend projected to its estimated end.
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.full
import com.clement.nutrimo.ui.theme.NutrimoColors
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor

/** Extra kg above and below the data so lines don't touch the edges. */
private const val Y_MARGIN_KG = 1.0

enum class ChartPeriod(val label: String, val months: Long?) {
    M1("1 mois", 1), M3("3 mois", 3), M6("6 mois", 6), ALL("Tout", null)
}

@Composable
fun WeightChartTab(state: WeightUiState) {
    var period by rememberSaveable { mutableStateOf(ChartPeriod.ALL) }
    if (state.weights.isEmpty()) return EmptyState("Ajoutez des pesées pour voir le graphique.")
    val series = buildSeries(state)
    val window = windowOf(series, period)
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChartPeriod.entries.forEach { FilterChip(selected = period == it, onClick = { period = it }, label = { Text(it.label) }) }
        }
        LineChart(series, window, Modifier.fillMaxWidth().height(300.dp))
        Legend()
        state.progress?.let { p ->
            SectionCard {
                state.goal?.let { StatRow("Date objectif", it.endDate.full()) }
                StatRow("Date estimée (tendance)", p.estimatedEnd?.full() ?: "–")
            }
        }
    }
}

private fun buildSeries(state: WeightUiState): List<ChartSeries> = buildList {
    val goal = state.goal
    if (goal != null) {
        add(ChartSeries(listOf(ChartPoint(goal.startDate, goal.startKg), ChartPoint(goal.endDate, goal.targetKg)), NutrimoColors.objective, dashed = true))
    }
    val trend = state.progress?.trend
    if (goal != null && trend != null) {
        val end = state.progress.estimatedEnd ?: state.weights.last().date
        add(ChartSeries(listOf(ChartPoint(goal.startDate, trend.weightAt(goal.startDate)), ChartPoint(end, trend.weightAt(end))), NutrimoColors.trend, dashed = true))
    }
    add(ChartSeries(state.weights.map { ChartPoint(it.date, it.kg) }, NutrimoColors.weight, dots = true))
}

private fun windowOf(series: List<ChartSeries>, period: ChartPeriod): ChartWindow {
    val points = series.flatMap { it.points }
    val last = maxOf(points.maxOf { it.date }, LocalDate.now())
    val first = period.months?.let { LocalDate.now().minusMonths(it) } ?: points.minOf { it.date }
    val visible = points.filter { !it.date.isBefore(first) }.ifEmpty { points }
    return ChartWindow(
        start = first,
        end = if (last.isAfter(first)) last else first.plusDays(1),
        // Whole kilograms keep the axis labels readable.
        min = floor(visible.minOf { it.value } - Y_MARGIN_KG),
        max = ceil(visible.maxOf { it.value } + Y_MARGIN_KG),
    )
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(NutrimoColors.weight, "Poids")
        LegendItem(NutrimoColors.objective, "Objectif")
        LegendItem(NutrimoColors.trend, "Tendance")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).background(color, MaterialTheme.shapes.small))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
