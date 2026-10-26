// Create / edit a custom food: label nutrients mandatory, fiber and micros optional, own portions.
package com.clement.nutrimo.ui.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.db.FoodEntity
import com.clement.nutrimo.data.db.PortionEntity
import com.clement.nutrimo.data.db.newId
import com.clement.nutrimo.data.network.searchTextOf
import com.clement.nutrimo.domain.model.FoodSource
import com.clement.nutrimo.domain.model.Nutrients
import com.clement.nutrimo.ui.common.toDecimal
import com.clement.nutrimo.ui.common.toInput
import com.clement.nutrimo.ui.navigation.CustomFoodRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One nutrient input; [required] ones are those of the French nutrition label. */
data class NutrientField(val key: String, val label: String, val unit: String, val required: Boolean)

val MACRO_FIELDS = listOf(
    NutrientField("kcal", "Énergie", "kcal", true),
    NutrientField("fat", "Matières grasses", "g", true),
    NutrientField("satFat", "dont acides gras saturés", "g", true),
    NutrientField("carbs", "Glucides", "g", true),
    NutrientField("sugars", "dont sucres", "g", true),
    NutrientField("fiber", "Fibres", "g", false),
    NutrientField("protein", "Protéines", "g", true),
    NutrientField("salt", "Sel", "g", true),
)

data class PortionDraft(val label: String = "", val grams: String = "")

data class CustomFoodDraft(
    val id: String? = null,
    val name: String = "",
    val brand: String = "",
    val barcode: String = "",
    val macros: Map<String, String> = emptyMap(),
    val micros: Map<String, String> = emptyMap(),
    val portions: List<PortionDraft> = emptyList(),
) {
    fun macro(key: String): Double? = macros[key]?.toDecimal()

    val missing: List<String>
        get() = buildList {
            if (name.isBlank()) add("Nom")
            MACRO_FIELDS.filter { it.required && macro(it.key) == null }.forEach { add(it.label) }
        }

    fun toEntity(existing: FoodEntity?): FoodEntity {
        val per100 = Nutrients(
            kcal = macro("kcal") ?: 0.0, fat = macro("fat"), satFat = macro("satFat"), carbs = macro("carbs"),
            sugars = macro("sugars"), fiber = macro("fiber"), protein = macro("protein"), salt = macro("salt"),
            micros = micros.mapNotNull { (k, v) -> v.toDecimal()?.let { k to it } }.toMap(),
        )
        val base = existing ?: FoodEntity(id = "custom:${newId()}", source = FoodSource.CUSTOM, name = name, per100 = per100, searchText = "")
        return base.copy(
            name = name.trim(),
            brand = brand.trim().ifEmpty { null },
            barcode = barcode.trim().ifEmpty { null },
            per100 = per100,
            searchText = searchTextOf(name.trim(), brand.trim().ifEmpty { null }),
        )
    }

    fun portionEntities(foodId: String) = portions.mapNotNull { p ->
        val g = p.grams.toDecimal()?.takeIf { it > 0 } ?: return@mapNotNull null
        PortionEntity(foodId = foodId, label = p.label.ifBlank { "1 portion" }, grams = g)
    }

    companion object {
        fun from(food: FoodEntity, portions: List<PortionEntity>): CustomFoodDraft {
            val n = food.per100
            val values = mapOf("kcal" to n.kcal, "fat" to n.fat, "satFat" to n.satFat, "carbs" to n.carbs,
                "sugars" to n.sugars, "fiber" to n.fiber, "protein" to n.protein, "salt" to n.salt)
            return CustomFoodDraft(
                id = food.id, name = food.name, brand = food.brand.orEmpty(), barcode = food.barcode.orEmpty(),
                macros = values.mapNotNull { (k, v) -> v?.let { k to it.toInput() } }.toMap(),
                micros = n.micros.mapValues { it.value.toInput() },
                portions = portions.map { PortionDraft(it.label, it.grams.toInput()) },
            )
        }
    }
}

class CustomFoodViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val route = handle.toRoute<CustomFoodRoute>()
    val draft = MutableStateFlow(CustomFoodDraft(barcode = route.barcode.orEmpty()))
    val isEdit = route.foodId != null

    init {
        route.foodId?.let { id ->
            viewModelScope.launch { c.foods.food(id)?.let { draft.value = CustomFoodDraft.from(it, c.foods.portions(id)) } }
        }
    }

    fun update(block: (CustomFoodDraft) -> CustomFoodDraft) = draft.update(block)

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val d = draft.value
        if (d.missing.isNotEmpty()) return@launch
        val food = d.toEntity(d.id?.let { c.foods.food(it) })
        c.foods.saveCustom(food, d.portionEntities(food.id))
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        draft.value.id?.let { c.foods.delete(it) }
        onDone()
    }
}
