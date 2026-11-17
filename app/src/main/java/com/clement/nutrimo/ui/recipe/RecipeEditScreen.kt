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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.R
import com.clement.nutrimo.data.repository.IngredientLine
import com.clement.nutrimo.ui.common.FormDialog
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.WarningText
import com.clement.nutrimo.ui.common.WideButton
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.common.asString
import com.clement.nutrimo.util.fmt
import com.clement.nutrimo.util.kcal
import com.clement.nutrimo.util.toDecimal
import com.clement.nutrimo.util.toInput

@Composable
fun RecipeEditScreen(onBack: () -> Unit, onAddIngredient: () -> Unit, picked: String?, onPickedConsumed: () -> Unit) {
    val vm = appViewModel { c, handle -> RecipeEditViewModel(c, handle) }
    val draft by vm.draft.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(picked) { picked?.let { onPickedConsumed(); vm.addIngredient(it) } }
    val resources = LocalContext.current.resources
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it.resolve(resources)) } }
    ScreenScaffold(if (vm.isEdit) stringResource(R.string.recipe_edit_modifier_recette) else stringResource(R.string.common_nouvelle_recette), onBack = onBack, snackbar = snackbar) { padding ->
        ScrollColumn(padding) {
            OutlinedTextField(draft.name, { v -> vm.update { it.copy(name = v) } }, label = { Text(stringResource(R.string.recipe_edit_nom)) }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(draft.parts, { v -> vm.update { it.copy(parts = v) } }, stringResource(R.string.recipe_edit_nombre_parts), Modifier.weight(1f))
                NumberField(draft.cookedWeight, { v -> vm.update { it.copy(cookedWeight = v) } }, stringResource(R.string.recipe_edit_poids_cuit), Modifier.weight(1f), "g",
                    supportingText = stringResource(R.string.common_optional))
            }
            IngredientsCard(draft.lines, onEdit = { editing = it }, onRemove = vm::remove, onAdd = onAddIngredient)
            TotalsCard(draft)
            draft.error?.let { WarningText(it.asString()) }
            WideButton(stringResource(R.string.common_enregistrer), { vm.save(onBack) }, enabled = draft.error == null)
        }
    }
    editing?.let { i -> draft.lines.getOrNull(i)?.let { QuantityDialog(it, { q -> vm.setQuantity(i, q) }, { editing = null }) } }
}

@Composable
private fun IngredientsCard(lines: List<IngredientLine>, onEdit: (Int) -> Unit, onRemove: (Int) -> Unit, onAdd: () -> Unit) {
    SectionCard(stringResource(R.string.recipe_edit_ingredients)) {
        lines.forEachIndexed { i, line ->
            Row(Modifier.fillMaxWidth().clickable { onEdit(i) }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(line.name.asString(), maxLines = 2)
                    Text(
                        "${ingredientQuantity(line)} · ${line.contribution?.let { kcal(it.nutrients.kcal) } ?: "–"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = { onRemove(i) }) { Icon(Icons.Default.Close, stringResource(R.string.common_retirer)) }
            }
        }
        WideButton(stringResource(R.string.common_ajouter_ingredient), onAdd, outlined = true)
    }
}

@Composable
private fun TotalsCard(draft: RecipeDraft) {
    val t = draft.totals ?: return
    SectionCard(stringResource(R.string.recipe_edit_valeurs)) {
        StatRow(stringResource(R.string.recipe_edit_total), kcal(t.total.kcal))
        StatRow(stringResource(R.string.common_par_part), kcal(t.perPart.kcal))
        StatRow(stringResource(R.string.recipe_edit_poids_total), stringResource(if (t.cookedWeightG == null) R.string.recipe_edit_raw_weight else R.string.recipe_edit_weight, fmt(t.weightG, 0)))
        t.per100?.let { StatRow(stringResource(R.string.common_pour_100_g), kcal(it.kcal)) }
    }
}

@Composable
private fun QuantityDialog(line: IngredientLine, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(line.ingredient.quantity.toInput()) }
    val isPart = line.ingredient.subRecipeId != null
    val quantity = text.toDecimal()?.takeIf { it > 0 }
    FormDialog(line.name.asString(), onDismiss, { quantity?.let(onSave) }, canConfirm = quantity != null) {
        NumberField(text, { text = it }, stringResource(R.string.common_quantite), suffix = if (isPart) stringResource(R.string.common_parts_unit) else "g")
    }
}

@Composable
fun ingredientQuantity(line: IngredientLine): String =
    if (line.ingredient.subRecipeId != null) stringResource(R.string.common_parts_count, fmt(line.ingredient.quantity)) else "${fmt(line.ingredient.quantity)} g"
