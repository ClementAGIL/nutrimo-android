// Journal tab: day switcher, daily summary, the 4 meals and water.
package com.clement.nutrimo.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.data.db.DiaryEntity
import com.clement.nutrimo.domain.model.Meal
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.common.day
import java.time.LocalDate

/** Navigation callbacks of the journal. */
data class JournalActions(
    val onAdd: (LocalDate, Meal) -> Unit,
    val onEdit: (DiaryEntity) -> Unit,
    val onDetails: (LocalDate) -> Unit,
)

@Composable
fun JournalScreen(actions: JournalActions) {
    val vm = appViewModel { c, _ -> JournalViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    ScreenScaffold("Journal", snackbar = snackbar) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { DayBar(state.date, vm::shiftDay) { vm.setDate(LocalDate.now()) } }
            item { SummaryCard(state) { actions.onDetails(state.date) } }
            items(Meal.entries) { meal ->
                MealCard(
                    meal = meal,
                    entries = state.meals[meal].orEmpty(),
                    onAdd = { actions.onAdd(state.date, meal) },
                    onEdit = actions.onEdit,
                    onDelete = vm::delete,
                    onCopy = { vm.copyFromPreviousDay(meal) },
                )
            }
            item { WaterCard(state.waterMl, state.waterGoalMl, vm::addWater) }
        }
    }
}

@Composable
private fun DayBar(date: LocalDate, onShift: (Long) -> Unit, onToday: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = { onShift(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Jour précédent") }
        val today = LocalDate.now()
        Text(
            when (date) {
                today -> "Aujourd'hui"
                today.minusDays(1) -> "Hier"
                else -> date.day()
            },
            style = MaterialTheme.typography.titleMedium,
        )
        Row {
            if (date != today) TextButton(onClick = onToday) { Text("Aujourd'hui") }
            IconButton(onClick = { onShift(1) }, enabled = date.isBefore(today)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Jour suivant")
            }
        }
    }
}
