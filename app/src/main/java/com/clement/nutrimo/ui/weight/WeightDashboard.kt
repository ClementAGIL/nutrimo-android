// Dashboard tab: goal and time progress bars, and the key figures of the current goal.
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.domain.calc.Bmi
import com.clement.nutrimo.domain.calc.GoalProgress
import com.clement.nutrimo.domain.calc.MIN_TREND_POINTS
import com.clement.nutrimo.ui.common.ProgressRow
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.WarningText
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.full
import com.clement.nutrimo.ui.common.kcal
import com.clement.nutrimo.ui.common.kg
import com.clement.nutrimo.ui.common.percent
import com.clement.nutrimo.ui.theme.NutrimoColors

@Composable
fun WeightDashboard(state: WeightUiState, onNewGoal: () -> Unit, onGoals: () -> Unit, onAchieved: () -> Unit) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val progress = state.progress
        if (state.goal == null || progress == null) {
            SectionCard("Aucun objectif en cours") {
                Button(onClick = onNewGoal, modifier = Modifier.fillMaxWidth()) { Text("Définir un objectif") }
            }
        } else {
            if (state.goalReached) GoalReachedCard(onAchieved)
            ProgressCard(progress)
            FiguresCard(state, progress)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onNewGoal, modifier = Modifier.weight(1f)) { Text("Nouvel objectif") }
            OutlinedButton(onClick = onGoals, modifier = Modifier.weight(1f)) { Text("Mes objectifs") }
        }
    }
}

@Composable
private fun GoalReachedCard(onAchieved: () -> Unit) {
    SectionCard("🎉 Objectif atteint !") {
        Text("Bravo ! Vous pouvez clôturer cet objectif et en définir un nouveau.")
        Button(onClick = onAchieved) { Text("Marquer comme atteint") }
    }
}

@Composable
private fun ProgressCard(p: GoalProgress) {
    SectionCard("Progression") {
        ProgressRow("Poids", "${kg(p.lostKg.coerceAtLeast(0.0))} / ${kg(p.startKg - p.targetKg)} (${percent(p.weightProgress)})", p.weightProgress, NutrimoColors.weight)
        ProgressRow("Temps", "${p.daysElapsed} j écoulés · ${p.daysRemaining} j restants", p.timeProgress, NutrimoColors.objective)
        if (p.weightProgress + 0.05 < p.timeProgress) WarningText("Un peu en retard sur le plan : courage !")
    }
}

@Composable
private fun FiguresCard(state: WeightUiState, p: GoalProgress) {
    val goal = state.goal ?: return
    SectionCard("Chiffres clés") {
        StatRow("Poids de départ", "${kg(p.startKg)} (${goal.startDate.full()})")
        StatRow("Poids actuel", kg(p.currentKg))
        StatRow("Objectif", kg(p.targetKg))
        StatRow("Perdu", kg(p.lostKg))
        StatRow("Reste à perdre", kg(p.remainingKg))
        state.bmi?.let { StatRow("IMC", "${fmt(it)} · ${Bmi.category(it)}") }
        p.avgLossPerWeek?.let { StatRow("Perte moyenne", "${fmt(it, 2)} kg / semaine") }
        p.requiredPace?.let { StatRow("Rythme nécessaire", "${fmt(it, 2)} kg / semaine") }
        state.target?.let { StatRow("Objectif calorique", "${kcal(it.target)} / jour") }
        StatRow("Date prévue", goal.endDate.full())
        StatRow("Estimation (tendance)", estimate(p))
        Text(
            "L'estimation prolonge la droite de tendance de vos pesées depuis le début de l'objectif.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun estimate(p: GoalProgress): String = when {
    p.trend == null -> "au moins $MIN_TREND_POINTS pesées nécessaires"
    p.estimatedEnd == null -> "pas de tendance à la baisse"
    else -> p.estimatedEnd.full()
}
