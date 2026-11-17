// Body mass index and its WHO category.
package com.clement.nutrimo.domain.calc

/** BMI from [min] (inclusive) to [max] (exclusive); null = open-ended. */
data class BmiZone(val min: Double?, val max: Double?, val label: String)

object Bmi {
    /** WHO categories, lightest first. */
    val ZONES = listOf(
        BmiZone(null, 18.5, "Insuffisance pondérale"),
        BmiZone(18.5, 25.0, "Corpulence normale"),
        BmiZone(25.0, 30.0, "Surpoids"),
        BmiZone(30.0, 35.0, "Obésité modérée"),
        BmiZone(35.0, 40.0, "Obésité sévère"),
        BmiZone(40.0, null, "Obésité morbide"),
    )

    fun of(weightKg: Double, heightCm: Double): Double {
        val m = heightCm / 100
        return weightKg / (m * m)
    }

    /** Weight giving [bmi] at [heightCm]. */
    fun weightAt(bmi: Double, heightCm: Double): Double {
        val m = heightCm / 100
        return bmi * m * m
    }

    fun zoneOf(bmi: Double): BmiZone = ZONES.first { it.max == null || bmi < it.max }

    fun category(bmi: Double): String = zoneOf(bmi).label

    /**
     * The next category down while losing weight, and the weight under which it starts at [heightCm]; null from
     * "Corpulence normale" down (going lower is not a goal).
     */
    fun nextLower(weightKg: Double, heightCm: Double): Pair<BmiZone, Double>? {
        val zone = zoneOf(of(weightKg, heightCm))
        val bound = zone.min ?: return null
        if (bound < NORMAL_MAX) return null
        return ZONES[ZONES.indexOf(zone) - 1] to weightAt(bound, heightCm)
    }

    private const val NORMAL_MAX = 25.0
}
