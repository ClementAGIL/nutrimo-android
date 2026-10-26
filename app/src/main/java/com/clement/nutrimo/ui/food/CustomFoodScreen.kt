// Custom food form: identity, label nutrients per 100 g, optional vitamins/minerals and portions.
package com.clement.nutrimo.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.domain.model.NutrientCatalog
import com.clement.nutrimo.ui.common.ConfirmDialog
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.WarningText
import com.clement.nutrimo.ui.common.appViewModel

@Composable
fun CustomFoodScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, handle -> CustomFoodViewModel(c, handle) }
    val draft by vm.draft.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    ScreenScaffold(if (vm.isEdit) "Modifier l'aliment" else "Nouvel aliment", onBack = onBack, actions = {
        if (vm.isEdit) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Supprimer") }
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IdentityCard(draft, vm)
            NutrientsCard(draft, vm)
            MicrosCard(draft, vm)
            PortionsCard(draft, vm)
            if (draft.missing.isNotEmpty()) WarningText("Obligatoire : ${draft.missing.joinToString(", ")}")
            Button(onClick = { vm.save(onBack) }, enabled = draft.missing.isEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text("Enregistrer")
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog("Supprimer l'aliment ?", "Les jours déjà enregistrés ne changent pas.", "Supprimer", { vm.delete(onBack) }, { confirmDelete = false })
    }
}

@Composable
private fun IdentityCard(d: CustomFoodDraft, vm: CustomFoodViewModel) {
    SectionCard {
        OutlinedTextField(d.name, { v -> vm.update { it.copy(name = v) } }, label = { Text("Nom *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(d.brand, { v -> vm.update { it.copy(brand = v) } }, label = { Text("Marque") }, modifier = Modifier.fillMaxWidth())
        NumberField(d.barcode, { v -> vm.update { it.copy(barcode = v.filter(Char::isDigit)) } }, "Code-barres", Modifier.fillMaxWidth())
    }
}

@Composable
private fun NutrientsCard(d: CustomFoodDraft, vm: CustomFoodViewModel) {
    SectionCard("Valeurs pour 100 g") {
        MACRO_FIELDS.forEach { f ->
            NumberField(
                value = d.macros[f.key].orEmpty(),
                onValueChange = { v -> vm.update { it.copy(macros = it.macros + (f.key to v)) } },
                label = if (f.required) "${f.label} *" else f.label,
                suffix = f.unit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MicrosCard(d: CustomFoodDraft, vm: CustomFoodViewModel) {
    var open by rememberSaveable { mutableStateOf(d.micros.isNotEmpty()) }
    SectionCard {
        TextButton(onClick = { open = !open }) { Text(if (open) "Masquer vitamines et minéraux" else "Vitamines et minéraux (facultatif)") }
        if (!open) return@SectionCard
        NutrientCatalog.MICROS.forEach { m ->
            NumberField(
                value = d.micros[m.key].orEmpty(),
                onValueChange = { v -> vm.update { it.copy(micros = it.micros + (m.key to v)) } },
                label = m.label,
                suffix = m.unit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PortionsCard(d: CustomFoodDraft, vm: CustomFoodViewModel) {
    SectionCard("Portions (facultatif)") {
        d.portions.forEachIndexed { i, p ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(p.label, { v -> vm.update { it.copy(portions = it.portions.replace(i, p.copy(label = v))) } },
                    label = { Text("Nom (ex : 1 tranche)") }, modifier = Modifier.weight(1.5f))
                NumberField(p.grams, { v -> vm.update { it.copy(portions = it.portions.replace(i, p.copy(grams = v))) } },
                    "Poids", Modifier.weight(1f), "g")
                IconButton(onClick = { vm.update { it.copy(portions = it.portions.filterIndexed { j, _ -> j != i }) } }) {
                    Icon(Icons.Default.Close, "Retirer")
                }
            }
        }
        TextButton(onClick = { vm.update { it.copy(portions = it.portions + PortionDraft()) } }) { Text("Ajouter une portion") }
    }
}

private fun <T> List<T>.replace(index: Int, value: T): List<T> = mapIndexed { i, v -> if (i == index) value else v }
