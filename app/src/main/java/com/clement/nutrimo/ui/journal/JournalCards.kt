// Cards of the journal: daily summary (kcal ring + macros), one card per meal, water.
package com.clement.nutrimo.ui.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.data.db.DiaryEntity
import com.clement.nutrimo.domain.model.Meal
import com.clement.nutrimo.ui.common.MacroBars
import com.clement.nutrimo.ui.common.MacroTargets
import com.clement.nutrimo.ui.common.ProgressRow
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.kcal
import com.clement.nutrimo.ui.theme.NutrimoColors

private const val WATER_STEP_ML = 250

@Composable
fun SummaryCard(state: JournalUiState, onClick: () -> Unit) {
    val target = state.target?.target
    SectionCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KcalRing(state.total.kcal, target)
            Column(Modifier.weight(1f)) {
                Text("Mangé : ${kcal(state.total.kcal)}")
                target?.let {
                    Text("Objectif : ${kcal(it)}")
                    val left = it - state.total.kcal
                    Text(
                        if (left >= 0) "Reste : ${kcal(left)}" else "Dépassement : ${kcal(-left)}",
                        fontWeight = FontWeight.SemiBold,
                        color = if (left >= 0) NutrimoColors.weight else NutrimoColors.fat,
                    )
                }
            }
        }
        MacroBars(state.total, target?.let { MacroTargets.forKcal(it) })
        Text("Toucher pour le détail des nutriments", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun KcalRing(eaten: Double, target: Double?) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { if (target != null && target > 0) (eaten / target).toFloat().coerceIn(0f, 1f) else 0f },
            modifier = Modifier.size(84.dp),
            strokeWidth = 8.dp,
            color = if (target != null && eaten > target) NutrimoColors.fat else MaterialTheme.colorScheme.primary,
        )
        Text(fmt(eaten, 0), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun MealCard(
    meal: Meal,
    entries: List<DiaryEntity>,
    onAdd: () -> Unit,
    onEdit: (DiaryEntity) -> Unit,
    onDelete: (DiaryEntity) -> Unit,
    onCopy: () -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(meal.label, style = MaterialTheme.typography.titleMedium)
                Text(kcal(entries.sumOf { it.nutrients.kcal }), style = MaterialTheme.typography.bodySmall)
            }
            if (entries.isEmpty()) IconButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, "Copier depuis la veille") }
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Ajouter") }
        }
        entries.forEach { EntryRow(it, onEdit, onDelete) }
    }
}

@Composable
fun WaterCard(ml: Int, goalMl: Int, onAdd: (Int) -> Unit) {
    SectionCard("Eau") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                ProgressRow("$ml / $goalMl ml", "${ml / WATER_STEP_ML} verre(s)", ml.toDouble() / goalMl, NutrimoColors.protein)
            }
            IconButton(onClick = { onAdd(-WATER_STEP_ML) }, enabled = ml > 0) { Icon(Icons.Default.Remove, "Retirer un verre") }
            IconButton(onClick = { onAdd(WATER_STEP_ML) }) { Icon(Icons.Default.Add, "Ajouter un verre") }
        }
    }
}

@Composable
private fun EntryRow(entry: DiaryEntity, onEdit: (DiaryEntity) -> Unit, onDelete: (DiaryEntity) -> Unit) {
    SwipeToDelete(onDelete = { onDelete(entry) }) {
        Row(
            Modifier.fillMaxWidth().clickable { onEdit(entry) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.label, maxLines = 1)
                Text(quantityText(entry), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(kcal(entry.nutrients.kcal))
        }
    }
}
