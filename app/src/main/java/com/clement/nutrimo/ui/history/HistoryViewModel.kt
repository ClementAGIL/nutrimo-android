// History state: one DayRecord per day (diary, sport, steps, water, weight), goal targets, browsed month and chart period.
package com.clement.nutrimo.ui.history

import com.clement.nutrimo.data.db.DEFAULT_WEIGHT_KG
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.db.DayTotal
import com.clement.nutrimo.data.health.toImported
import com.clement.nutrimo.data.repository.goalTargets
import com.clement.nutrimo.domain.calc.CalorieTarget
import com.clement.nutrimo.domain.calc.DailyGoal
import com.clement.nutrimo.domain.calc.DayRecord
import com.clement.nutrimo.domain.calc.GoalTargets
import com.clement.nutrimo.domain.calc.Streak
import com.clement.nutrimo.domain.calc.streak
import com.clement.nutrimo.ui.common.stateWhileSubscribed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Always loaded, so streaks and records cover a long enough past. */
private const val HISTORY_DAYS = 180L
val CHART_PERIODS = listOf(7, 30)

data class HistoryState(
    val today: LocalDate = LocalDate.now(),
    val records: Map<LocalDate, DayRecord> = emptyMap(),
    val totals: List<DayTotal> = emptyList(),
    val target: CalorieTarget? = null,
    val targets: GoalTargets = GoalTargets(),
    val month: YearMonth = YearMonth.now(),
    val selected: LocalDate = LocalDate.now(),
    val chartDays: Int = CHART_PERIODS.first(),
) {
    val streaks: Map<DailyGoal, Streak> get() = DailyGoal.entries.associateWith { streak(it, records, targets, today) }

    val chartFrom: LocalDate get() = today.minusDays(chartDays - 1L)

    /** Records of the chart period, oldest first. */
    val chartRecords: List<DayRecord> get() = records.values.filter { !it.date.isBefore(chartFrom) }.sortedBy { it.date }

    /** Diary totals of the chart period (for the macro averages). */
    val chartTotals: List<DayTotal> get() = totals.filter { !it.date.isBefore(chartFrom) }
}

private data class Browse(val month: YearMonth, val selected: LocalDate, val chartDays: Int)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val c: AppContainer) : ViewModel() {
    private val today = LocalDate.now()
    private val browse = MutableStateFlow(Browse(YearMonth.from(today), today, CHART_PERIODS.first()))
    private val health = MutableStateFlow(HealthHistory())

    /** Start of the loaded range: the recent past, extended when browsing older months. */
    private val from = browse.map { minOf(it.month.atDay(1), today.minusDays(HISTORY_DAYS)) }.distinctUntilChanged()

    private val local = from.flatMapLatest { f ->
        combine(c.diary.dailyTotals(f, today), c.activities.burned(f, today), c.diary.waterBetween(f, today), c.body.weightHistory) { totals, burned, water, weights ->
            LocalHistory(totals, burned, water, weights)
        }
    }

    val state = combine(local, health, c.body.calorieTarget, c.settings.settings, c.body.profile) { l, h, target, settings, profile ->
        val kg = l.weights.lastOrNull()?.kg ?: DEFAULT_WEIGHT_KG
        HistoryState(
            today = today,
            records = buildRecords(l, h, target?.target, settings.healthInBudget, kg),
            totals = l.totals,
            target = target,
            targets = settings.goalTargets(profile),
        )
    }.combine(browse) { s, b -> s.copy(month = b.month, selected = b.selected, chartDays = b.chartDays) }
        .stateWhileSubscribed(viewModelScope, HistoryState())

    init {
        // Health Connect is not observable: read once per loaded range.
        viewModelScope.launch { from.collectLatest { f -> health.value = readHealth(f) } }
    }

    fun showMonth(month: YearMonth) {
        if (month <= YearMonth.from(today)) browse.value = browse.value.copy(month = month)
    }

    fun select(date: LocalDate) {
        if (!date.isAfter(today)) browse.value = browse.value.copy(selected = date)
    }

    fun setChartDays(days: Int) {
        browse.value = browse.value.copy(chartDays = days)
    }

    private suspend fun readHealth(from: LocalDate): HealthHistory {
        val kg = c.body.latestWeight()?.kg ?: DEFAULT_WEIGHT_KG
        val sessions = c.health.sessionsBetween(from, today).groupBy({ it.first }, { it.second.toImported(kg) })
        return HealthHistory(c.health.dailySteps(from, today), sessions, c.health.dailyActiveKcal(from, today))
    }
}
