// Goal form (weights, pace preset or end date) with a live calorie preview; used by onboarding and GoalEdit.
package com.clement.nutrimo.ui.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.R
import com.clement.nutrimo.data.db.ProfileEntity
import com.clement.nutrimo.domain.calc.Energy
import com.clement.nutrimo.domain.model.GoalMode
import com.clement.nutrimo.ui.common.ChoiceChips
import com.clement.nutrimo.ui.common.DateField
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.WarningText
import com.clement.nutrimo.ui.common.asString
import com.clement.nutrimo.util.fmt
import com.clement.nutrimo.util.full
import com.clement.nutrimo.util.kcal
import java.time.LocalDate

@Composable
fun GoalForm(draft: GoalDraft, onChange: (GoalDraft) -> Unit, profile: ProfileEntity?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(draft.startKg, { onChange(draft.copy(startKg = it)) }, stringResource(R.string.common_poids_actuel), Modifier.weight(1f), "kg")
            NumberField(draft.targetKg, { onChange(draft.copy(targetKg = it)) }, stringResource(R.string.common_objectif), Modifier.weight(1f), "kg")
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            GoalMode.entries.forEachIndexed { i, mode ->
                SegmentedButton(
                    selected = draft.mode == mode,
                    onClick = { onChange(draft.copy(mode = mode)) },
                    shape = SegmentedButtonDefaults.itemShape(i, GoalMode.entries.size),
                ) { Text(if (mode == GoalMode.PACE) stringResource(R.string.goal_form_par_rythme) else stringResource(R.string.goal_form_par_date)) }
            }
        }
        if (draft.mode == GoalMode.PACE) PaceChooser(draft, onChange)
        else DateField(stringResource(R.string.goal_form_date_objectif), draft.endDate, { onChange(draft.copy(endDate = it)) }, Modifier.fillMaxWidth(), min = LocalDate.now().plusDays(7))
        draft.error?.let { if (draft.startKg.isNotEmpty() && draft.targetKg.isNotEmpty()) WarningText(it.asString()) }
        GoalPreview(draft, profile)
    }
}

@Composable
private fun PaceChooser(draft: GoalDraft, onChange: (GoalDraft) -> Unit) {
    ChoiceChips(PACE_PRESETS + CUSTOM_PACE, draft.preset, { if (it == CUSTOM_PACE) stringResource(R.string.goal_form_autre) else "${fmt(it)} kg" }) {
        onChange(draft.copy(preset = it))
    }
    if (draft.preset == CUSTOM_PACE) {
        NumberField(draft.customPace, { onChange(draft.copy(customPace = it)) }, stringResource(R.string.goal_form_rythme), Modifier.fillMaxWidth(), stringResource(R.string.goal_form_kg_semaine))
    }
}

/** What the goal means per day: maintenance, deficit, target, end date, and safety warnings. */
@Composable
private fun GoalPreview(draft: GoalDraft, profile: ProfileEntity?) {
    val start = draft.start ?: return
    val pace = draft.pace?.takeIf { draft.error == null } ?: return
    SectionCard(stringResource(R.string.goal_form_ce_que_ca_represente)) {
        StatRow(stringResource(R.string.goal_form_rythme), stringResource(R.string.common_1_kg_semaine, fmt(pace, 2)))
        draft.resolvedEnd?.let { StatRow(stringResource(R.string.goal_form_date_fin_prevue), it.full()) }
        if (profile == null) return@SectionCard
        val t = Energy.target(profile.sex, start, profile.heightCm, Energy.age(profile.birthDate), profile.activity, pace)
        StatRow(stringResource(R.string.goal_form_maintenance_estimee), kcal(t.tdee))
        StatRow(stringResource(R.string.goal_form_deficit), stringResource(R.string.common_1_jour, kcal(t.deficit)))
        Text(stringResource(R.string.goal_form_objectif_1_jour, kcal(t.target)), style = MaterialTheme.typography.titleMedium)
        if (t.floorApplied) {
            WarningText(stringResource(R.string.goal_form_minimum_1_applique_rythme_reel, kcal(t.target), fmt(t.effectivePace, 2)))
        }
        if (t.tooFast) WarningText(stringResource(R.string.goal_form_plus_1_poids_par_semaine))
    }
}
