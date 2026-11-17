// Display catalogue of vitamins and minerals; keys match the API's MICROS (nutrimo-api src/foods/nutrients.ts).
package com.clement.nutrimo.domain.model

import com.clement.nutrimo.R
import androidx.annotation.StringRes

data class MicroNutrient(val key: String, @StringRes val label: Int, val unit: String, val group: MicroGroup)

enum class MicroGroup(@StringRes val label: Int) { MINERAL(R.string.micro_group_minerals), VITAMIN(R.string.micro_group_vitamins), OTHER(R.string.micro_group_other) }

object NutrientCatalog {
    val MICROS: List<MicroNutrient> = listOf(
        MicroNutrient("calcium", R.string.nutrient_calcium, "mg", MicroGroup.MINERAL),
        MicroNutrient("iron", R.string.nutrient_iron, "mg", MicroGroup.MINERAL),
        MicroNutrient("magnesium", R.string.nutrient_magnesium, "mg", MicroGroup.MINERAL),
        MicroNutrient("potassium", R.string.nutrient_potassium, "mg", MicroGroup.MINERAL),
        MicroNutrient("phosphorus", R.string.nutrient_phosphorus, "mg", MicroGroup.MINERAL),
        MicroNutrient("sodium", R.string.nutrient_sodium, "mg", MicroGroup.MINERAL),
        MicroNutrient("zinc", R.string.nutrient_zinc, "mg", MicroGroup.MINERAL),
        MicroNutrient("iodine", R.string.nutrient_iodine, "µg", MicroGroup.MINERAL),
        MicroNutrient("selenium", R.string.nutrient_selenium, "µg", MicroGroup.MINERAL),
        MicroNutrient("vitaminA", R.string.nutrient_vitamin_a, "µg", MicroGroup.VITAMIN),
        MicroNutrient("betaCarotene", R.string.nutrient_beta_carotene, "µg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminB1", R.string.nutrient_vitamin_b_1, "mg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminB2", R.string.nutrient_vitamin_b_2, "mg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminB3", R.string.nutrient_vitamin_b_3, "mg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminB5", R.string.nutrient_vitamin_b_5, "mg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminB6", R.string.nutrient_vitamin_b_6, "mg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminB9", R.string.nutrient_vitamin_b_9, "µg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminB12", R.string.nutrient_vitamin_b_12, "µg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminC", R.string.nutrient_vitamin_c, "mg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminD", R.string.nutrient_vitamin_d, "µg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminE", R.string.nutrient_vitamin_e, "mg", MicroGroup.VITAMIN),
        MicroNutrient("vitaminK", R.string.nutrient_vitamin_k, "µg", MicroGroup.VITAMIN),
        MicroNutrient("cholesterol", R.string.nutrient_cholesterol, "mg", MicroGroup.OTHER),
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
