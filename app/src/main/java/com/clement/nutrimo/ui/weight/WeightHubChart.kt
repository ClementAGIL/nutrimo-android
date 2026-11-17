// Hub chart of the weight tab: weight, goal and trend lines over the BMI zones for the user's height;
// under the plot, a diamond on measurement days and a ring on photo days.
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.data.db.MeasurementEntity
import com.clement.nutrimo.data.db.PhotoEntity
import com.clement.nutrimo.domain.calc.Bmi
import com.clement.nutrimo.domain.calc.MIN_TREND_POINTS
import com.clement.nutrimo.domain.calc.TREND_WINDOW_DAYS
import com.clement.nutrimo.ui.common.InfoButton
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.chart.ChartBand
import com.clement.nutrimo.ui.common.chart.ChartLegend
import com.clement.nutrimo.ui.common.chart.ChartMarker
import com.clement.nutrimo.ui.common.chart.ChartPeriod
import com.clement.nutrimo.ui.common.chart.ChartPoint
import com.clement.nutrimo.ui.common.chart.ChartSeries
import com.clement.nutrimo.ui.common.chart.LineChart
import com.clement.nutrimo.ui.common.chart.PeriodChips
import com.clement.nutrimo.ui.common.chart.windowOf
import com.clement.nutrimo.ui.theme.NutrimoColors
import com.clement.nutrimo.util.full
import com.clement.nutrimo.util.kg
import java.time.LocalDate

private val LEGEND = listOf(NutrimoColors.weight to "Poids", NutrimoColors.objective to "Objectif", NutrimoColors.trend to "Tendance")
private val MEASUREMENT_COLOR = NutrimoColors.carbs
private val PHOTO_COLOR = NutrimoColors.water
private const val MEASUREMENT_LABEL = "Mensurations"
private const val PHOTO_LABEL = "Photos"
/** One per [Bmi.ZONES] entry: blue (underweight), green (normal), then yellow → dark red. */
private val BMI_COLORS = listOf(
    NutrimoColors.protein, NutrimoColors.weight, NutrimoColors.caution, NutrimoColors.warning, NutrimoColors.fat, Color(0xFFA0283C),
)

/** A tap selects the closest day with data, if it is within this share of the visible period (at least 2 days). */
private const val TAP_SHARE = 0.03

@Composable
fun WeightHubChart(state: WeightUiState, measurements: List<MeasurementEntity>, photos: List<PhotoEntity>, actions: WeightHubActions) {
    var period by rememberSaveable { mutableStateOf(ChartPeriod.ALL) }
    /** Dates picked by pinch / drag; null = the [period] chip decides. */
    var zoomed by rememberSaveable { mutableStateOf<Pair<LocalDate, LocalDate>?>(null) }
    var bubble by remember { mutableStateOf<Pair<HubDay, Float>?>(null) }
    val series = buildSeries(state)
    val markers = markersOf(measurements, photos)
    val bands = bmiBands(state.profile?.heightCm)
    val first = state.weights.first().date
    val preset = presetRange(period, first, state.weights.last().date)
    // Up to the end of the goal / trend lines and of the preset's future part, so the projection can be reached.
    val bounds = minOf(first, preset.first)..maxOf(series.maxOf { s -> s.points.maxOf { it.date } }, LocalDate.now(), preset.second)
    val (start, end) = zoomed ?: preset
    // Values framed on every line inside the view (weigh-ins, goal, trend), only where they cross it.
    val window = windowOf(series, start, end)
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Scrolls sideways on a narrow screen rather than pushing the ⓘ out of the card.
            Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                PeriodChips(period.takeIf { zoomed == null }) { period = it; zoomed = null; bubble = null }
            }
            InfoButton("Graphique du poids", helpText(bands.isNotEmpty()))
        }
        zoomed?.let { (start, end) ->
            Text("${start.full()} – ${end.full()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.fillMaxWidth().height(300.dp)) {
            LineChart(
                series, window, Modifier.fillMaxSize(), markers = markers, bands = bands, selected = bubble?.first?.date,
                onTap = { tapped, xOf ->
                    val tolerance = maxOf(2L, (window.days * TAP_SHARE).toLong())
                    bubble = nearestDay(tapped, tolerance, state.weights, measurements, photos)?.let { it to xOf(it.date) }
                },
                zoomBounds = bounds,
                onZoom = { start, end -> zoomed = start to end; bubble = null },
            )
            bubble?.let { (day, x) -> DayBubble(day, x, actions) { bubble = null } }
        }
        ChartLegend(LEGEND, listOf(ChartMarker(LocalDate.MIN, MEASUREMENT_COLOR, false, MEASUREMENT_LABEL), ChartMarker(LocalDate.MIN, PHOTO_COLOR, true, PHOTO_LABEL)))
        TrendText(state)
    }
}

/** Content of the ⓘ dialog of the chart. */
private fun helpText(bmiZones: Boolean) = buildString {
    append("• Touchez un jour pour voir sa pesée, ses mensurations et ses photos.\n")
    append("• Pincez pour zoomer, glissez sur le côté pour vous déplacer dans le temps ; les boutons de période reviennent à une vue prédéfinie, la dernière pesée aux 3/4 de la largeur.\n")
    append("• La tendance suit vos pesées des $TREND_WINDOW_DAYS derniers jours.\n")
    append("• Sous la courbe : un losange les jours de mensurations, un cercle les jours de photos.")
    if (bmiZones) append("\n• Les zones colorées sont les catégories d'IMC pour votre taille.")
}

/** "D'après la tendance, vous atteindrez votre objectif (82 kg) le 12 mars 2027." */
@Composable
private fun TrendText(state: WeightUiState) {
    val p = state.progress ?: return
    val goal = state.goal ?: return
    val text = when {
        state.goalReached -> "Objectif atteint, bravo !"
        p.trend == null -> "Encore quelques pesées ($MIN_TREND_POINTS minimum) pour estimer quand vous atteindrez votre objectif."
        p.estimatedEnd == null -> "Votre poids ne baisse pas en ce moment : pas encore d'estimation d'arrivée."
        else -> "D'après la tendance, vous atteindrez votre objectif (${kg(p.targetKg)}) le ${p.estimatedEnd.full()}" +
            if (p.estimatedEnd.isAfter(goal.endDate)) " (prévu le ${goal.endDate.full()})." else ", dans les temps."
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
}

/** Drawn in the lane under the plot: a row of diamonds (measurements), a row of rings (photos). */
private fun markersOf(measurements: List<MeasurementEntity>, photos: List<PhotoEntity>) = buildList {
    measurements.forEach { add(ChartMarker(it.date, MEASUREMENT_COLOR, ring = false)) }
    photos.map { it.date }.distinct().forEach { add(ChartMarker(it, PHOTO_COLOR, ring = true)) }
}

/** The WHO BMI zones as weight ranges for the user's height (none without a profile). */
private fun bmiBands(heightCm: Double?): List<ChartBand> {
    if (heightCm == null || heightCm <= 0) return emptyList()
    return Bmi.ZONES.zip(BMI_COLORS) { zone, color ->
        ChartBand(
            from = zone.min?.let { Bmi.weightAt(it, heightCm) } ?: Double.NEGATIVE_INFINITY,
            to = zone.max?.let { Bmi.weightAt(it, heightCm) } ?: Double.POSITIVE_INFINITY,
            color = color,
            label = zone.label,
        )
    }
}

/** Weight, objective line (start → goal date) and trend projected to its estimated end. */
private fun buildSeries(state: WeightUiState): List<ChartSeries> = buildList {
    val goal = state.goal
    if (goal != null) {
        add(ChartSeries(listOf(ChartPoint(goal.startDate, goal.startKg), ChartPoint(goal.endDate, goal.targetKg)), NutrimoColors.objective, dashed = true))
    }
    val trend = state.progress?.trend
    if (goal != null && trend != null) {
        val (from, end) = trendLineDates(trend, state.progress.estimatedEnd, goal.endDate, state.weights.last().date)
        add(ChartSeries(listOf(ChartPoint(from, trend.weightAt(from)), ChartPoint(end, trend.weightAt(end))), NutrimoColors.trend, dashed = true))
    }
    add(ChartSeries(state.weights.map { ChartPoint(it.date, it.kg) }, NutrimoColors.weight, dots = true))
}
