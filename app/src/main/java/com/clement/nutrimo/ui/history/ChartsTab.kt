// Charts tab: daily bars against the goal line for calories, sport, steps and water over 7 or 30 days, plus averages.
package com.clement.nutrimo.ui.history

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.R
import com.clement.nutrimo.domain.calc.DailyGoal
import com.clement.nutrimo.domain.calc.DayRecord
import com.clement.nutrimo.domain.calc.KCAL_PER_KG
import com.clement.nutrimo.ui.common.ChoiceChips
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.ui.common.HintText
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.chart.ChartPoint
import com.clement.nutrimo.ui.common.chart.ChartSeries
import com.clement.nutrimo.ui.common.chart.ChartWindow
import com.clement.nutrimo.ui.common.chart.LineChart
import com.clement.nutrimo.ui.theme.NutrimoColors
import com.clement.nutrimo.util.fmt
import com.clement.nutrimo.util.grams
import com.clement.nutrimo.util.kcal
import kotlin.math.ceil

/** One bar chart per goal: what to plot, the goal line and the Y axis rounding step. */
private class Metric(val title: String, val goal: DailyGoal, val yStep: Double, val value: (DayRecord) -> Double?)

private val METRICS = listOf(
    Metric("Calories mangées", DailyGoal.CALORIES, 250.0) { it.eatenKcal },
    Metric("Sport (kcal)", DailyGoal.SPORT, 100.0) { it.sportKcal.takeIf { k -> k > 0 } },
    Metric("Pas", DailyGoal.STEPS, 1000.0) { it.steps?.toDouble()?.takeIf { s -> s > 0 } },
    Metric("Eau (ml)", DailyGoal.WATER, 250.0) { it.waterMl?.toDouble()?.takeIf { w -> w > 0 } },
)

@Composable
fun ChartsTab(state: HistoryState, onDays: (Int) -> Unit) {
    ScrollColumn(PaddingValues()) {
        ChoiceChips(CHART_PERIODS, state.chartDays, { stringResource(R.string.charts_1_jours, it) }, onDays)
        if (state.chartRecords.isEmpty()) return@ScrollColumn EmptyState(stringResource(R.string.charts_aucune_donnee_sur_cette_periode))
        HintText(stringResource(R.string.charts_barres_valeur_jour_pointilles_objectif))
        METRICS.forEach { MetricChart(it, state) }
        HistoryStats(state)
    }
}

private fun HistoryState.goalValue(goal: DailyGoal): Double? = when (goal) {
    DailyGoal.CALORIES -> target?.target
    DailyGoal.SPORT -> targets.sportKcal.toDouble()
    DailyGoal.STEPS -> targets.steps.toDouble()
    DailyGoal.WATER -> targets.waterMl.toDouble()
    DailyGoal.WEIGHT, DailyGoal.JOURNAL -> null
}

@Composable
private fun MetricChart(metric: Metric, state: HistoryState) {
    val bars = state.chartRecords.mapNotNull { r -> metric.value(r)?.let { ChartPoint(r.date, it) } }
    SectionCard(metric.title) {
        if (bars.isEmpty()) return@SectionCard HintText(stringResource(R.string.charts_aucune_donnee))
        val end = state.today.plusDays(1)
        val goal = state.goalValue(metric.goal)
        val top = maxOf(bars.maxOf { it.value }, goal ?: 0.0) * 1.1
        val goalLine = goal?.let { listOf(ChartSeries(listOf(ChartPoint(state.chartFrom, it), ChartPoint(end, it)), NutrimoColors.objective, dashed = true)) }
        LineChart(
            series = goalLine.orEmpty(),
            window = ChartWindow(state.chartFrom, end, 0.0, ceil(top / metric.yStep) * metric.yStep),
            bars = bars,
            barColor = metric.goal.color,
            modifier = Modifier.fillMaxWidth().height(200.dp),
        )
    }
}

@Composable
private fun HistoryStats(state: HistoryState) {
    val records = state.chartRecords
    val eaten = records.filter { it.eatenKcal != null }
    fun average(values: List<Double>) = values.takeIf { it.isNotEmpty() }?.average()
    SectionCard(stringResource(R.string.charts_sur_1_jours, state.chartDays)) {
        StatRow(stringResource(R.string.charts_jours_notes), "${eaten.size} / ${state.chartDays}")
        average(eaten.mapNotNull { it.eatenKcal })?.let { StatRow(stringResource(R.string.charts_calories_moyennes), stringResource(R.string.common_1_jour, kcal(it))) }
        state.target?.let { StatRow(stringResource(R.string.common_objectif), stringResource(R.string.common_1_jour, kcal(it.target))) }
        StatRow(stringResource(R.string.charts_jours_dans_objectif), "${eaten.count { it.achieved(DailyGoal.CALORIES, state.targets) == true }} / ${eaten.size}")
        records.sumOf { it.sportKcal }.takeIf { it > 0 }?.let { StatRow(stringResource(R.string.common_sport), kcal(it)) }
        average(records.mapNotNull { it.steps?.takeIf { s -> s > 0 }?.toDouble() })?.let { StatRow(stringResource(R.string.charts_pas_moyens), stringResource(R.string.common_1_jour, fmt(it, 0))) }
        average(records.mapNotNull { it.waterMl?.takeIf { w -> w > 0 }?.toDouble() })?.let { StatRow(stringResource(R.string.charts_eau_moyenne), stringResource(R.string.charts_1_ml_jour, fmt(it, 0))) }
        state.target?.let { t ->
            // Energy not eaten compared with maintenance + sport, converted to fat mass (logged days only).
            val loss = eaten.sumOf { t.tdee + it.sportKcal - (it.eatenKcal ?: 0.0) } / KCAL_PER_KG
            StatRow(stringResource(R.string.charts_perte_estimee_calories), "${fmt(loss, 2)} kg")
        }
        val totals = state.chartTotals
        StatRow(stringResource(R.string.charts_proteines_moyennes), grams(average(totals.mapNotNull { it.protein })))
        StatRow(stringResource(R.string.charts_glucides_moyens), grams(average(totals.mapNotNull { it.carbs })))
        StatRow(stringResource(R.string.charts_lipides_moyens), grams(average(totals.mapNotNull { it.fat })))
        Text(
            stringResource(R.string.charts_perte_estimee_compare_vos_calories),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
