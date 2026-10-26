// Journal card listing the day's physical activities, with a dialog to add one.
package com.clement.nutrimo.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.data.db.ActivityEntity
import com.clement.nutrimo.domain.calc.Activities
import com.clement.nutrimo.ui.common.Dropdown
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.kcal
import com.clement.nutrimo.ui.common.toDecimal

@Composable
fun ActivityCard(
    activities: List<ActivityEntity>,
    onAdd: (type: String, minutes: Int?, kcal: Double?) -> Unit,
    onDelete: (ActivityEntity) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Activité physique", style = MaterialTheme.typography.titleMedium)
                Text("${kcal(activities.sumOf { it.kcal })} dépensées · ajoutées au budget", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { adding = true }) { Icon(Icons.Default.Add, "Ajouter une activité") }
        }
        activities.forEach { a ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                val minutes = a.minutes?.let { " · $it min" }.orEmpty()
                Text("${Activities.byKey(a.type).label}$minutes", Modifier.weight(1f))
                Text(kcal(a.kcal))
                IconButton(onClick = { onDelete(a) }) { Icon(Icons.Default.Close, "Supprimer") }
            }
        }
    }
    if (adding) ActivityDialog(onAdd) { adding = false }
}

@Composable
private fun ActivityDialog(onAdd: (String, Int?, Double?) -> Unit, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(Activities.TYPES.first()) }
    var minutes by remember { mutableStateOf("30") }
    var manual by remember { mutableStateOf("") }
    val valid = if (type.met != null) minutes.toDecimal()?.let { it > 0 } == true else manual.toDecimal()?.let { it > 0 } == true
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Activité") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Dropdown("Type", type, Activities.TYPES, { it.label }, { type = it }, Modifier.fillMaxWidth())
                if (type.met != null) NumberField(minutes, { minutes = it }, "Durée", Modifier.fillMaxWidth(), "min")
                else NumberField(manual, { manual = it }, "Calories dépensées", Modifier.fillMaxWidth(), "kcal")
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onAdd(type.key, minutes.toDecimal()?.toInt()?.takeIf { type.met != null }, manual.toDecimal())
                onDismiss()
            }) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
