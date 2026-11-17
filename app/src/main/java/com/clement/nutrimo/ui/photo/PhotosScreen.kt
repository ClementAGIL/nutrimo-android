// Photo screen: pose filter, timeline of photo days on the weight curve, photos A / B to compare, selected day card.
package com.clement.nutrimo.ui.photo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.data.db.PhotoEntity
import com.clement.nutrimo.domain.model.PhotoPose
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.ui.common.FAB_CLEARANCE
import com.clement.nutrimo.ui.common.HintText
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.util.short

@Composable
fun PhotosScreen(onBack: () -> Unit, onCamera: () -> Unit, onCompare: (PhotoEntity, PhotoEntity) -> Unit) {
    val vm = appViewModel { c, _ -> PhotosViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    SecureWindow()
    ScreenScaffold(
        "Photos",
        onBack = onBack,
        floatingActionButton = { FloatingActionButton(onClick = onCamera) { Icon(Icons.Default.PhotoCamera, "Prendre une photo") } },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PhotoLockGate { PhotosBody(state, vm, onCompare) }
        }
    }
}

@Composable
private fun PhotosBody(state: PhotosUiState, vm: PhotosViewModel, onCompare: (PhotoEntity, PhotoEntity) -> Unit) {
    if (state.loaded && state.days.isEmpty() && state.pose == null) {
        return EmptyState("Aucune photo pour l'instant.\nPrenez la première avec le bouton appareil photo.")
    }
    ScrollColumn(PaddingValues(bottom = FAB_CLEARANCE)) {
        PoseFilter(state.pose, vm::setPose)
        SectionCard {
            if (state.days.isEmpty()) HintText("Aucune photo pour cette pose.")
            PhotoTimeline(state, vm::select)
            HintText("Touchez un point pour voir les photos du jour.")
        }
        CompareBar(state, vm, onCompare)
        state.selectedDay?.let { PhotoDayCard(it, state, vm) }
    }
}

@Composable
private fun PoseFilter(pose: PhotoPose?, onSelect: (PhotoPose?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = pose == null, onClick = { onSelect(null) }, label = { Text("Toutes") })
        PhotoPose.entries.forEach { p -> FilterChip(selected = pose == p, onClick = { onSelect(p) }, label = { Text(p.label) }) }
    }
}

/** The two chosen photos and the button opening the comparison. */
@Composable
private fun CompareBar(state: PhotosUiState, vm: PhotosViewModel, onCompare: (PhotoEntity, PhotoEntity) -> Unit) {
    SectionCard("Comparer") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CompareSlot("A", state.a, Modifier.weight(1f))
            CompareSlot("B", state.b, Modifier.weight(1f))
        }
        val a = state.a
        val b = state.b
        Button(onClick = { if (a != null && b != null) onCompare(a, b) }, enabled = a != null && b != null) { Text("Comparer A et B") }
        if (a == null || b == null) HintText("Choisissez deux photos dans la frise (« Photo A », « Photo B »).")
    }
}

@Composable
private fun CompareSlot(letter: String, photo: PhotoEntity?, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(letter, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        if (photo == null) {
            HintText("Pas encore choisie")
        } else {
            PhotoThumb(photo, Modifier.size(42.dp, 56.dp).clip(MaterialTheme.shapes.small))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(photo.date.short(), style = MaterialTheme.typography.bodyMedium)
                HintText(photo.pose.label)
            }
        }
    }
}
