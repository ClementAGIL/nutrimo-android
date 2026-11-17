// Bilan cards of the weight hub: goal and time progress bars, and the key figures of the current goal.
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.R
import com.clement.nutrimo.domain.calc.Bmi
import com.clement.nutrimo.domain.calc.GoalProgress
import com.clement.nutrimo.domain.calc.MIN_TREND_POINTS
import com.clement.nutrimo.domain.calc.TREND_WINDOW_DAYS
import com.clement.nutrimo.ui.common.HintText
import com.clement.nutrimo.ui.common.ProgressBar
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.WarningText
import com.clement.nutrimo.ui.common.WideButton
import com.clement.nutrimo.ui.theme.NutrimoColors
import com.clement.nutrimo.util.fmt
import com.clement.nutrimo.util.full
import com.clement.nutrimo.util.kcal
import com.clement.nutrimo.util.kg
import com.clement.nutrimo.util.percent
import java.time.LocalDate

@Composable
fun WeightDashboard(state: WeightUiState, onNewGoal: () -> Unit, onAchieved: () -> Unit) {
    val progress = state.progress
    if (state.goal == null || progress == null) {
        SectionCard(stringResource(R.string.weight_dashboard_aucun_objectif_en_cours)) {
            WideButton(stringResource(R.string.weight_dashboard_definir_objectif), onNewGoal)
        }
        return
    }
    if (state.goalReached) GoalReachedCard(onAchieved)
    ProgressCard(progress)
    FiguresCard(state, progress)
}

@Composable
private fun GoalReachedCard(onAchieved: () -> Unit) {
    SectionCard(stringResource(R.string.weight_dashboard_objectif_atteint)) {
        Text(stringResource(R.string.weight_dashboard_bravo_vous_pouvez_cloturer_cet))
        Button(onClick = onAchieved) { Text(stringResource(R.string.weight_dashboard_marquer_comme_atteint)) }
    }
}

@Composable
private fun ProgressCard(p: GoalProgress) {
    SectionCard("Progression") {
        PercentProgress("Poids", p.weightProgress, stringResource(R.string.weight_dashboard_1_perdus_sur_2, kg(p.lostKg.coerceAtLeast(0.0)), kg(p.startKg - p.targetKg)), NutrimoColors.weight)
        PercentProgress("Temps", p.timeProgress, stringResource(R.string.weight_dashboard_1_j_ecoules_2_j, p.daysElapsed, p.daysRemaining), NutrimoColors.objective)
        if (p.weightProgress + 0.05 < p.timeProgress) WarningText(stringResource(R.string.weight_dashboard_peu_en_retard_sur_plan))
    }
}

/** Title, then the bar with its percentage on the same line, then the detail. */
@Composable
private fun PercentProgress(title: String, progress: Double, detail: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressBar(progress, color, Modifier.weight(1f))
            Text(percent(progress, 1), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        HintText(detail)
    }
}

@Composable
private fun FiguresCard(state: WeightUiState, p: GoalProgress) {
    val goal = state.goal ?: return
    SectionCard(stringResource(R.string.weight_dashboard_chiffres_cles)) {
        StatRow(stringResource(R.string.weight_dashboard_poids_depart), "${kg(p.startKg)} (${goal.startDate.full()})")
        StatRow(stringResource(R.string.common_poids_actuel), kg(p.currentKg))
        StatRow("Objectif", kg(p.targetKg))
        StatRow("Perdu", kg(p.lostKg))
        StatRow(stringResource(R.string.weight_dashboard_reste_perdre), kg(p.remainingKg))
        state.bmi?.let { StatRow("IMC", "${fmt(it)} · ${Bmi.category(it)}") }
        state.profile?.let { profile -> NextBmiZone(p, profile.heightCm) }
        p.avgLossPerWeek?.let { StatRow(stringResource(R.string.weight_dashboard_perte_moyenne), stringResource(R.string.common_1_kg_semaine, fmt(it, 2))) }
        p.requiredPace?.let { StatRow(stringResource(R.string.weight_dashboard_rythme_necessaire), stringResource(R.string.common_1_kg_semaine, fmt(it, 2))) }
        state.target?.let { StatRow(stringResource(R.string.weight_dashboard_objectif_calorique), stringResource(R.string.common_1_jour, kcal(it.target))) }
        StatRow(stringResource(R.string.weight_dashboard_date_prevue), goal.endDate.full())
        StatRow(stringResource(R.string.weight_dashboard_estimation_tendance), estimate(p))
        HintText(
            stringResource(R.string.weight_dashboard_estimation_prolonge_droite_tendance_vos, TREND_WINDOW_DAYS),
        )
    }
}

/** "Obésité sévère : sous 141,1 kg, encore 5,9 kg" and the date the trend gets there. */
@Composable
private fun NextBmiZone(p: GoalProgress, heightCm: Double) {
    val (zone, threshold) = Bmi.nextLower(p.currentKg, heightCm) ?: return
    StatRow(stringResource(R.string.weight_dashboard_prochaine_categorie), zone.label)
    StatRow("Seuil", stringResource(R.string.weight_dashboard_sous_1_encore_2, kg(threshold), kg(p.currentKg - threshold)))
    StatRow(stringResource(R.string.weight_dashboard_atteinte_estimee_tendance), estimate(p, p.trend?.dateReaching(threshold)))
}

private fun estimate(p: GoalProgress, date: LocalDate? = p.estimatedEnd): String = when {
    p.trend == null -> "$MIN_TREND_POINTS pesées minimum"
    date == null -> "pas de tendance à la baisse"
    else -> date.full()
}
