// Nutrition of a recipe from its ingredients, per part and per 100 g.
package com.clement.nutrimo.domain.calc

import com.clement.nutrimo.domain.model.Nutrients

data class RecipeTotals(
    val total: Nutrients,
    /** Sum of the ingredient weights. */
    val rawWeightG: Double,
    val parts: Double,
    val cookedWeightG: Double?,
) {
    /** Cooking changes the weight (water loss), so the cooked weight wins when known. */
    val weightG: Double get() = cookedWeightG ?: rawWeightG
    val perPart: Nutrients get() = total.scaled(1 / parts.coerceAtLeast(1e-6))
    val per100: Nutrients? get() = if (weightG > 0) total.scaled(100 / weightG) else null
    val weightPerPart: Double get() = weightG / parts.coerceAtLeast(1e-6)
}

/** One resolved ingredient: what it brings and how much it weighs. */
data class IngredientContribution(val nutrients: Nutrients, val weightG: Double)

object RecipeMath {
    fun totals(items: List<IngredientContribution>, parts: Double, cookedWeightG: Double?) = RecipeTotals(
        total = Nutrients.sum(items.map { it.nutrients }),
        rawWeightG = items.sumOf { it.weightG },
        parts = parts,
        cookedWeightG = cookedWeightG,
    )

    fun ofFood(per100: Nutrients, grams: Double) = IngredientContribution(per100.scaled(grams / 100), grams)

    fun ofSubRecipe(sub: RecipeTotals, parts: Double) =
        IngredientContribution(sub.perPart.scaled(parts), sub.weightPerPart * parts)
}
