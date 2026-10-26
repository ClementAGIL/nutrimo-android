// Display catalogue of vitamins and minerals; keys match the API's MICROS (nutrimo-api src/foods/nutrients.ts).
package com.clement.nutrimo.domain.model

data class MicroNutrient(val key: String, val label: String, val unit: String, val group: MicroGroup)

enum class MicroGroup(val label: String) { MINERAL("Minéraux"), VITAMIN("Vitamines"), OTHER("Autres") }

object NutrientCatalog {
    val MICROS: List<MicroNutrient> = listOf(
        MicroNutrient("calcium", "Calcium", "mg", MicroGroup.MINERAL),
        MicroNutrient("iron", "Fer", "mg", MicroGroup.MINERAL),
        MicroNutrient("magnesium", "Magnésium", "mg", MicroGroup.MINERAL),
        MicroNutrient("potassium", "Potassium", "mg", MicroGroup.MINERAL),
        MicroNutrient("phosphorus", "Phosphore", "mg", MicroGroup.MINERAL),
        MicroNutrient("sodium", "Sodium", "mg", MicroGroup.MINERAL),
        MicroNutrient("zinc", "Zinc", "mg", MicroGroup.MINERAL),
        MicroNutrient("iodine", "Iode", "µg", MicroGroup.MINERAL),
        MicroNutrient("selenium", "Sélénium", "µg", MicroGroup.MINERAL),
        MicroNutrient("vitaminA", "Vitamine A (rétinol)", "µg", MicroGroup.VITAMIN),
        MicroNutrient("betaCarotene", "Bêta-carotène", "µg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminB1", "Vitamine B1", "mg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminB2", "Vitamine B2", "mg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminB3", "Vitamine B3", "mg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminB5", "Vitamine B5", "mg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminB6", "Vitamine B6", "mg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminB9", "Vitamine B9", "µg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminB12", "Vitamine B12", "µg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminC", "Vitamine C", "mg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminD", "Vitamine D", "µg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminE", "Vitamine E", "mg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminK", "Vitamine K", "µg", MicroGroup.VITAMIN),
        MicroNutrient("cholesterol", "Cholestérol", "mg", MicroGroup.OTHER),
    )

    /** OFF keys of each micro, used to map products fetched directly from Open Food Facts. */
    val OFF_KEYS: Map<String, String> = mapOf(
        "calcium" to "calcium_100g", "iron" to "iron_100g", "magnesium" to "magnesium_100g",
        "potassium" to "potassium_100g", "phosphorus" to "phosphorus_100g", "sodium" to "sodium_100g",
        "zinc" to "zinc_100g", "iodine" to "iodine_100g", "selenium" to "selenium_100g",
        "cholesterol" to "cholesterol_100g", "vitaminA" to "vitamin-a_100g", "betaCarotene" to "beta-carotene_100g",
        "vitaminD" to "vitamin-d_100g", "vitaminE" to "vitamin-e_100g", "vitaminK" to "vitamin-k_100g",
        "vitaminC" to "vitamin-c_100g", "vitaminB1" to "vitamin-b1_100g", "vitaminB2" to "vitamin-b2_100g",
        "vitaminB3" to "vitamin-pp_100g", "vitaminB5" to "pantothenic-acid_100g", "vitaminB6" to "vitamin-b6_100g",
        "vitaminB9" to "vitamin-b9_100g", "vitaminB12" to "vitamin-b12_100g",
    )

    fun unitOf(key: String): String = MICROS.firstOrNull { it.key == key }?.unit ?: ""
}
