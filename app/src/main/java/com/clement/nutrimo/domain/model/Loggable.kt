// Something that can be logged in the diary or used as an ingredient: a food or a recipe.
package com.clement.nutrimo.domain.model

import java.text.Normalizer

data class Portion(val label: String, val grams: Double)

/** A way to express a quantity of a [Loggable] (grams, parts of a recipe, or a named portion). */
data class UnitChoice(val unit: QuantityUnit, val label: String, val portion: Portion? = null)

data class Loggable(
    val foodId: String? = null,
    val recipeId: String? = null,
    val name: String,
    val subtitle: String? = null,
    /** Per 100 g; null for a recipe whose weight is unknown. */
    val per100: Nutrients?,
    /** Per part; only for recipes. */
    val perPart: Nutrients? = null,
    val portions: List<Portion> = emptyList(),
) {
    val units: List<UnitChoice>
        get() = buildList {
            if (perPart != null) add(UnitChoice(QuantityUnit.PART, "part(s)"))
            if (per100 != null) {
                add(UnitChoice(QuantityUnit.GRAM, "g"))
                portions.forEach { add(UnitChoice(QuantityUnit.PORTION, "${it.label} (${fmt(it.grams)} g)", it)) }
            }
        }

    fun nutrientsFor(quantity: Double, choice: UnitChoice): Nutrients = when (choice.unit) {
        QuantityUnit.PART -> (perPart ?: Nutrients.ZERO).scaled(quantity)
        QuantityUnit.GRAM -> (per100 ?: Nutrients.ZERO).scaled(quantity / 100)
        QuantityUnit.PORTION -> (per100 ?: Nutrients.ZERO).scaled(quantity * (choice.portion?.grams ?: 0.0) / 100)
    }
}

private fun fmt(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)

/** Lowercase without accents, so "Pâté" matches "pate". */
fun String.normalized(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").trim()
