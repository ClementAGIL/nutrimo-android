// Calendar tab: month grid with one dot per goal and day, the selected day's detail and the goal streaks.
package com.clement.nutrimo.ui.history

import com.clement.nutrimo.ui.common.HintText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.domain.calc.DailyGoal
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.day
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.kcal
import com.clement.nutrimo.ui.common.kg
import com.clement.nutrimo.ui.common.signedKg
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WEEK_DAYS = listOf("L", "M", "M", "J", "V", "S", "D")
private val MONTH_TITLE = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRANCE)
private val CELL_SHAPE = RoundedCornerShape(8.dp)
private const val FUTURE_ALPHA = 0.4f

@Composable
fun CalendarTab(state: HistoryState, onMonth: (YearMonth) -> Unit, onSelect: (LocalDate) -> Unit) {
    ScrollColumn(PaddingValues()) {
        SectionCard {
            MonthHeader(state.month, canGoNext = state.month < YearMonth.from(state.today)) { onMonth(state.month.plusMonths(it)) }
            MonthGrid(state, onSelect)
            GoalLegend()
        }
        DayDetail(state)
        Streaks(state)
    }
}

@Composable
private fun MonthHeader(month: YearMonth, canGoNext: Boolean, onMove: (Long) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onMove(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mois précédent") }
        Text(
            month.format(MONTH_TITLE).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onMove(1) }, enabled = canGoNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mois suivant") }
    }
}

/** Weeks start on Monday; leading cells before the 1st stay empty. */
@Composable
private fun MonthGrid(state: HistoryState, onSelect: (LocalDate) -> Unit) {
    val offset = state.month.atDay(1).dayOfWeek.value - 1
    val cells = List(offset) { null } + (1..state.month.lengthOfMonth()).map { state.month.atDay(it) }
    Row {
        WEEK_DAYS.forEach {
            Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    cells.chunked(WEEK_DAYS.size).forEach { week ->
        Row {
            repeat(WEEK_DAYS.size) { i ->
                Box(Modifier.weight(1f)) { week.getOrNull(i)?.let { DayCell(it, state, onSelect) } }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, state: HistoryState, onSelect: (LocalDate) -> Unit) {
    val future = date.isAfter(state.today)
    val record = state.records[date]
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .clip(CELL_SHAPE)
            .background(if (date == state.selected) colors.primaryContainer else Color.Transparent)
            .then(if (date == state.today) Modifier.border(1.dp, colors.primary, CELL_SHAPE) else Modifier)
            .clickable(enabled = !future) { onSelect(date) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "${date.dayOfMonth}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (future) colors.onSurface.copy(alpha = FUTURE_ALPHA) else colors.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            DailyGoal.entries.forEach { GoalDot(it, record?.achieved(it, state.targets), 6.dp) }
        }
    }
}

@Composable
private fun DayDetail(state: HistoryState) {
    val r = state.records[state.selected]
    val t = state.targets
    fun done(goal: DailyGoal) = r?.achieved(goal, t)
    SectionCard(if (state.selected == state.today) "Aujourd'hui" else state.selected.day()) {
        val eaten = r?.eatenKcal?.let { e -> r.budgetKcal?.let { "${fmt(e, 0)} / ${kcal(it)}" } ?: kcal(e) }
        GoalRow(DailyGoal.CALORIES, done(DailyGoal.CALORIES), eaten ?: "Rien noté", "Calories mangées")
        GoalRow(DailyGoal.SPORT, done(DailyGoal.SPORT), "${fmt(r?.sportKcal ?: 0.0, 0)} / ${kcal(t.sportKcal.toDouble())}", "Sport")
        GoalRow(DailyGoal.STEPS, done(DailyGoal.STEPS), "${fmt((r?.steps ?: 0).toDouble(), 0)} / ${fmt(t.steps.toDouble(), 0)}")
        GoalRow(DailyGoal.WATER, done(DailyGoal.WATER), "${r?.waterMl ?: 0} / ${t.waterMl} ml")
        val weight = r?.weightKg?.let { w -> kg(w) + r.previousWeightKg?.let { " (${signedKg(w - it)})" }.orEmpty() }
        GoalRow(DailyGoal.WEIGHT, done(DailyGoal.WEIGHT), weight ?: "Pas de pesée")
    }
}

@Composable
private fun Streaks(state: HistoryState) {
    SectionCard("Séries") {
        state.streaks.forEach { (goal, s) ->
            val unit = if (goal.skipsEmptyDays) "pesée" else "jour"
            GoalRow(goal, true, "${plural(s.current, unit)} · record ${s.best}")
        }
        HintText(
            "Jours consécutifs où l'objectif est atteint. Un jour sans donnée interrompt la série, sauf aujourd'hui " +
                "(la journée n'est pas finie). Le poids compte les pesées stables ou en baisse.",
        )
    }
}

private fun plural(n: Int, unit: String) = "$n $unit${if (n > 1) "s" else ""}"
