// Journal tab: day switcher, daily summary, the 4 meals, steps, activities, water and "Ma journée".
package com.clement.nutrimo.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.R
import com.clement.nutrimo.data.db.DiaryEntity
import com.clement.nutrimo.domain.model.Meal
import com.clement.nutrimo.ui.common.DateField
import com.clement.nutrimo.ui.common.FormDialog
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.navigation.GoalEditRoute
import com.clement.nutrimo.ui.navigation.ProfileRoute
import com.clement.nutrimo.ui.navigation.StepsRoute
import com.clement.nutrimo.util.day
import com.clement.nutrimo.util.full
import java.time.LocalDate

/** Navigation callbacks of the journal. */
data class JournalActions(
    val onAdd: (LocalDate, Meal) -> Unit,
    val onEdit: (DiaryEntity) -> Unit,
    val onDetails: (LocalDate) -> Unit,
    val onHistory: () -> Unit,
    /** Opens the settings screen of a goal (one of the navigation routes). */
    val onGoalSettings: (route: Any) -> Unit,
)

@Composable
fun JournalScreen(actions: JournalActions) {
    val vm = appViewModel { c, _ -> JournalViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }
    var movingDay by remember { mutableStateOf(false) }
    val resources = LocalContext.current.resources
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it.resolve(resources)) } }
    LifecycleResumeEffect(Unit) {
        vm.checkToday()
        vm.refreshHealth()
        onPauseOrDispose {}
    }
    ScreenScaffold(stringResource(R.string.journal_journal), snackbar = snackbar, actions = {
        IconButton(onClick = actions.onHistory) { Icon(Icons.Default.BarChart, stringResource(R.string.common_historique)) }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.journal_plus_options)) }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(
                    { Text(stringResource(R.string.journal_deplacer_journee)) }, { menu = false; movingDay = true },
                    leadingIcon = { Icon(Icons.Default.EditCalendar, null) },
                )
            }
        }
    }) { padding -> JournalList(state, today, vm, actions, padding) }
    if (movingDay) MoveDayDialog(state.date, today, vm::moveDay) { movingDay = false }
}

/** Picks the day to move the whole shown [date] to (up to [today]). */
@Composable
private fun MoveDayDialog(date: LocalDate, today: LocalDate, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    var to by remember { mutableStateOf(date) }
    FormDialog(stringResource(R.string.journal_deplacer_journee), onDismiss, { onConfirm(to) }, stringResource(R.string.journal_deplacer), canConfirm = to != date) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.journal_deplacer_journee_aide, date.full()))
            DateField(stringResource(R.string.reminders_jour), to, { to = it }, Modifier.fillMaxWidth(), max = today)
        }
    }
}

/** The day: date bar, summary, the four meals, then steps, activities and water. */
@Composable
private fun JournalList(state: JournalUiState, today: LocalDate, vm: JournalViewModel, actions: JournalActions, padding: PaddingValues) {
    LazyColumn(
        Modifier.padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { DayBar(state.date, today, vm::shiftDay) { vm.setDate(today) } }
        item { SummaryCard(state, { actions.onDetails(state.date) }) { actions.onGoalSettings(GoalEditRoute) } }
        items(Meal.entries) { meal ->
            val entries = state.meals[meal].orEmpty()
            MealCard(
                meal,
                entries,
                state.date,
                today,
                MealCardActions(
                    onAdd = { actions.onAdd(state.date, meal) },
                    onEdit = actions.onEdit,
                    onDelete = vm::delete,
                    onCopy = { to, toMeal -> vm.copyMeal(meal, to, toMeal) },
                    onMove = { to, toMeal -> vm.moveMeal(meal, to, toMeal) },
                    onSave = { name -> vm.saveMeal(name, entries) },
                ),
            )
        }
        item { StepsCard(state.health, state.targets.steps, state.healthInBudget, vm::refreshHealth) { actions.onGoalSettings(StepsRoute) } }
        item {
            ActivityCard(state.activities, state.health?.sessions.orEmpty(), vm::addActivity, vm::deleteActivity, state.targets.sportKcal) {
                actions.onGoalSettings(StepsRoute)
            }
        }
        item { WaterCard(state.waterMl, state.targets.waterMl, vm::addWater) { actions.onGoalSettings(ProfileRoute) } }
        item { DayNoteCard(state.date, state.dayNote, vm::saveDayNote) }
    }
}

@Composable
/** [today] comes from the ViewModel (kept current), not LocalDate.now(): a skipped recomposition would keep a stale day. */
private fun DayBar(date: LocalDate, today: LocalDate, onShift: (Long) -> Unit, onToday: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = { onShift(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.journal_jour_precedent)) }
        Text(
            when (date) {
                today -> stringResource(R.string.common_aujourd_hui)
                today.minusDays(1) -> stringResource(R.string.journal_hier)
                else -> date.day()
            },
            style = MaterialTheme.typography.titleMedium,
        )
        Row {
            if (date != today) TextButton(onClick = onToday) { Text(stringResource(R.string.common_aujourd_hui)) }
            IconButton(onClick = { onShift(1) }, enabled = date.isBefore(today)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.journal_jour_suivant))
            }
        }
    }
}
