// Review of a shot before saving it: the photo, an optional note, the day's weight and measurements.
package com.clement.nutrimo.ui.photo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.data.db.MeasurementEntity
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.weight.MeasurementDialog
import kotlinx.coroutines.launch

@Composable
fun PhotoReview(vm: PhotoCameraViewModel, state: PhotoCameraState) {
    val shot = state.shot ?: return
    ScrollColumn(androidx.compose.foundation.layout.PaddingValues()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Image(
                shot.asImageBitmap(), "Photo ${state.pose.label}",
                Modifier.fillMaxWidth(0.75f).aspectRatio(shot.width.toFloat() / shot.height).clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Fit,
            )
        }
        Text("Pose : ${state.pose.label}", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(state.note, vm::setNote, Modifier.fillMaxWidth(), label = { Text("Note (facultatif)") })
        NumberField(state.weight, vm::setWeight, "Poids du jour (facultatif)", Modifier.fillMaxWidth(), "kg")
        MeasurementsButton(vm)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::retake, Modifier.weight(1f), enabled = !state.saving) { Text("Reprendre") }
            Button(onClick = vm::save, Modifier.weight(1f), enabled = !state.saving) { Text("Enregistrer") }
        }
    }
}

/** Opens the usual measurement dialog for today, pre-filled with today's entry when there is one. */
@Composable
private fun MeasurementsButton(vm: PhotoCameraViewModel) {
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<MeasurementEntity?>(null) }
    TextButton(onClick = { scope.launch { editing = vm.todayMeasurement() } }) { Text("Ajouter des mensurations pour aujourd'hui") }
    editing?.let { m -> MeasurementDialog(m, vm::saveMeasurement) { editing = null } }
}
