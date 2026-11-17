// All goals, current and archived, newest first.
package com.clement.nutrimo.ui.goal

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.R
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.common.stateWhileSubscribed
import com.clement.nutrimo.util.fmt
import com.clement.nutrimo.util.full
import com.clement.nutrimo.util.kg

class GoalsViewModel(c: AppContainer) : ViewModel() {
    /** Newest first. */
    val goals = c.body.allGoals.stateWhileSubscribed(viewModelScope, emptyList())
}

@Composable
fun GoalsScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, _ -> GoalsViewModel(c) }
    val goals by vm.goals.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.common_mes_objectifs), onBack = onBack) { padding ->
        if (goals.isEmpty()) return@ScreenScaffold EmptyState(stringResource(R.string.goals_aucun_objectif), Modifier.padding(padding))
        LazyColumn(Modifier.padding(padding)) {
            items(goals, key = { it.id }) { goal ->
                ListItem(
                    headlineContent = { Text("${kg(goal.startKg)} → ${kg(goal.targetKg)}") },
                    supportingContent = {
                        Text(stringResource(R.string.goals_1_2_3_kg_sem, goal.startDate.full(), goal.endDate.full(), fmt(goal.paceKgPerWeek, 2)))
                    },
                    trailingContent = { Text(stringResource(goal.status.label)) },
                )
                HorizontalDivider()
            }
        }
    }
}
