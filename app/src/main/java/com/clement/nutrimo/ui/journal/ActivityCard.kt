// Journal card: progress to the daily sport goal, the day's activities (manual + Health Connect) and an add dialog.
package com.clement.nutrimo.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.R
import com.clement.nutrimo.data.db.ActivityEntity
import com.clement.nutrimo.data.health.ImportedActivity
import com.clement.nutrimo.domain.calc.Activities
import com.clement.nutrimo.domain.calc.ActivityType
import com.clement.nutrimo.ui.common.CardHeader
import com.clement.nutrimo.ui.common.Dropdown
import com.clement.nutrimo.ui.common.FormDialog
import com.clement.nutrimo.ui.common.GoalSettingsButton
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.ProgressRow
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.asString
import com.clement.nutrimo.ui.theme.NutrimoColors
import com.clement.nutrimo.util.fmt
import com.clement.nutrimo.util.kcal
import com.clement.nutrimo.util.toDecimal

@Composable
fun ActivityCard(
    activities: List<ActivityEntity>,
    /** Choices of the add dialog, most logged first (the first is preselected). */
    types: List<ActivityType>,
    imported: List<ImportedActivity>,
    onAdd: (type: String, minutes: Int?, kcal: Double?) -> Unit,
    onDelete: (ActivityEntity) -> Unit,
    goalKcal: Int,
    onGoalSettings: () -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    val total = activities.sumOf { it.kcal } + imported.sumOf { it.kcal }
    SectionCard {
        CardHeader(stringResource(R.string.common_activite_physique), stringResource(R.string.activity_activites_notees_ajoutees_au_budget)) {
            GoalSettingsButton(onGoalSettings)
            IconButton(onClick = { adding = true }) { Icon(Icons.Default.Add, stringResource(R.string.activity_ajouter_activite)) }
        }
        ProgressRow("${fmt(total, 0)} / ${kcal(goalKcal.toDouble())}", "", total / goalKcal, NutrimoColors.sport)
        activities.forEach { a ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                val minutes = a.minutes?.let { stringResource(R.string.activity_1_min, it) }.orEmpty()
                Text(stringResource(Activities.byKey(a.type).label) + minutes, Modifier.weight(1f))
                Text(kcal(a.kcal))
                IconButton(onClick = { onDelete(a) }) { Icon(Icons.Default.Close, stringResource(R.string.common_supprimer)) }
            }
        }
        if (imported.isNotEmpty()) ImportedSessions(imported)
    }
    if (adding) ActivityDialog(types, onAdd) { adding = false }
}

/** Read-only workouts from Health Connect (they count in the budget only if enabled in Plus → Pas). */
@Composable
private fun ImportedSessions(sessions: List<ImportedActivity>) {
    Text(stringResource(R.string.activity_depuis_health_connect), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    sessions.forEach { s ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.activity_1_2_min, s.label.asString(), s.minutes), Modifier.weight(1f))
            Text(if (s.measured) kcal(s.kcal) else stringResource(R.string.activity_estimated, kcal(s.kcal)))
        }
    }
}

@Composable
private fun ActivityDialog(types: List<ActivityType>, onAdd: (String, Int?, Double?) -> Unit, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(types.first()) }
    var minutes by remember { mutableStateOf("30") }
    var manual by remember { mutableStateOf("") }
    // Minutes are whole ("0,5" would round to 0 and log nothing).
    val valid = if (type.met != null) minutes.toDecimal()?.let { it >= 1 } == true else manual.toDecimal()?.let { it > 0 } == true
    FormDialog(
        stringResource(R.string.activity_activite), onDismiss,
        { onAdd(type.key, minutes.toDecimal()?.toInt()?.takeIf { type.met != null }, manual.toDecimal()) },
        confirmLabel = stringResource(R.string.common_ajouter), canConfirm = valid,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Dropdown(stringResource(R.string.activity_type), type, types, { stringResource(it.label) }, { type = it }, Modifier.fillMaxWidth())
            if (type.met != null) NumberField(minutes, { minutes = it }, stringResource(R.string.activity_duree), Modifier.fillMaxWidth(), "min")
            else NumberField(manual, { manual = it }, stringResource(R.string.activity_calories_depensees), Modifier.fillMaxWidth(), "kcal")
        }
    }
}
