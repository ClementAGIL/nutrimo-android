// JSON shape of a food, shared by the Nutrimo API, the bundled CIQUAL asset and the OFF mapper.
package com.clement.nutrimo.data.network

import com.clement.nutrimo.data.db.FoodEntity
import com.clement.nutrimo.data.db.PortionEntity
import com.clement.nutrimo.domain.model.FoodSource
import com.clement.nutrimo.domain.model.Nutrients
import com.clement.nutrimo.domain.model.normalized
import kotlinx.serialization.Serializable

@Serializable
data class FoodDto(
    val id: String,
    val source: String,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val servingLabel: String? = null,
    val servingG: Double? = null,
    val kcal: Double,
    val fat: Double? = null,
    val satFat: Double? = null,
    val carbs: Double? = null,
    val sugars: Double? = null,
    val fiber: Double? = null,
    val protein: Double? = null,
    val salt: Double? = null,
    val micros: Map<String, Double> = emptyMap(),
)

@Serializable
data class SearchResponse(val items: List<FoodDto>)

fun FoodDto.toEntity() = FoodEntity(
    id = id,
    source = if (source == "ciqual") FoodSource.CIQUAL else FoodSource.OFF,
    name = name,
    brand = brand,
    barcode = barcode,
    per100 = Nutrients(kcal, fat, satFat, carbs, sugars, fiber, protein, salt, micros),
    searchText = searchTextOf(name, brand),
)

/** The product's serving ("1 pot (125 g)") as a portion, when OFF gives its weight. */
fun FoodDto.servingPortion(): PortionEntity? {
    val grams = servingG?.takeIf { it > 0 } ?: return null
    return PortionEntity(foodId = id, label = servingLabel?.take(40) ?: "1 portion", grams = grams)
}

fun searchTextOf(name: String, brand: String?) = listOfNotNull(name, brand).joinToString(" ").normalized()
