// Card of the selected photo day: weight, thumbnails per pose; pick one to set it as photo A or B, or delete it.
package com.clement.nutrimo.ui.photo

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.R
import com.clement.nutrimo.data.db.PhotoEntity
import com.clement.nutrimo.ui.common.ConfirmDialog
import com.clement.nutrimo.ui.common.HintText
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.util.day
import com.clement.nutrimo.util.kg

@Composable
fun PhotoDayCard(day: PhotoDay, state: PhotosUiState, vm: PhotosViewModel) {
    // The picked photo of the day, reset when another day is selected.
    var current by remember(day.date) { mutableStateOf(day.photos.first().id) }
    val photo = day.photos.firstOrNull { it.id == current } ?: day.photos.first()
    var deleting by remember { mutableStateOf<PhotoEntity?>(null) }
    SectionCard(day.date.day()) {
        HintText(weightText(day))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            day.photos.forEach { p -> DayThumb(p, selected = p.id == photo.id, chosen = chosenLetter(p, state)) { current = p.id } }
        }
        photo.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { vm.setA(photo) }, Modifier.weight(1f)) { Text(stringResource(R.string.common_photo)) }
            OutlinedButton(onClick = { vm.setB(photo) }, Modifier.weight(1f)) { Text(stringResource(R.string.common_photo_b)) }
            IconButton(onClick = { deleting = photo }) { Icon(Icons.Default.Delete, stringResource(R.string.photo_day_supprimer_photo)) }
        }
    }
    deleting?.let {
        ConfirmDialog(stringResource(R.string.photo_day_supprimer_photo_2), stringResource(R.string.photo_day_photo_1_2, stringResource(it.pose.label), it.date.day()), stringResource(R.string.common_supprimer), { vm.delete(it) }, { deleting = null })
    }
}

@Composable
private fun weightText(day: PhotoDay): String = when {
    day.kg == null -> stringResource(R.string.photo_day_no_weight)
    day.weighedThatDay -> stringResource(R.string.photo_day_weighed, kg(day.kg))
    else -> stringResource(R.string.photo_day_curve, kg(day.kg))
}

private fun chosenLetter(photo: PhotoEntity, state: PhotosUiState): String? =
    listOfNotNull("A".takeIf { state.a?.id == photo.id }, "B".takeIf { state.b?.id == photo.id }).joinToString("").ifEmpty { null }

@Composable
private fun DayThumb(photo: PhotoEntity, selected: Boolean, chosen: String?, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PhotoThumb(
            photo,
            Modifier.size(90.dp, 120.dp).clip(MaterialTheme.shapes.medium).border(3.dp, border, MaterialTheme.shapes.medium).clickable(onClick = onClick),
        )
        Text(stringResource(photo.pose.label) + (chosen?.let { " · $it" } ?: ""), style = MaterialTheme.typography.labelMedium)
    }
}
