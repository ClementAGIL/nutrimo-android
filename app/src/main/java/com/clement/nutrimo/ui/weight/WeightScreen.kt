// Weight tab: a Poids / Mensurations switch, each with its sub-tabs, and the "+" button adding to the current one.
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.data.db.MeasurementEntity
import com.clement.nutrimo.ui.common.AppTabs
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.appViewModel
import java.time.LocalDate

private enum class BodyView(val label: String, val tabs: List<String>, val addLabel: String) {
    WEIGHT("Poids", listOf("Bilan", "Historique", "Graphique"), "Ajouter une pesée"),
    MEASUREMENTS("Mensurations", listOf("Historique", "Graphiques"), "Ajouter des mensurations"),
}

@Composable
fun WeightScreen(onNewGoal: () -> Unit, onGoals: () -> Unit) {
    val vm = appViewModel { c, _ -> WeightViewModel(c) }
    var view by rememberSaveable { mutableStateOf(BodyView.WEIGHT) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    var measurement by remember { mutableStateOf<MeasurementEntity?>(null) }
    ScreenScaffold(
        title = "Poids",
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (view == BodyView.WEIGHT) adding = true else measurement = MeasurementEntity(date = LocalDate.now())
            }) { Icon(Icons.Default.Add, view.addLabel) }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            ViewSwitch(view) {
                view = it
                tab = 0
            }
            AppTabs(view.tabs, tab) { tab = it }
            if (view == BodyView.WEIGHT) WeightTabs(vm, tab, onNewGoal, onGoals) else MeasurementTabs(vm, tab) { measurement = it }
        }
    }
    if (adding) WeightDialog(vm, onDismiss = { adding = false })
    measurement?.let { MeasurementDialog(it, vm::saveMeasurement) { measurement = null } }
}

@Composable
private fun ViewSwitch(view: BodyView, onSelect: (BodyView) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        BodyView.entries.forEachIndexed { i, v ->
            SegmentedButton(
                selected = view == v,
                onClick = { onSelect(v) },
                shape = SegmentedButtonDefaults.itemShape(i, BodyView.entries.size),
            ) { Text(v.label) }
        }
    }
}

@Composable
private fun WeightTabs(vm: WeightViewModel, tab: Int, onNewGoal: () -> Unit, onGoals: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    when (tab) {
        0 -> WeightDashboard(state, onNewGoal, onGoals, vm::markGoalAchieved)
        1 -> WeightHistory(state.weights, vm)
        else -> WeightChartTab(state)
    }
}

@Composable
private fun MeasurementTabs(vm: WeightViewModel, tab: Int, onEdit: (MeasurementEntity) -> Unit) {
    val list by vm.measurements.collectAsStateWithLifecycle()
    if (tab == 0) MeasurementList(list, onEdit) { vm.deleteMeasurement(it.id) } else MeasurementCharts(list)
}
