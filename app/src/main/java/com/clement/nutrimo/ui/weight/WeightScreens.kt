// Screens reached from the weight hub: history (weigh-ins + goal actions) and measurements (list + charts).
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.R
import com.clement.nutrimo.data.db.MeasurementEntity
import com.clement.nutrimo.ui.common.AppTabs
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.appViewModel
import java.time.LocalDate

private val MEASUREMENT_TABS = listOf(R.string.measurements_tab_history, R.string.measurements_tab_charts)

/** Weigh-ins (newest first) and the goal actions: a new goal, or the past ones. */
@Composable
fun WeighHistoryScreen(onBack: () -> Unit, onNewGoal: () -> Unit, onGoals: () -> Unit) {
    val vm = appViewModel { c, _ -> WeightViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    ScreenScaffold(
        stringResource(R.string.common_historique),
        onBack = onBack,
        floatingActionButton = { FloatingActionButton(onClick = { adding = true }) { Icon(Icons.Default.Add, stringResource(R.string.weight_screens_ajouter_pesee)) } },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onNewGoal, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.common_nouvel_objectif)) }
                OutlinedButton(onClick = onGoals, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.common_mes_objectifs)) }
            }
            WeightHistory(state.weights, vm)
        }
    }
    if (adding) WeightDialog(vm, onDismiss = { adding = false })
}

@Composable
fun MeasurementsScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, _ -> WeightViewModel(c) }
    val list by vm.measurements.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<MeasurementEntity?>(null) }
    ScreenScaffold(
        stringResource(R.string.common_mensurations),
        onBack = onBack,
        // Only on the list: on the charts tab it would hide part of a chart.
        floatingActionButton = {
            if (tab == 0) FloatingActionButton(onClick = { editing = MeasurementEntity(date = LocalDate.now()) }) { Icon(Icons.Default.Add, stringResource(R.string.weight_screens_ajouter_mensurations)) }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            AppTabs(MEASUREMENT_TABS.map { stringResource(it) }, tab) { tab = it }
            if (tab == 0) MeasurementList(list, { editing = it }) { vm.deleteMeasurement(it.id) } else MeasurementCharts(list)
        }
    }
    editing?.let { MeasurementDialog(it, vm::saveMeasurement) { editing = null } }
}
