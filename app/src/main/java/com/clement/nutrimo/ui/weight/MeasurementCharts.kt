// Measurement charts: one line chart per measurement (waist, hips…) with its latest value and change over the period.
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
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
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.signed
import java.time.LocalDate

@Composable
fun MeasurementCharts(list: List<MeasurementEntity>) {
    var period by rememberSaveable { mutableStateOf(ChartPeriod.ALL) }
    if (list.isEmpty()) return EmptyState("Ajoutez des mensurations pour voir leur évolution.")
    val from = period.months?.let { LocalDate.now().minusMonths(it) }
    val visible = list.filter { from == null || !it.date.isBefore(from) }.sortedBy { it.date }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = FAB_CLEARANCE),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChartPeriod.entries.forEach { FilterChip(selected = period == it, onClick = { period = it }, label = { Text(it.label) }) }
        }
        val charts = MEASUREMENT_FIELDS.mapNotNull { f -> visible.mapNotNull { m -> f.get(m)?.let { ChartPoint(m.date, it) } }.takeIf { it.isNotEmpty() }?.let { f to it } }
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
