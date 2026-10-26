// Diary entry helpers: quantity text ("120 g", "1,5 part(s)", "2 × 1 pot") and swipe-to-delete.
package com.clement.nutrimo.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.data.db.DiaryEntity
import com.clement.nutrimo.domain.model.QuantityUnit
import com.clement.nutrimo.ui.common.fmt

fun quantityText(entry: DiaryEntity): String = when (entry.unit) {
    QuantityUnit.GRAM -> "${fmt(entry.quantity)} g"
    QuantityUnit.PART -> "${fmt(entry.quantity)} part(s)"
    QuantityUnit.PORTION -> "${fmt(entry.quantity)} × ${entry.portionLabel} (${fmt(entry.portionGrams ?: 0.0)} g)"
}

/** Swipe from right to left to delete. */
@Composable
fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(confirmValueChange = {
        if (it == SwipeToDismissBoxValue.EndToStart) onDelete()
        it == SwipeToDismissBoxValue.EndToStart
    })
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) { Icon(Icons.Default.Delete, "Supprimer", tint = MaterialTheme.colorScheme.onErrorContainer) }
        },
    ) {
        Box(Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(vertical = 6.dp)) { content() }
    }
}
