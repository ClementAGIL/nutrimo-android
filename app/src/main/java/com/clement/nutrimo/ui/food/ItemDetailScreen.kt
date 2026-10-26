// Quantity screen for a food or recipe: amount + unit, meal, live nutrients, favorite; add / save / delete.
package com.clement.nutrimo.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.domain.model.Meal
import com.clement.nutrimo.ui.common.Dropdown
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.NutrientTable
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.common.fmt

data class ItemDetailActions(
    val onBack: () -> Unit,
    /** After adding / saving / deleting a diary entry. */
    val onDone: () -> Unit,
    /** Recipe mode: the encoded ingredient. */
    val onPicked: (String) -> Unit,
    val onEditCustom: (String) -> Unit,
)

@Composable
fun ItemDetailScreen(actions: ItemDetailActions) {
    val vm = appViewModel { c, handle -> ItemDetailViewModel(c, handle) }
    val state by vm.state.collectAsStateWithLifecycle()
    val item = state.item ?: return
    ScreenScaffold(item.name, onBack = actions.onBack, actions = {
        if (state.isCustom) IconButton(onClick = { item.foodId?.let(actions.onEditCustom) }) { Icon(Icons.Default.Edit, "Modifier") }
        if (item.foodId != null || item.recipeId != null) {
            IconButton(onClick = vm::toggleFavorite) {
                Icon(if (state.favorite) Icons.Default.Star else Icons.Default.StarBorder, "Favori")
            }
        }
    }) { padding ->
        ScrollColumn(padding) {
            item.subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            item.per100?.let { Text("${fmt(it.kcal, 0)} kcal pour 100 g", style = MaterialTheme.typography.bodySmall) }
            QuantityRow(state, vm)
            if (!vm.route.forRecipe) Dropdown("Repas", state.meal, Meal.entries, { it.label }, vm::setMeal, Modifier.fillMaxWidth())
            state.preview?.let { NutrientTable(it, "Pour cette quantité") }
            Buttons(state, vm, actions)
        }
    }
}

@Composable
private fun QuantityRow(state: ItemDetailState, vm: ItemDetailViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(state.quantity, vm::setQuantity, "Quantité", Modifier.weight(1f), isError = state.amount == null)
        val unit = state.unit
        if (state.entry == null && unit != null) {
            Dropdown("Unité", unit, state.units, { it.label }, vm::setUnit, Modifier.weight(1.4f))
        }
    }
}

@Composable
private fun Buttons(state: ItemDetailState, vm: ItemDetailViewModel, actions: ItemDetailActions) {
    val enabled = state.amount != null
    when {
        vm.route.forRecipe -> Button(onClick = { vm.ingredientResult()?.let(actions.onPicked) }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("Ajouter à la recette")
        }
        state.entry != null -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.delete(actions.onDone) }, modifier = Modifier.weight(1f)) { Text("Supprimer") }
            Button(onClick = { vm.save(actions.onDone) }, enabled = enabled, modifier = Modifier.weight(1f)) { Text("Enregistrer") }
        }
        else -> Button(onClick = { vm.save(actions.onDone) }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("Ajouter au journal")
        }
    }
}
