// Calorie history: daily kcal bars against the target over 7 or 30 days, with averages and the deficit achieved.
package com.clement.nutrimo.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.db.DayBurn
import com.clement.nutrimo.data.db.DayTotal
import com.clement.nutrimo.domain.calc.CalorieTarget
import com.clement.nutrimo.domain.calc.KCAL_PER_KG
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.grams
import com.clement.nutrimo.ui.common.kcal
import com.clement.nutrimo.ui.theme.NutrimoColors
import com.clement.nutrimo.ui.weight.ChartPoint
import com.clement.nutrimo.ui.weight.ChartSeries
import com.clement.nutrimo.ui.weight.ChartWindow
import com.clement.nutrimo.ui.weight.LineChart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import kotlin.math.ceil

private val PERIODS = listOf(7, 30)
/** Y axis rounded up to this step, with room above the highest bar. */
private const val Y_STEP_KCAL = 250.0

data class HistoryState(
    val days: Int = 7,
    val totals: List<DayTotal> = emptyList(),
    val target: CalorieTarget? = null,
    val burned: List<DayBurn> = emptyList(),
) {
    val from: LocalDate get() = LocalDate.now().minusDays(days - 1L)
    val average: Double? get() = totals.takeIf { it.isNotEmpty() }?.map { it.kcal }?.average()
    val onTarget: Int get() = target?.let { t -> totals.count { it.kcal <= t.target } } ?: 0

    /** Energy not eaten compared with maintenance, converted to fat mass. */
    val estimatedLossKg: Double? get() = target?.let { t -> totals.sumOf { t.tdee + burnedOn(it.date) - it.kcal } / KCAL_PER_KG }

    val totalBurned: Double get() = burned.sumOf { it.kcal }

    private fun burnedOn(date: LocalDate): Double = burned.firstOrNull { it.date == date }?.kcal ?: 0.0
}

@OptIn(ExperimentalCoroutinesApi::class)
class CalorieHistoryViewModel(c: AppContainer) : ViewModel() {
    val days = MutableStateFlow(PERIODS.first())
    val state = combine(
        days,
        days.flatMapLatest { d -> c.diary.dailyTotals(LocalDate.now().minusDays(d - 1L), LocalDate.now()) },
        c.body.calorieTarget,
        days.flatMapLatest { d -> c.activities.burned(LocalDate.now().minusDays(d - 1L), LocalDate.now()) },
    ) { d, totals, target, burned -> HistoryState(d, totals, target, burned) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())
}

@Composable
fun CalorieHistoryScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, _ -> CalorieHistoryViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    ScreenScaffold("Historique des calories", onBack = onBack) { padding ->
        ScrollColumn(padding) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PERIODS.forEach { d -> FilterChip(selected = state.days == d, onClick = { vm.days.value = d }, label = { Text("$d jours") }) }
            }
            if (state.totals.isEmpty()) return@ScrollColumn EmptyState("Aucun repas noté sur cette période.")
            HistoryChart(state)
            Text("Barres : calories mangées · ligne : objectif", style = MaterialTheme.typography.bodySmall)
            HistoryStats(state)
        }
    }
}

@Composable
private fun HistoryChart(state: HistoryState) {
    val end = LocalDate.now().plusDays(1)
    val target = state.target?.target
    val top = maxOf(state.totals.maxOf { it.kcal }, target ?: 0.0) * 1.1
    val window = ChartWindow(state.from, end, 0.0, ceil(top / Y_STEP_KCAL) * Y_STEP_KCAL)
    val targetLine = target?.let { listOf(ChartSeries(listOf(ChartPoint(state.from, it), ChartPoint(end, it)), NutrimoColors.fat, dashed = true)) }
    LineChart(
        series = targetLine.orEmpty(),
        window = window,
        bars = state.totals.map { ChartPoint(it.date, it.kcal) },
        barColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().height(260.dp),
    )
}

@Composable
private fun HistoryStats(state: HistoryState) {
    SectionCard("Sur ${state.days} jours") {
        StatRow("Jours notés", "${state.totals.size} / ${state.days}")
        state.average?.let { StatRow("Moyenne", "${kcal(it)} / jour") }
        state.target?.let { StatRow("Objectif", "${kcal(it.target)} / jour") }
        if (state.target != null) StatRow("Jours dans l'objectif", "${state.onTarget} / ${state.totals.size}")
        if (state.totalBurned > 0) StatRow("Activité physique", kcal(state.totalBurned))
        state.estimatedLossKg?.let { StatRow("Perte estimée (calories)", "${fmt(it, 2)} kg") }
        StatRow("Protéines moyennes", grams(state.totals.mapNotNull { it.protein }.takeIf { it.isNotEmpty() }?.average()))
        StatRow("Glucides moyens", grams(state.totals.mapNotNull { it.carbs }.takeIf { it.isNotEmpty() }?.average()))
        StatRow("Lipides moyens", grams(state.totals.mapNotNull { it.fat }.takeIf { it.isNotEmpty() }?.average()))
        Text(
            "La perte estimée compare vos calories notées à votre maintenance + activité (jours notés uniquement).",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
