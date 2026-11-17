// Weight tab = hub: one chart (weight, goal, trend, measurement and photo days), the goal's Bilan, the ways into
// Historique / Mensurations / Photos, and a "+" offering a weigh-in, measurements or a photo.
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.R
import com.clement.nutrimo.data.db.MeasurementEntity
import com.clement.nutrimo.ui.common.FAB_CLEARANCE
import com.clement.nutrimo.ui.common.IntroText
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.appViewModel
import java.time.LocalDate

/** Where the hub leads; [onPhotos] and [onCamera] lock the photos again (entering them from outside). */
class WeightHubActions(val onHistory: () -> Unit, val onNewGoal: () -> Unit, val onMeasurements: () -> Unit, val onPhotos: () -> Unit, val onCamera: () -> Unit)

@Composable
fun WeightScreen(actions: WeightHubActions) {
    val vm = appViewModel { c, _ -> WeightViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val measurements by vm.measurements.collectAsStateWithLifecycle()
    val photos by vm.photos.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var measurement by remember { mutableStateOf<MeasurementEntity?>(null) }
    ScreenScaffold(
        title = stringResource(R.string.common_poids),
        floatingActionButton = { AddMenu({ adding = true }, { measurement = MeasurementEntity(date = LocalDate.now()) }, actions.onCamera) },
    ) { padding ->
        ScrollColumn(padding) {
            if (state.weights.isEmpty()) SectionCard { IntroText(stringResource(R.string.weight_ajoutez_votre_premiere_pesee_avec)) }
            else WeightHubChart(state, measurements, photos, actions)
            HubButtons(actions)
            WeightDashboard(state, actions.onNewGoal, vm::markGoalAchieved)
            Spacer(Modifier.height(FAB_CLEARANCE))
        }
    }
    if (adding) WeightDialog(vm, onDismiss = { adding = false })
    measurement?.let { MeasurementDialog(it, vm::saveMeasurement) { measurement = null } }
}

/** The chart is the main entry; these buttons are for users who don't know they can tap it. */
@Composable
private fun HubButtons(actions: WeightHubActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HubButton(stringResource(R.string.common_historique), Icons.Default.History, actions.onHistory, Modifier.weight(1f))
        HubButton(stringResource(R.string.common_mensurations), Icons.Default.Straighten, actions.onMeasurements, Modifier.weight(1f))
        HubButton(stringResource(R.string.common_photos), Icons.Default.PhotoLibrary, actions.onPhotos, Modifier.weight(1f))
    }
}

@Composable
private fun HubButton(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null)
            Text(label, maxLines = 1)
        }
    }
}

@Composable
private fun AddMenu(onWeight: () -> Unit, onMeasurement: () -> Unit, onPhoto: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FloatingActionButton(onClick = { open = true }) { Icon(Icons.Default.Add, stringResource(R.string.common_ajouter)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuItem(stringResource(R.string.common_pesee), Icons.Default.MonitorWeight) { open = false; onWeight() }
            MenuItem(stringResource(R.string.common_mensurations), Icons.Default.Straighten) { open = false; onMeasurement() }
            MenuItem(stringResource(R.string.weight_photo), Icons.Default.PhotoCamera) { open = false; onPhoto() }
        }
    }
}

@Composable
private fun MenuItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, onClick = onClick, leadingIcon = { Icon(icon, null) }, modifier = Modifier.padding(end = 8.dp))
}
