// Daily energy needs and the calorie target for a weight-loss pace.
package com.clement.nutrimo.domain.calc

import com.clement.nutrimo.domain.model.ActivityLevel
import com.clement.nutrimo.domain.model.Sex
import java.time.LocalDate
import java.time.Period

/** Energy stored in 1 kg of body fat, the usual approximation. */
const val KCAL_PER_KG = 7700.0

/** Common minimum intakes below which a diet should be medically supervised. */
private val FLOOR_KCAL = mapOf(Sex.FEMALE to 1200.0, Sex.MALE to 1500.0)

/** A pace above 1 % of body weight per week is considered too fast. */
private const val MAX_SAFE_PACE_RATIO = 0.01

data class CalorieTarget(
    val bmr: Double,
    val tdee: Double,
    val deficit: Double,
    val target: Double,
    /** True when the deficit was capped by the minimum intake. */
    val floorApplied: Boolean,
    /** kg/week actually reachable with [target]. */
    val effectivePace: Double,
    val tooFast: Boolean,
)

object Energy {
    fun age(birthDate: LocalDate, today: LocalDate = LocalDate.now()): Int = Period.between(birthDate, today).years

    /** Mifflin-St Jeor basal metabolic rate in kcal/day. */
    fun bmr(sex: Sex, weightKg: Double, heightCm: Double, age: Int): Double {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * age
        return if (sex == Sex.MALE) base + 5 else base - 161
    }

    fun deficitForPace(paceKgPerWeek: Double): Double = paceKgPerWeek * KCAL_PER_KG / 7

    fun target(
        sex: Sex,
        weightKg: Double,
        heightCm: Double,
        age: Int,
        activity: ActivityLevel,
        paceKgPerWeek: Double,
    ): CalorieTarget {
        val bmr = bmr(sex, weightKg, heightCm, age)
        val tdee = bmr * activity.factor
        val wanted = tdee - deficitForPace(paceKgPerWeek)
        val floor = FLOOR_KCAL.getValue(sex)
        val target = maxOf(wanted, floor)
        val deficit = (tdee - target).coerceAtLeast(0.0)
        return CalorieTarget(
            bmr = bmr,
            tdee = tdee,
            deficit = deficit,
            target = target,
            floorApplied = wanted < floor,
            effectivePace = deficit * 7 / KCAL_PER_KG,
            tooFast = paceKgPerWeek > weightKg * MAX_SAFE_PACE_RATIO,
        )
    }
}
