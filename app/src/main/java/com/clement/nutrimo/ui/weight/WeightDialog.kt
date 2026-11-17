// Dialog to add or edit a weigh-in; asks before replacing an existing weigh-in on the same day.
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.R
import com.clement.nutrimo.ui.common.ConfirmDialog
import com.clement.nutrimo.ui.common.DateField
import com.clement.nutrimo.ui.common.FormDialog
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.util.full
import com.clement.nutrimo.util.kg
import com.clement.nutrimo.util.toDecimal
import com.clement.nutrimo.util.toInput
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
        ConfirmDialog(
            stringResource(R.string.weight_remplacer_pesee), stringResource(R.string.weight_il_y_deja_1_2, kg(replacing!!), date.full()),
            stringResource(R.string.weight_remplacer), { value?.let(save) }, { replacing = null },
        )
        return
    }
    // Not closed on confirm: an existing weigh-in that day first asks before replacing it.
    FormDialog(stringResource(R.string.common_pesee), onDismiss, {
        val kgValue = value ?: return@FormDialog
        scope.launch {
            val existing = vm.existingOn(date)
            if (existing != null && !(editing && date == initialDate)) replacing = existing.kg else save(kgValue)
        }
    }, confirmLabel = stringResource(R.string.common_enregistrer), canConfirm = value != null, closeOnConfirm = false) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // An existing weigh-in keeps its date; moving it would leave the old day behind.
            if (editing) Text(date.full())
            else DateField(stringResource(R.string.common_date), date, { date = it }, Modifier.fillMaxWidth(), max = LocalDate.now())
            NumberField(text, { text = it }, stringResource(R.string.common_poids), Modifier.fillMaxWidth(), "kg", isError = text.isNotEmpty() && value == null)
        }
    }
}
