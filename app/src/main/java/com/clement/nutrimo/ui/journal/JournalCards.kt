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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.data.db.DiaryEntity
import com.clement.nutrimo.domain.model.Meal
import com.clement.nutrimo.ui.common.MacroBars
import com.clement.nutrimo.ui.common.ProgressRow
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.TextDialog
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.kcal
import com.clement.nutrimo.ui.theme.NutrimoColors

private const val WATER_STEP_ML = 250

@Composable
fun SummaryCard(state: JournalUiState, onClick: () -> Unit) {
    val target = state.budget
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KcalRing(state.total.kcal, target)
            Column(Modifier.weight(1f)) {
                Text("Mangé : ${kcal(state.total.kcal)}")
                target?.let {
                    Text("${if (state.burned > 0) "Budget" else "Objectif"} : ${kcal(it)}")
                    if (state.burned > 0) Text("dont ${kcal(state.burned)} d'activité", style = MaterialTheme.typography.bodySmall)
                    val left = it - state.total.kcal
                    Text(
                        if (left >= 0) "Reste : ${kcal(left)}" else "Dépassement : ${kcal(-left)}",
                        fontWeight = FontWeight.SemiBold,
                        color = if (left >= 0) NutrimoColors.weight else NutrimoColors.fat,
                    )
                }
            }
        }
        MacroBars(state.total, target?.let { state.macroSplit.grams(it) })
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

/** What a meal card can do. */
class MealCardActions(
    val onAdd: () -> Unit,
    val onEdit: (DiaryEntity) -> Unit,
    val onDelete: (DiaryEntity) -> Unit,
    val onCopy: () -> Unit,
    /** Saves the meal's entries as a reusable meal named by the user. */
    val onSave: (String) -> Unit,
)

@Composable
fun MealCard(meal: Meal, entries: List<DiaryEntity>, actions: MealCardActions) {
    var naming by remember { mutableStateOf(false) }
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(meal.label, style = MaterialTheme.typography.titleMedium)
                Text(kcal(entries.sumOf { it.nutrients.kcal }), style = MaterialTheme.typography.bodySmall)
            }
            if (entries.isEmpty()) IconButton(onClick = actions.onCopy) { Icon(Icons.Default.ContentCopy, "Copier depuis la veille") }
            else IconButton(onClick = { naming = true }) { Icon(Icons.Default.BookmarkAdd, "Enregistrer ce repas") }
            IconButton(onClick = actions.onAdd) { Icon(Icons.Default.Add, "Ajouter") }
        }
        // key(): the swipe state must follow its entry, not its position, once a row is deleted.
        entries.forEach { entry -> key(entry.id) { EntryRow(entry, actions.onEdit, actions.onDelete) } }
    }
    if (naming) TextDialog("Enregistrer ce repas", "Nom (ex : Petit-déj habituel)", meal.label, actions.onSave) { naming = false }
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
