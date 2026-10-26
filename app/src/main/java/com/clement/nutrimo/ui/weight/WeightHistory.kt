// History tab: weigh-ins newest first, with the change since the previous one; tap to edit, bin to delete.
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.clement.nutrimo.data.db.WeightEntity
import com.clement.nutrimo.ui.common.ConfirmDialog
import com.clement.nutrimo.ui.common.FAB_CLEARANCE
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.ui.common.day
import com.clement.nutrimo.ui.common.kg
import com.clement.nutrimo.ui.common.signedKg
import com.clement.nutrimo.ui.theme.NutrimoColors

@Composable
fun WeightHistory(weights: List<WeightEntity>, vm: WeightViewModel) {
    var editing by remember { mutableStateOf<WeightEntity?>(null) }
    var deleting by remember { mutableStateOf<WeightEntity?>(null) }
    if (weights.isEmpty()) return EmptyState("Aucune pesée pour l'instant.\nAppuyez sur + pour en ajouter une.")
    val newestFirst = weights.reversed()
    LazyColumn(contentPadding = PaddingValues(bottom = FAB_CLEARANCE)) {
        items(newestFirst.size, key = { newestFirst[it].id }) { i ->
            val entry = newestFirst[i]
            WeightRow(entry, newestFirst.getOrNull(i + 1), onEdit = { editing = entry }, onDelete = { deleting = entry })
            HorizontalDivider()
        }
    }
    editing?.let { WeightDialog(vm, it.date, it.kg, onDismiss = { editing = null }) }
    deleting?.let {
        ConfirmDialog("Supprimer la pesée ?", "${kg(it.kg)} le ${it.date.day()}", "Supprimer", { vm.delete(it.id) }, { deleting = null })
    }
}

@Composable
private fun WeightRow(entry: WeightEntity, previous: WeightEntity?, onEdit: () -> Unit, onDelete: () -> Unit) {
    val delta = previous?.let { entry.kg - it.kg }
    ListItem(
        headlineContent = { Text(kg(entry.kg), fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(entry.date.day()) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                delta?.let { DeltaText(it) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Supprimer") }
            }
        },
        modifier = Modifier.clickable(onClick = onEdit),
    )
}

@Composable
private fun DeltaText(delta: Double) {
    val color = when {
        delta < 0 -> NutrimoColors.weight
        delta > 0 -> NutrimoColors.fat
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(signedKg(delta), color = color)
}
