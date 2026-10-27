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
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.data.db.ActivityEntity
import com.clement.nutrimo.data.health.ImportedActivity
import com.clement.nutrimo.domain.calc.Activities
import com.clement.nutrimo.ui.common.CardHeader
import com.clement.nutrimo.ui.common.Dropdown
import com.clement.nutrimo.ui.common.FormDialog
import com.clement.nutrimo.ui.common.GoalSettingsButton
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.ProgressRow
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.kcal
import com.clement.nutrimo.ui.common.toDecimal
import com.clement.nutrimo.ui.theme.NutrimoColors

@Composable
fun ActivityCard(
    activities: List<ActivityEntity>,
    imported: List<ImportedActivity>,
    onAdd: (type: String, minutes: Int?, kcal: Double?) -> Unit,
    onDelete: (ActivityEntity) -> Unit,
    goalKcal: Int,
    onGoalSettings: () -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    val total = activities.sumOf { it.kcal } + imported.sumOf { it.kcal }
    SectionCard {
        CardHeader("Activité physique", "Activités notées : ajoutées au budget") {
            GoalSettingsButton(onGoalSettings)
            IconButton(onClick = { adding = true }) { Icon(Icons.Default.Add, "Ajouter une activité") }
        }
        ProgressRow("${fmt(total, 0)} / ${kcal(goalKcal.toDouble())}", "", total / goalKcal, NutrimoColors.sport)
        activities.forEach { a ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                val minutes = a.minutes?.let { " · $it min" }.orEmpty()
                Text("${Activities.byKey(a.type).label}$minutes", Modifier.weight(1f))
                Text(kcal(a.kcal))
                IconButton(onClick = { onDelete(a) }) { Icon(Icons.Default.Close, "Supprimer") }
            }
        }
        if (imported.isNotEmpty()) ImportedSessions(imported)
    }
    if (adding) ActivityDialog(onAdd) { adding = false }
}

/** Read-only workouts from Health Connect (they count in the budget only if enabled in Plus → Pas). */
@Composable
private fun ImportedSessions(sessions: List<ImportedActivity>) {
    Text("Depuis Health Connect", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    sessions.forEach { s ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${s.label} · ${s.minutes} min", Modifier.weight(1f))
            Text("${kcal(s.kcal)}${if (s.measured) "" else " (est.)"}")
        }
    }
}

@Composable
private fun ActivityDialog(onAdd: (String, Int?, Double?) -> Unit, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(Activities.TYPES.first()) }
    var minutes by remember { mutableStateOf("30") }
    var manual by remember { mutableStateOf("") }
    val valid = if (type.met != null) minutes.toDecimal()?.let { it > 0 } == true else manual.toDecimal()?.let { it > 0 } == true
    FormDialog(
        "Activité", onDismiss,
        { onAdd(type.key, minutes.toDecimal()?.toInt()?.takeIf { type.met != null }, manual.toDecimal()) },
        confirmLabel = "Ajouter", canConfirm = valid,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Dropdown("Type", type, Activities.TYPES, { it.label }, { type = it }, Modifier.fillMaxWidth())
            if (type.met != null) NumberField(minutes, { minutes = it }, "Durée", Modifier.fillMaxWidth(), "min")
            else NumberField(manual, { manual = it }, "Calories dépensées", Modifier.fillMaxWidth(), "kcal")
        }
    }
}
