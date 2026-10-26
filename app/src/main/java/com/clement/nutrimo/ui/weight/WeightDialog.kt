// Dialog to add or edit a weigh-in; asks before replacing an existing weigh-in on the same day.
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.ui.common.DateField
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.full
import com.clement.nutrimo.ui.common.kg
import com.clement.nutrimo.ui.common.toDecimal
import com.clement.nutrimo.ui.common.toInput
import kotlinx.coroutines.launch
import java.time.LocalDate

private val WEIGHT_RANGE = 30.0..400.0

@Composable
fun WeightDialog(
    vm: WeightViewModel,
    initialDate: LocalDate = LocalDate.now(),
    initialKg: Double? = null,
    onDismiss: () -> Unit,
    /** Editing an entry: saving on its own date is not a replacement. */
    editing: Boolean = initialKg != null,
) {
    var date by remember { mutableStateOf(initialDate) }
    var text by remember { mutableStateOf(initialKg?.toInput() ?: "") }
    var replacing by remember { mutableStateOf<Double?>(null) }
    val scope = rememberCoroutineScope()
    val value = text.toDecimal()?.takeIf { it in WEIGHT_RANGE }
    val save = { kg: Double -> vm.save(date, kg); onDismiss() }

    if (replacing != null) {
        AlertDialog(
            onDismissRequest = { replacing = null },
            title = { Text("Remplacer la pesée ?") },
            text = { Text("Il y a déjà ${kg(replacing!!)} le ${date.full()}. Une seule pesée par jour est conservée.") },
            confirmButton = { TextButton(onClick = { value?.let(save) }) { Text("Remplacer") } },
            dismissButton = { TextButton(onClick = { replacing = null }) { Text("Annuler") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pesée") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // An existing weigh-in keeps its date; moving it would leave the old day behind.
                if (editing) Text(date.full())
                else DateField("Date", date, { date = it }, Modifier.fillMaxWidth(), max = LocalDate.now())
                NumberField(text, { text = it }, "Poids", Modifier.fillMaxWidth(), "kg", isError = text.isNotEmpty() && value == null)
            }
        },
        confirmButton = {
            TextButton(enabled = value != null, onClick = {
                val kgValue = value ?: return@TextButton
                scope.launch {
                    val existing = vm.existingOn(date)
                    if (existing != null && !(editing && date == initialDate)) replacing = existing.kg else save(kgValue)
                }
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
