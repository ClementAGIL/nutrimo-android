// Daily reference intakes of an adult, to show a day's nutrients as a % of them. Needs (fiber, vitamins, minerals) are
// ANSES's references for adults (population reference intake, or adequate intake when there is none), by sex; limits
// (salt, sodium, sugars, saturated fat) are the WHO / EU reference intakes not to exceed. Pure logic, tested.
package com.clement.nutrimo.domain.calc

import com.clement.nutrimo.domain.model.Sex

/** [amount] in the nutrient's own unit (g, mg or µg); [limit] = a maximum (more is worse), otherwise a need to reach. */
data class DailyReference(val amount: Double, val limit: Boolean = false) {
    /** Share of the reference reached by [value] (1.0 = 100 %). */
    fun ratio(value: Double): Double = value / amount
}

object DailyNeeds {
    /** Keys of the macro lines of the nutrient table (micros use their NutrientCatalog key). */
    const val FIBER = "fiber"
    const val SALT = "salt"
    const val SUGARS = "sugars"
    const val SAT_FAT = "satFat"

    /** Young adults need a little more calcium (bone growth ends around 25). */
    private const val CALCIUM_YOUNG_UNTIL = 24

    /** Same for both sexes. */
    private val COMMON = mapOf(
        FIBER to DailyReference(30.0),
        SALT to DailyReference(5.0, limit = true),
        SUGARS to DailyReference(90.0, limit = true),
        SAT_FAT to DailyReference(20.0, limit = true),
        "sodium" to DailyReference(2000.0, limit = true),
        "iron" to DailyReference(11.0),
        "potassium" to DailyReference(3500.0),
        "phosphorus" to DailyReference(700.0),
        "iodine" to DailyReference(150.0),
        "selenium" to DailyReference(70.0),
        "vitaminB9" to DailyReference(330.0),
        "vitaminB12" to DailyReference(4.0),
        "vitaminC" to DailyReference(110.0),
        "vitaminD" to DailyReference(15.0),
        "vitaminK" to DailyReference(70.0),
    )

    private val FEMALE = mapOf(
        "magnesium" to DailyReference(360.0),
        "zinc" to DailyReference(8.9),
        "vitaminA" to DailyReference(650.0),
        "vitaminB1" to DailyReference(1.2),
        "vitaminB2" to DailyReference(1.5),
        "vitaminB3" to DailyReference(14.0),
        "vitaminB5" to DailyReference(4.7),
        "vitaminB6" to DailyReference(1.5),
        "vitaminE" to DailyReference(9.9),
    )

    private val MALE = mapOf(
        "magnesium" to DailyReference(420.0),
        "zinc" to DailyReference(11.0),
        "vitaminA" to DailyReference(750.0),
        "vitaminB1" to DailyReference(1.5),
        "vitaminB2" to DailyReference(1.6),
        "vitaminB3" to DailyReference(17.0),
        "vitaminB5" to DailyReference(5.8),
        "vitaminB6" to DailyReference(1.8),
        "vitaminE" to DailyReference(10.5),
    )

    /** References by nutrient key; nutrients without one (beta-carotene, cholesterol…) are absent. */
    fun of(sex: Sex, age: Int): Map<String, DailyReference> =
        COMMON + (if (sex == Sex.FEMALE) FEMALE else MALE) +
            ("calcium" to DailyReference(if (age <= CALCIUM_YOUNG_UNTIL) 1000.0 else 950.0))
}
