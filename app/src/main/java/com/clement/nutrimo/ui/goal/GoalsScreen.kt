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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.appContainer
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.full
import com.clement.nutrimo.ui.common.kg

@Composable
fun GoalsScreen(onBack: () -> Unit) {
    val goals by appContainer().body.allGoals.collectAsStateWithLifecycle(initialValue = emptyList())
    ScreenScaffold("Mes objectifs", onBack = onBack) { padding ->
        if (goals.isEmpty()) return@ScreenScaffold EmptyState("Aucun objectif", Modifier.padding(padding))
        LazyColumn(Modifier.padding(padding)) {
            items(goals, key = { it.id }) { goal ->
                ListItem(
                    headlineContent = { Text("${kg(goal.startKg)} → ${kg(goal.targetKg)}") },
                    supportingContent = {
                        Text("${goal.startDate.full()} → ${goal.endDate.full()} · ${fmt(goal.paceKgPerWeek, 2)} kg/sem")
                    },
                    trailingContent = { Text(goal.status.label) },
                )
                HorizontalDivider()
            }
        }
    }
}
