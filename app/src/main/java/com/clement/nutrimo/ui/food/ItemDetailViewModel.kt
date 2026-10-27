// Quantity screen state: logs a food/recipe in the diary, edits an entry, or picks a recipe ingredient.
package com.clement.nutrimo.ui.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.db.DiaryEntity
import com.clement.nutrimo.domain.model.FoodSource
import com.clement.nutrimo.domain.model.Loggable
import com.clement.nutrimo.domain.model.Meal
import com.clement.nutrimo.domain.model.Nutrients
import com.clement.nutrimo.domain.model.QuantityUnit
import com.clement.nutrimo.domain.model.UnitChoice
import com.clement.nutrimo.ui.common.toDecimal
import com.clement.nutrimo.ui.common.toInput
import com.clement.nutrimo.ui.navigation.ItemDetailRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val DEFAULT_GRAMS = "100"

data class ItemDetailState(
    val item: Loggable? = null,
    /** Set when editing an existing diary entry. */
    val entry: DiaryEntity? = null,
    val units: List<UnitChoice> = emptyList(),
    val unit: UnitChoice? = null,
    val quantity: String = "",
    val meal: Meal = Meal.LUNCH,
    val favorite: Boolean = false,
    val isCustom: Boolean = false,
) {
    val amount: Double? get() = quantity.toDecimal()?.takeIf { it > 0 }

    val preview: Nutrients?
        get() {
            val q = amount ?: return null
            entry?.let { return it.nutrients.scaled(q / it.quantity) }
            return item?.nutrientsFor(q, unit ?: return null)
        }
}

class ItemDetailViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val route = handle.toRoute<ItemDetailRoute>()
    val state = MutableStateFlow(ItemDetailState())

    init {
        viewModelScope.launch { state.value = load() }
    }

    private suspend fun load(): ItemDetailState {
        val entry = route.entryId?.let { c.diary.entry(it) }
        val foodId = entry?.foodId ?: route.foodId
        val recipeId = entry?.recipeId ?: route.recipeId
        val item = foodId?.let { c.foods.loggable(it) } ?: recipeId?.let { c.recipes.loggable(it) }
        val units = item?.let { unitsFor(it) }.orEmpty()
        val unit = units.firstOrNull { it.unit == QuantityUnit.PORTION } ?: units.firstOrNull()
        return ItemDetailState(
            item = item ?: entry?.let { Loggable(name = it.label, per100 = null) },
            entry = entry,
            units = units,
            unit = unit,
            quantity = entry?.quantity?.toInput() ?: if (unit?.unit == QuantityUnit.GRAM) DEFAULT_GRAMS else "1",
            meal = entry?.meal ?: route.meal.takeIf { it.isNotEmpty() }?.let(Meal::valueOf) ?: Meal.LUNCH,
            favorite = foodId?.let { c.foods.food(it)?.isFavorite } ?: recipeId?.let { c.recipes.recipe(it)?.isFavorite } ?: false,
            isCustom = foodId?.let(FoodSource.CUSTOM::owns) == true,
        )
    }

    /** A recipe ingredient is stored in grams (food) or parts (sub-recipe), so other units are hidden. */
    private fun unitsFor(item: Loggable): List<UnitChoice> = when {
        !route.forRecipe -> item.units
        item.recipeId != null -> item.units.filter { it.unit == QuantityUnit.PART }
        else -> item.units.filter { it.unit != QuantityUnit.PART }
    }

    fun setQuantity(q: String) = state.update { it.copy(quantity = q) }
    fun setMeal(m: Meal) = state.update { it.copy(meal = m) }
    fun setUnit(u: UnitChoice) = state.update {
        it.copy(unit = u, quantity = if (u.unit == QuantityUnit.GRAM) DEFAULT_GRAMS else "1")
    }

    fun toggleFavorite() = viewModelScope.launch {
        val s = state.value
        val fav = !s.favorite
        s.item?.foodId?.let { c.foods.setFavorite(it, fav) }
        s.item?.recipeId?.let { c.recipes.setFavorite(it, fav) }
        state.update { it.copy(favorite = fav) }
    }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val s = state.value
        val q = s.amount ?: return@launch
        val entry = s.entry
        if (entry != null) c.diary.update(entry, q, s.meal)
        else c.diary.log(LocalDate.ofEpochDay(route.epochDay), s.meal, s.item ?: return@launch, q, s.unit ?: return@launch)
        onDone()
    }

    /** Encodes the picked ingredient for the recipe editor (see NavResults.INGREDIENT). */
    fun ingredientResult(): String? {
        val s = state.value
        val q = s.amount ?: return null
        val item = s.item ?: return null
        item.recipeId?.let { return "recipe|$it|$q" }
        val grams = if (s.unit?.unit == QuantityUnit.PORTION) q * (s.unit.portion?.grams ?: 0.0) else q
        return "food|${item.foodId}|$grams"
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        state.value.entry?.let { c.diary.delete(it.id) }
        onDone()
    }
}
