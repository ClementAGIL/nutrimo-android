// Recipe editor state: name, parts, optional cooked weight, ingredients (foods in g, sub-recipes in parts).
package com.clement.nutrimo.ui.recipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.db.IngredientEntity
import com.clement.nutrimo.data.db.RecipeEntity
import com.clement.nutrimo.data.db.newId
import com.clement.nutrimo.data.repository.IngredientLine
import com.clement.nutrimo.data.repository.RecipeDetail
import com.clement.nutrimo.domain.calc.RecipeMath
import com.clement.nutrimo.domain.calc.RecipeTotals
import com.clement.nutrimo.ui.common.toDecimal
import com.clement.nutrimo.ui.common.toInput
import com.clement.nutrimo.ui.navigation.RecipeEditRoute
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeDraft(
    val id: String = newId(),
    val existing: RecipeEntity? = null,
    val name: String = "",
    val parts: String = "4",
    val cookedWeight: String = "",
    val lines: List<IngredientLine> = emptyList(),
) {
    val partsValue: Double? get() = parts.toDecimal()?.takeIf { it > 0 }
    val cookedValue: Double? get() = cookedWeight.toDecimal()?.takeIf { it > 0 }
    val totals: RecipeTotals? get() = partsValue?.let { RecipeMath.totals(lines.mapNotNull { l -> l.contribution }, it, cookedValue) }

    val error: String?
        get() = when {
            name.isBlank() -> "Donnez un nom à la recette"
            partsValue == null -> "Nombre de parts invalide"
            lines.isEmpty() -> "Ajoutez au moins un ingrédient"
            else -> null
        }
}

class RecipeEditViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val route = handle.toRoute<RecipeEditRoute>()
    val draft = MutableStateFlow(RecipeDraft())
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val isEdit = route.recipeId != null

    init {
        route.recipeId?.let { id -> viewModelScope.launch { c.recipes.detail(id)?.let { draft.value = draftOf(it) } } }
    }

    private fun draftOf(d: RecipeDetail) = RecipeDraft(
        id = d.recipe.id,
        existing = d.recipe,
        name = d.recipe.name,
        parts = d.recipe.parts.toInput(),
        cookedWeight = d.recipe.cookedWeightG?.toInput().orEmpty(),
        lines = d.lines,
    )

    fun update(block: (RecipeDraft) -> RecipeDraft) = draft.update(block)

    /** [encoded] comes from the quantity screen: "food|<id>|<grams>" or "recipe|<id>|<parts>". */
    fun addIngredient(encoded: String) = viewModelScope.launch {
        val (kind, id, qty) = encoded.split('|').takeIf { it.size == 3 } ?: return@launch
        val quantity = qty.toDoubleOrNull() ?: return@launch
        val recipeId = draft.value.id
        if (kind == "recipe" && !c.recipes.canUseAsSub(recipeId, id)) {
            messages.tryEmit("Impossible : cette recette contient déjà la recette en cours")
            return@launch
        }
        val ingredient = IngredientEntity(
            recipeId = recipeId,
            position = draft.value.lines.size,
            foodId = id.takeIf { kind == "food" },
            subRecipeId = id.takeIf { kind == "recipe" },
            quantity = quantity,
        )
        val line = c.recipes.line(ingredient, setOf(recipeId))
        draft.update { it.copy(lines = it.lines + line) }
    }

    fun setQuantity(index: Int, quantity: Double) = viewModelScope.launch {
        val line = draft.value.lines.getOrNull(index) ?: return@launch
        val updated = c.recipes.line(line.ingredient.copy(quantity = quantity), setOf(draft.value.id))
        draft.update { d -> d.copy(lines = d.lines.mapIndexed { i, l -> if (i == index) updated else l }) }
    }

    fun remove(index: Int) = draft.update { d -> d.copy(lines = d.lines.filterIndexed { i, _ -> i != index }) }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val d = draft.value
        if (d.error != null) return@launch
        val base = d.existing ?: RecipeEntity(id = d.id, name = d.name, parts = d.partsValue!!)
        c.recipes.save(base.copy(name = d.name.trim(), parts = d.partsValue!!, cookedWeightG = d.cookedValue), d.lines.map { it.ingredient })
        onDone()
    }
}
