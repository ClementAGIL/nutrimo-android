// Graph tab: actual weight, objective line (start → goal date) and trend projected to its estimated end.
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.chart.ChartLegend
import com.clement.nutrimo.ui.common.chart.ChartPeriod
import com.clement.nutrimo.ui.common.chart.ChartPoint
import com.clement.nutrimo.ui.common.chart.ChartSeries
import com.clement.nutrimo.ui.common.chart.LineChart
import com.clement.nutrimo.ui.common.chart.PeriodChips
import com.clement.nutrimo.ui.common.chart.windowOf
import com.clement.nutrimo.ui.common.full
import com.clement.nutrimo.ui.theme.NutrimoColors

private val LEGEND = listOf(NutrimoColors.weight to "Poids", NutrimoColors.objective to "Objectif", NutrimoColors.trend to "Tendance")

@Composable
fun WeightChartTab(state: WeightUiState) {
    var period by rememberSaveable { mutableStateOf(ChartPeriod.ALL) }
    if (state.weights.isEmpty()) return EmptyState("Ajoutez des pesées pour voir le graphique.")
    val series = buildSeries(state)
    ScrollColumn(PaddingValues()) {
        PeriodChips(period) { period = it }
        LineChart(series, windowOf(series, period), Modifier.fillMaxWidth().height(300.dp))
        ChartLegend(LEGEND)
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
