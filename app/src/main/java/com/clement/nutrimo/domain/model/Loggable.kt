// Something that can be logged in the diary or used as an ingredient: a food or a recipe.
package com.clement.nutrimo.domain.model

import com.clement.nutrimo.R
import com.clement.nutrimo.util.UiText
import com.clement.nutrimo.util.fmt
import com.clement.nutrimo.util.uiText
import java.text.Normalizer

data class Portion(val label: String, val grams: Double)

/** A way to express a quantity of a [Loggable] (grams, parts of a recipe, or a named portion). */
data class UnitChoice(val unit: QuantityUnit, val label: UiText, val portion: Portion? = null)

data class Loggable(
    val foodId: String? = null,
    val recipeId: String? = null,
    val name: String,
    /** E.g. "Marque · CIQUAL" or "Recette · 4 parts". */
    val subtitle: UiText? = null,
    /** Per 100 g; null for a recipe whose weight is unknown. */
    val per100: Nutrients?,
    /** Per part; only for recipes. */
    val perPart: Nutrients? = null,
    val portions: List<Portion> = emptyList(),
) {
    val units: List<UnitChoice>
        get() = buildList {
            if (perPart != null) add(UnitChoice(QuantityUnit.PART, uiText(R.string.common_parts_unit)))
            if (per100 != null) {
                add(UnitChoice(QuantityUnit.GRAM, UiText.Raw("g")))
                portions.forEach { add(UnitChoice(QuantityUnit.PORTION, UiText.Raw(portionLabel(it)), it)) }
            }
        }

    fun nutrientsFor(quantity: Double, choice: UnitChoice): Nutrients = when (choice.unit) {
        QuantityUnit.PART -> (perPart ?: Nutrients.ZERO).scaled(quantity)
        QuantityUnit.GRAM -> (per100 ?: Nutrients.ZERO).scaled(quantity / 100)
        QuantityUnit.PORTION -> (per100 ?: Nutrients.ZERO).scaled(quantity * (choice.portion?.grams ?: 0.0) / 100)
    }
}

/** "1 pot (125 g)", but just "125 g" when the label already says the weight. */
private fun portionLabel(p: Portion): String {
    val grams = fmt(p.grams)
    return if (p.label.replace(" ", "").contains("${grams}g")) p.label else "${p.label} ($grams g)"
}

/** Lowercase without accents, so "Pâté" matches "pate". */
fun String.normalized(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").trim()
