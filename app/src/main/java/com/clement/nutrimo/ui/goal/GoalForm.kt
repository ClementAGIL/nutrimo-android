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
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.data.db.ProfileEntity
import com.clement.nutrimo.domain.calc.Energy
import com.clement.nutrimo.domain.model.GoalMode
import com.clement.nutrimo.ui.common.ChoiceChips
import com.clement.nutrimo.ui.common.DateField
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.WarningText
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.full
import com.clement.nutrimo.ui.common.kcal
import java.time.LocalDate

@Composable
fun GoalForm(draft: GoalDraft, onChange: (GoalDraft) -> Unit, profile: ProfileEntity?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(draft.startKg, { onChange(draft.copy(startKg = it)) }, "Poids actuel", Modifier.weight(1f), "kg")
            NumberField(draft.targetKg, { onChange(draft.copy(targetKg = it)) }, "Objectif", Modifier.weight(1f), "kg")
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            GoalMode.entries.forEachIndexed { i, mode ->
                SegmentedButton(
                    selected = draft.mode == mode,
                    onClick = { onChange(draft.copy(mode = mode)) },
                    shape = SegmentedButtonDefaults.itemShape(i, GoalMode.entries.size),
                ) { Text(if (mode == GoalMode.PACE) "Par rythme" else "Par date") }
            }
        }
        if (draft.mode == GoalMode.PACE) PaceChooser(draft, onChange)
        else DateField("Date objectif", draft.endDate, { onChange(draft.copy(endDate = it)) }, Modifier.fillMaxWidth(), min = LocalDate.now().plusDays(7))
        draft.error?.let { if (draft.startKg.isNotEmpty() && draft.targetKg.isNotEmpty()) WarningText(it) }
        GoalPreview(draft, profile)
    }
}

@Composable
private fun PaceChooser(draft: GoalDraft, onChange: (GoalDraft) -> Unit) {
    ChoiceChips(PACE_PRESETS + CUSTOM_PACE, draft.preset, { if (it == CUSTOM_PACE) "Autre" else "${fmt(it)} kg" }) {
        onChange(draft.copy(preset = it))
    }
    if (draft.preset == CUSTOM_PACE) {
        NumberField(draft.customPace, { onChange(draft.copy(customPace = it)) }, "Rythme", Modifier.fillMaxWidth(), "kg / semaine")
    }
}

/** What the goal means per day: maintenance, deficit, target, end date, and safety warnings. */
@Composable
private fun GoalPreview(draft: GoalDraft, profile: ProfileEntity?) {
    val start = draft.start ?: return
    val pace = draft.pace?.takeIf { draft.error == null } ?: return
    SectionCard("Ce que ça représente") {
        StatRow("Rythme", "${fmt(pace, 2)} kg / semaine")
        draft.resolvedEnd?.let { StatRow("Date de fin prévue", it.full()) }
        if (profile == null) return@SectionCard
        val t = Energy.target(profile.sex, start, profile.heightCm, Energy.age(profile.birthDate), profile.activity, pace)
        StatRow("Maintenance estimée", kcal(t.tdee))
        StatRow("Déficit", "${kcal(t.deficit)} / jour")
        Text("Objectif : ${kcal(t.target)} / jour", style = MaterialTheme.typography.titleMedium)
        if (t.floorApplied) {
            WarningText("Minimum de ${kcal(t.target)} appliqué : le rythme réel sera d'environ ${fmt(t.effectivePace, 2)} kg / semaine.")
        }
        if (t.tooFast) WarningText("Plus de 1 % du poids par semaine : rythme déconseillé sans suivi médical.")
    }
}
