// Recipe editor: name, parts, cooked weight, ingredient list (tap to change the quantity), live totals.
package com.clement.nutrimo.ui.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.data.repository.IngredientLine
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.WarningText
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.kcal
import com.clement.nutrimo.ui.common.toDecimal
import com.clement.nutrimo.ui.common.toInput

@Composable
fun RecipeEditScreen(onBack: () -> Unit, onAddIngredient: () -> Unit, picked: String?, onPickedConsumed: () -> Unit) {
    val vm = appViewModel { c, handle -> RecipeEditViewModel(c, handle) }
    val draft by vm.draft.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(picked) { picked?.let { onPickedConsumed(); vm.addIngredient(it) } }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    ScreenScaffold(if (vm.isEdit) "Modifier la recette" else "Nouvelle recette", onBack = onBack, snackbar = snackbar) { padding ->
        ScrollColumn(padding) {
            OutlinedTextField(draft.name, { v -> vm.update { it.copy(name = v) } }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(draft.parts, { v -> vm.update { it.copy(parts = v) } }, "Nombre de parts", Modifier.weight(1f))
                NumberField(draft.cookedWeight, { v -> vm.update { it.copy(cookedWeight = v) } }, "Poids cuit", Modifier.weight(1f), "g",
                    supportingText = "facultatif")
            }
            IngredientsCard(draft.lines, onEdit = { editing = it }, onRemove = vm::remove, onAdd = onAddIngredient)
            TotalsCard(draft)
            draft.error?.let { WarningText(it) }
            Button(onClick = { vm.save(onBack) }, enabled = draft.error == null, modifier = Modifier.fillMaxWidth()) { Text("Enregistrer") }
        }
    }
    editing?.let { i -> draft.lines.getOrNull(i)?.let { QuantityDialog(it, { q -> vm.setQuantity(i, q) }, { editing = null }) } }
}

@Composable
private fun IngredientsCard(lines: List<IngredientLine>, onEdit: (Int) -> Unit, onRemove: (Int) -> Unit, onAdd: () -> Unit) {
    SectionCard("Ingrédients") {
        lines.forEachIndexed { i, line ->
            Row(Modifier.fillMaxWidth().clickable { onEdit(i) }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(line.name, maxLines = 2)
                    Text(
                        "${ingredientQuantity(line)} · ${line.contribution?.let { kcal(it.nutrients.kcal) } ?: "–"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = { onRemove(i) }) { Icon(Icons.Default.Close, "Retirer") }
            }
        }
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Ajouter un ingrédient") }
    }
}

@Composable
private fun TotalsCard(draft: RecipeDraft) {
    val t = draft.totals ?: return
    SectionCard("Valeurs") {
        StatRow("Total", kcal(t.total.kcal))
        StatRow("Par part", kcal(t.perPart.kcal))
        StatRow("Poids total", "${fmt(t.weightG, 0)} g${if (t.cookedWeightG == null) " (cru)" else ""}")
        t.per100?.let { StatRow("Pour 100 g", kcal(it.kcal)) }
    }
}

@Composable
private fun QuantityDialog(line: IngredientLine, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(line.ingredient.quantity.toInput()) }
    val isPart = line.ingredient.subRecipeId != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(line.name) },
        text = { NumberField(text, { text = it }, "Quantité", suffix = if (isPart) "part(s)" else "g") },
        confirmButton = {
            TextButton(onClick = { text.toDecimal()?.takeIf { it > 0 }?.let(onSave); onDismiss() }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

fun ingredientQuantity(line: IngredientLine): String =
    if (line.ingredient.subRecipeId != null) "${fmt(line.ingredient.quantity)} part(s)" else "${fmt(line.ingredient.quantity)} g"
