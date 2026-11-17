// Measurement charts: one line chart per measurement (waist, hips…) with its latest value and change over the period.
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.R
import com.clement.nutrimo.data.db.MeasurementEntity
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.ui.common.FAB_CLEARANCE
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.chart.ChartPeriod
import com.clement.nutrimo.ui.common.chart.ChartPoint
import com.clement.nutrimo.ui.common.chart.ChartSeries
import com.clement.nutrimo.ui.common.chart.GapAxis
import com.clement.nutrimo.ui.common.chart.LineChart
import com.clement.nutrimo.ui.common.chart.PeriodChips
import com.clement.nutrimo.ui.common.chart.windowOf
import com.clement.nutrimo.util.fmt
import com.clement.nutrimo.util.signed
import java.time.LocalDate

@Composable
fun MeasurementCharts(list: List<MeasurementEntity>) {
    var period by rememberSaveable { mutableStateOf(ChartPeriod.ALL) }
    if (list.isEmpty()) return EmptyState(stringResource(R.string.measurement_charts_ajoutez_mensurations_pour_voir_leur))
    val from = period.start()
    val visible = list.filter { from == null || !it.date.isBefore(from) }.sortedBy { it.date }
    val charts = MEASUREMENT_FIELDS.mapNotNull { f ->
        visible.mapNotNull { m -> f.get(m)?.let { ChartPoint(m.date, it) } }.takeIf { it.isNotEmpty() }?.let { f to it }
    }
    ScrollColumn(PaddingValues(bottom = FAB_CLEARANCE)) {
        PeriodChips(period) { period = it }
        if (charts.isEmpty()) EmptyState(stringResource(R.string.measurement_charts_aucune_mensuration_sur_cette_periode))
        charts.forEach { (field, points) -> MeasurementChart(field, points, period) }
    }
}

/** [points] are sorted by date and never empty. */
@Composable
private fun MeasurementChart(field: MeasurementField, points: List<ChartPoint>, period: ChartPeriod) {
    val series = listOf(ChartSeries(points, field.color, dots = true))
    SectionCard(stringResource(field.label)) {
        StatRow(stringResource(R.string.measurement_charts_actuel), "${fmt(points.last().value)} cm")
        if (points.size > 1) StatRow(stringResource(R.string.measurement_charts_evolution_sur_periode), signed(points.last().value - points.first().value, "cm"))
        // Months without measurements (more than 6 in a row) shrink to a short "…" break; today keeps its place.
        val axis = remember(points) { GapAxis(points.map { it.date } + LocalDate.now()) }
        LineChart(series, windowOf(series, period), Modifier.fillMaxWidth().height(180.dp), axis = axis)
    }
}
