// Nutrient amounts (kcal, macros in g, micros in their catalogue unit), per 100 g or for an eaten quantity.
package com.clement.nutrimo.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Nutrients(
    val kcal: Double = 0.0,
    val fat: Double? = null,
    val satFat: Double? = null,
    val carbs: Double? = null,
    val sugars: Double? = null,
    val fiber: Double? = null,
    val protein: Double? = null,
    val salt: Double? = null,
    val micros: Map<String, Double> = emptyMap(),
) {
    fun scaled(factor: Double) = Nutrients(
        kcal = kcal * factor,
        fat = fat?.times(factor),
        satFat = satFat?.times(factor),
        carbs = carbs?.times(factor),
        sugars = sugars?.times(factor),
        fiber = fiber?.times(factor),
        protein = protein?.times(factor),
        salt = salt?.times(factor),
        micros = micros.mapValues { it.value * factor },
    )

    /** Unknown values (null) count as 0 when the other side is known, so totals stay meaningful. */
    operator fun plus(other: Nutrients) = Nutrients(
        kcal = kcal + other.kcal,
        fat = add(fat, other.fat),
        satFat = add(satFat, other.satFat),
        carbs = add(carbs, other.carbs),
        sugars = add(sugars, other.sugars),
        fiber = add(fiber, other.fiber),
        protein = add(protein, other.protein),
        salt = add(salt, other.salt),
        micros = (micros.keys + other.micros.keys).associateWith { (micros[it] ?: 0.0) + (other.micros[it] ?: 0.0) },
    )

    companion object {
        val ZERO = Nutrients()
        fun sum(items: List<Nutrients>): Nutrients = items.fold(ZERO, Nutrients::plus)
    }
}

private fun add(a: Double?, b: Double?): Double? = if (a == null && b == null) null else (a ?: 0.0) + (b ?: 0.0)
