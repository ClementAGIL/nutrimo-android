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
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.R
import com.clement.nutrimo.data.db.DiaryEntity
import com.clement.nutrimo.domain.model.Meal
import com.clement.nutrimo.ui.common.CardHeader
import com.clement.nutrimo.ui.common.DateField
import com.clement.nutrimo.ui.common.Dropdown
import com.clement.nutrimo.ui.common.FormDialog
import com.clement.nutrimo.ui.common.GoalSettingsButton
import com.clement.nutrimo.ui.common.HintText
import com.clement.nutrimo.ui.common.MacroBars
import com.clement.nutrimo.ui.common.ProgressRow
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.TextDialog
import com.clement.nutrimo.ui.theme.NutrimoColors
import com.clement.nutrimo.util.fmt
import com.clement.nutrimo.util.kcal
import java.time.LocalDate

private const val WATER_STEP_ML = 250

@Composable
fun SummaryCard(state: JournalUiState, onClick: () -> Unit, onGoalSettings: () -> Unit) {
    val target = state.budget
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KcalRing(state.total.kcal, target, zoneColor(state.kcalZone) ?: MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.journal_mange_1, kcal(state.total.kcal)))
                if (target != null) KcalBalance(state, target)
            }
            GoalSettingsButton(onGoalSettings)
        }
        MacroBars(state.total, target?.let { state.macroSplit.grams(it) })
        HintText(stringResource(R.string.journal_toucher_pour_detail_nutriments))
    }
}

/** Budget, maintenance and what is left; over the goal but under maintenance is yellow (still losing), not red. */
@Composable
private fun KcalBalance(state: JournalUiState, budget: Double) {
    Text(stringResource(if (state.burned > 0) R.string.journal_budget_line else R.string.journal_goal_line, kcal(budget)))
    if (state.burned > 0) Text(stringResource(R.string.journal_dont_1_activite_pas, kcal(state.burned)), style = MaterialTheme.typography.bodySmall)
    state.maintenance?.let { Text(stringResource(R.string.journal_maintenance_1, kcal(it)), style = MaterialTheme.typography.bodySmall) }
    val eaten = state.total.kcal
    val (text, style) = when (state.kcalZone) {
        // The yellow ring already says "still under maintenance": the text stays short and discreet.
        KcalZone.UNDER_MAINTENANCE -> stringResource(R.string.journal_au_dessus_objectif_1, kcal(eaten - budget)) to MaterialTheme.typography.bodySmall
        KcalZone.OVER_MAINTENANCE -> stringResource(R.string.journal_depassement_1, kcal(eaten - budget)) to LocalTextStyle.current
        else -> stringResource(R.string.journal_reste_1, kcal(budget - eaten)) to LocalTextStyle.current
    }
    Text(text, style = style, fontWeight = FontWeight.SemiBold, color = zoneColor(state.kcalZone) ?: NutrimoColors.weight)
}

private fun zoneColor(zone: KcalZone?) = when (zone) {
    KcalZone.UNDER_MAINTENANCE -> NutrimoColors.caution
    KcalZone.OVER_MAINTENANCE -> NutrimoColors.fat
    else -> null
}

@Composable
private fun KcalRing(eaten: Double, target: Double?, color: Color) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { if (target != null && target > 0) (eaten / target).toFloat().coerceIn(0f, 1f) else 0f },
            modifier = Modifier.size(84.dp),
            strokeWidth = 8.dp,
            color = color,
        )
        Text(fmt(eaten, 0), style = MaterialTheme.typography.titleLarge)
    }
}

/** What a meal card can do. */
class MealCardActions(
    val onAdd: () -> Unit,
    val onEdit: (DiaryEntity) -> Unit,
    val onDelete: (DiaryEntity) -> Unit,
    /** Copies the whole meal to a day and meal, where it is added to what is there. */
    val onCopy: (LocalDate, Meal) -> Unit,
    /** Moves the whole meal to a day and meal (entries logged on the wrong date). */
    val onMove: (LocalDate, Meal) -> Unit,
    /** Saves the meal's entries as a reusable meal named by the user. */
    val onSave: (String) -> Unit,
)

@Composable
fun MealCard(meal: Meal, entries: List<DiaryEntity>, date: LocalDate, today: LocalDate, actions: MealCardActions) {
    var naming by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    var copying by remember { mutableStateOf(false) }
    SectionCard {
        CardHeader(stringResource(meal.label), kcal(entries.sumOf { it.nutrients.kcal })) {
            // Only on a meal with foods: copies this meal to the chosen day and meal (to eat the same again).
            if (entries.isNotEmpty()) IconButton(onClick = { copying = true }) { Icon(Icons.Default.ContentCopy, stringResource(R.string.journal_copier_ce_repas)) }
            IconButton(onClick = actions.onAdd) { Icon(Icons.Default.Add, stringResource(R.string.common_ajouter)) }
            if (entries.isNotEmpty()) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.journal_plus_options)) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(
                            { Text(stringResource(R.string.journal_enregistrer_ce_repas)) }, { menu = false; naming = true },
                            leadingIcon = { Icon(Icons.Default.BookmarkAdd, null) },
                        )
                        DropdownMenuItem(
                            { Text(stringResource(R.string.journal_deplacer_autre_jour)) }, { menu = false; moving = true },
                            leadingIcon = { Icon(Icons.Default.EditCalendar, null) },
                        )
                    }
                }
            }
        }
        // key(): the swipe state must follow its entry, not its position, once a row is deleted.
        entries.forEach { entry -> key(entry.id) { EntryRow(entry, actions.onEdit, actions.onDelete) } }
    }
    if (copying) {
        MealTargetDialog(stringResource(R.string.journal_copier_ce_repas), stringResource(R.string.journal_copier), today, meal, today, actions.onCopy) { copying = false }
    }
    if (moving) {
        MealTargetDialog(stringResource(R.string.journal_deplacer_autre_jour), stringResource(R.string.journal_deplacer), date, meal, today, actions.onMove) { moving = false }
    }
    if (naming) TextDialog(stringResource(R.string.journal_enregistrer_ce_repas), stringResource(R.string.journal_nom_ex_petit_dej_habituel), stringResource(meal.label), actions.onSave) { naming = false }
}

/** Picks a day (up to [today]) and a meal, starting on [date] and [meal]; used to copy or move a meal. */
@Composable
private fun MealTargetDialog(
    title: String, confirmLabel: String, date: LocalDate, meal: Meal, today: LocalDate, onConfirm: (LocalDate, Meal) -> Unit, onDismiss: () -> Unit,
) {
    var day by remember { mutableStateOf(date) }
    var target by remember { mutableStateOf(meal) }
    FormDialog(title, onDismiss, { onConfirm(day, target) }, confirmLabel) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DateField(stringResource(R.string.reminders_jour), day, { day = it }, Modifier.fillMaxWidth(), max = today)
            Dropdown(stringResource(R.string.item_detail_repas), target, Meal.entries, { stringResource(it.label) }, { target = it }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun WaterCard(ml: Int, goalMl: Int, onAdd: (Int) -> Unit, onGoalSettings: () -> Unit) {
    SectionCard {
        CardHeader(stringResource(R.string.journal_eau)) { GoalSettingsButton(onGoalSettings) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                ProgressRow("$ml / $goalMl ml", stringResource(R.string.journal_1_verre, ml / WATER_STEP_ML), ml.toDouble() / goalMl, NutrimoColors.water)
            }
            IconButton(onClick = { onAdd(-WATER_STEP_ML) }, enabled = ml > 0) { Icon(Icons.Default.Remove, stringResource(R.string.journal_retirer_verre)) }
            IconButton(onClick = { onAdd(WATER_STEP_ML) }) { Icon(Icons.Default.Add, stringResource(R.string.journal_ajouter_verre)) }
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
                HintText(quantityText(entry))
            }
            Text(kcal(entry.nutrients.kcal))
        }
    }
}
