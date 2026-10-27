// Measurement charts: one line chart per measurement (waist, hips…) with its latest value and change over the period.
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
import com.clement.nutrimo.data.db.MeasurementEntity
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.ui.common.FAB_CLEARANCE
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.chart.ChartPeriod
import com.clement.nutrimo.ui.common.chart.ChartPoint
import com.clement.nutrimo.ui.common.chart.ChartSeries
import com.clement.nutrimo.ui.common.chart.LineChart
import com.clement.nutrimo.ui.common.chart.PeriodChips
import com.clement.nutrimo.ui.common.chart.windowOf
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.signed

@Composable
fun MeasurementCharts(list: List<MeasurementEntity>) {
    var period by rememberSaveable { mutableStateOf(ChartPeriod.ALL) }
    if (list.isEmpty()) return EmptyState("Ajoutez des mensurations pour voir leur évolution.")
    val from = period.start()
    val visible = list.filter { from == null || !it.date.isBefore(from) }.sortedBy { it.date }
    val charts = MEASUREMENT_FIELDS.mapNotNull { f ->
        visible.mapNotNull { m -> f.get(m)?.let { ChartPoint(m.date, it) } }.takeIf { it.isNotEmpty() }?.let { f to it }
    }
    ScrollColumn(PaddingValues(bottom = FAB_CLEARANCE)) {
        PeriodChips(period) { period = it }
        if (charts.isEmpty()) EmptyState("Aucune mensuration sur cette période.")
        charts.forEach { (field, points) -> MeasurementChart(field, points, period) }
    }
}

/** [points] are sorted by date and never empty. */
@Composable
private fun MeasurementChart(field: MeasurementField, points: List<ChartPoint>, period: ChartPeriod) {
    val series = listOf(ChartSeries(points, field.color, dots = true))
    SectionCard(field.label) {
        StatRow("Actuel", "${fmt(points.last().value)} cm")
        if (points.size > 1) StatRow("Évolution sur la période", signed(points.last().value - points.first().value, "cm"))
        LineChart(series, windowOf(series, period), Modifier.fillMaxWidth().height(180.dp))
    }
}
