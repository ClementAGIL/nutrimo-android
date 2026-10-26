// Body measurements (waist, hips, chest, arm, thigh in cm), one entry per day, newest first.
package com.clement.nutrimo.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.data.db.MeasurementEntity
import com.clement.nutrimo.ui.common.DateField
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.appContainer
import com.clement.nutrimo.ui.common.day
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.toDecimal
import com.clement.nutrimo.ui.common.toInput
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Label and accessors of each measurement, so the list and the dialog share one definition. */
private data class Field(val label: String, val get: (MeasurementEntity) -> Double?, val set: (MeasurementEntity, Double?) -> MeasurementEntity)

private val FIELDS = listOf(
    Field("Tour de taille", { it.waist }, { m, v -> m.copy(waist = v) }),
    Field("Hanches", { it.hips }, { m, v -> m.copy(hips = v) }),
    Field("Poitrine", { it.chest }, { m, v -> m.copy(chest = v) }),
    Field("Bras", { it.arm }, { m, v -> m.copy(arm = v) }),
    Field("Cuisse", { it.thigh }, { m, v -> m.copy(thigh = v) }),
)

@Composable
fun MeasurementsScreen(onBack: () -> Unit) {
    val body = appContainer().body
    val scope = rememberCoroutineScope()
    val list by body.measurementHistory.collectAsStateWithLifecycle(initialValue = emptyList())
    var editing by remember { mutableStateOf<MeasurementEntity?>(null) }
    ScreenScaffold("Mensurations", onBack = onBack, floatingActionButton = {
        FloatingActionButton(onClick = { editing = MeasurementEntity(date = LocalDate.now()) }) { Icon(Icons.Default.Add, "Ajouter") }
    }) { padding ->
        if (list.isEmpty()) return@ScreenScaffold EmptyState("Aucune mensuration.", Modifier.padding(padding))
        LazyColumn(Modifier.padding(padding)) {
            items(list, key = { it.id }) { m ->
                ListItem(
                    headlineContent = { Text(m.date.day()) },
                    supportingContent = { Text(FIELDS.mapNotNull { f -> f.get(m)?.let { "${f.label} ${fmt(it)} cm" } }.joinToString(" · ")) },
                    trailingContent = { IconButton(onClick = { scope.launch { body.deleteMeasurement(m.id) } }) { Icon(Icons.Default.Delete, "Supprimer") } },
                    modifier = Modifier.clickable { editing = m },
                )
                HorizontalDivider()
            }
        }
    }
    editing?.let { m -> MeasurementDialog(m, { scope.launch { body.saveMeasurement(it) } }, { editing = null }) }
}

@Composable
private fun MeasurementDialog(initial: MeasurementEntity, onSave: (MeasurementEntity) -> Unit, onDismiss: () -> Unit) {
    var date by remember { mutableStateOf(initial.date) }
    val texts = remember { FIELDS.map { f -> mutableStateOf(f.get(initial)?.toInput().orEmpty()) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mensurations") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Date", date, { date = it }, Modifier.fillMaxWidth(), max = LocalDate.now())
                FIELDS.forEachIndexed { i, f -> NumberField(texts[i].value, { texts[i].value = it }, f.label, Modifier.fillMaxWidth(), "cm") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val filled = FIELDS.foldIndexed(initial.copy(date = date)) { i, m, f -> f.set(m, texts[i].value.toDecimal()) }
                onSave(filled)
                onDismiss()
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
