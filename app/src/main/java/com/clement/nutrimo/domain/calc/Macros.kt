// Macro targets: share of the calorie target given to protein, carbs and fat, converted to grams.
package com.clement.nutrimo.domain.calc

import com.clement.nutrimo.R

private const val KCAL_PER_G_PROTEIN = 4.0
private const val KCAL_PER_G_CARBS = 4.0
private const val KCAL_PER_G_FAT = 9.0

/** Percentages of the daily kcal; valid when they add up to 100. */
data class MacroSplit(val proteinPct: Int, val carbsPct: Int, val fatPct: Int) {
    val total: Int get() = proteinPct + carbsPct + fatPct
    val isValid: Boolean get() = total == 100 && listOf(proteinPct, carbsPct, fatPct).all { it in 5..80 }

    fun grams(kcal: Double) = MacroGrams(
        protein = kcal * proteinPct / 100 / KCAL_PER_G_PROTEIN,
        carbs = kcal * carbsPct / 100 / KCAL_PER_G_CARBS,
        fat = kcal * fatPct / 100 / KCAL_PER_G_FAT,
    )

    companion object {
        val BALANCED = MacroSplit(25, 45, 30)
    }
}

data class MacroGrams(val protein: Double, val carbs: Double, val fat: Double)

/** Common splits offered as one-tap presets. */
/** Label (string resource) to split. */
val MACRO_PRESETS: List<Pair<Int, MacroSplit>> = listOf(
    R.string.macro_preset_balanced to MacroSplit.BALANCED,
    R.string.macro_preset_high_protein to MacroSplit(35, 35, 30),
    R.string.macro_preset_low_carb to MacroSplit(30, 20, 50),
)
