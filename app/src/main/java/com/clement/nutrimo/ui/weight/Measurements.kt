// Body measurements (waist, hips, chest, arm, thigh in cm), one entry per day: list (newest first) and edit dialog.
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.data.db.MeasurementEntity
import com.clement.nutrimo.ui.common.DateField
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.ui.common.FAB_CLEARANCE
import com.clement.nutrimo.ui.common.FormDialog
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.day
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.toDecimal
import com.clement.nutrimo.ui.common.toInput
import com.clement.nutrimo.ui.theme.NutrimoColors
import java.time.LocalDate

/** Label, chart color and accessors of each measurement, so the list, the charts and the dialog share one definition. */
class MeasurementField(
    val label: String,
    val color: Color,
    val get: (MeasurementEntity) -> Double?,
    val set: (MeasurementEntity, Double?) -> MeasurementEntity,
)

val MEASUREMENT_FIELDS = listOf(
    MeasurementField("Tour de taille", NutrimoColors.carbs, { it.waist }, { m, v -> m.copy(waist = v) }),
    MeasurementField("Hanches", NutrimoColors.fat, { it.hips }, { m, v -> m.copy(hips = v) }),
    MeasurementField("Poitrine", NutrimoColors.protein, { it.chest }, { m, v -> m.copy(chest = v) }),
    MeasurementField("Bras", NutrimoColors.trend, { it.arm }, { m, v -> m.copy(arm = v) }),
    MeasurementField("Cuisse", NutrimoColors.weight, { it.thigh }, { m, v -> m.copy(thigh = v) }),
)

@Composable
fun MeasurementList(list: List<MeasurementEntity>, onEdit: (MeasurementEntity) -> Unit, onDelete: (MeasurementEntity) -> Unit) {
    if (list.isEmpty()) return EmptyState("Aucune mensuration. Ajoutez-en avec le bouton +.")
    LazyColumn(contentPadding = PaddingValues(bottom = FAB_CLEARANCE)) {
        items(list, key = { it.id }) { m ->
            ListItem(
                headlineContent = { Text(m.date.day()) },
                supportingContent = { Text(MEASUREMENT_FIELDS.mapNotNull { f -> f.get(m)?.let { "${f.label} ${fmt(it)} cm" } }.joinToString(" · ")) },
                trailingContent = { IconButton(onClick = { onDelete(m) }) { Icon(Icons.Default.Delete, "Supprimer") } },
                modifier = Modifier.clickable { onEdit(m) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun MeasurementDialog(initial: MeasurementEntity, onSave: (MeasurementEntity) -> Unit, onDismiss: () -> Unit) {
    var date by remember { mutableStateOf(initial.date) }
    val texts = remember { MEASUREMENT_FIELDS.map { f -> mutableStateOf(f.get(initial)?.toInput().orEmpty()) } }
    FormDialog("Mensurations", onDismiss, {
        onSave(MEASUREMENT_FIELDS.foldIndexed(initial.copy(date = date)) { i, m, f -> f.set(m, texts[i].value.toDecimal()) })
    }, confirmLabel = "Enregistrer") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField("Date", date, { date = it }, Modifier.fillMaxWidth(), max = LocalDate.now())
            MEASUREMENT_FIELDS.forEachIndexed { i, f -> NumberField(texts[i].value, { texts[i].value = it }, f.label, Modifier.fillMaxWidth(), "cm") }
        }
    }
}
