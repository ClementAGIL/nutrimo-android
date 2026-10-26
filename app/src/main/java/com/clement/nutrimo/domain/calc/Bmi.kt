// Body mass index and its WHO category.
package com.clement.nutrimo.domain.calc

object Bmi {
    fun of(weightKg: Double, heightCm: Double): Double {
        val m = heightCm / 100
        return weightKg / (m * m)
    }

    fun category(bmi: Double): String = when {
        bmi < 18.5 -> "Insuffisance pondérale"
        bmi < 25 -> "Corpulence normale"
        bmi < 30 -> "Surpoids"
        bmi < 35 -> "Obésité modérée"
        bmi < 40 -> "Obésité sévère"
        else -> "Obésité morbide"
    }
}
