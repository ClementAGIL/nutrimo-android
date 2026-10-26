// Weight tab: dashboard / history / graph sub-tabs and the "+" button to add a weigh-in.
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.appViewModel

private val TABS = listOf("Bilan", "Historique", "Graphique")

@Composable
fun WeightScreen(onNewGoal: () -> Unit, onGoals: () -> Unit) {
    val vm = appViewModel { c, _ -> WeightViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    ScreenScaffold(
        title = "Poids",
        floatingActionButton = { FloatingActionButton(onClick = { adding = true }) { Icon(Icons.Default.Add, "Ajouter une pesée") } },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                TABS.forEachIndexed { i, title -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) }) }
            }
            when (tab) {
                0 -> WeightDashboard(state, onNewGoal, onGoals, vm::markGoalAchieved)
                1 -> WeightHistory(state.weights, vm)
                else -> WeightChartTab(state)
            }
        }
    }
    if (adding) WeightDialog(vm, onDismiss = { adding = false })
}
