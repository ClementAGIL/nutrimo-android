// Physical activities and the energy they burn (MET values from the Compendium of Physical Activities).
package com.clement.nutrimo.domain.calc

import com.clement.nutrimo.R
import androidx.annotation.StringRes

/** [met] = energy cost relative to rest; null means the user types the kcal ("Autre"). */
data class ActivityType(val key: String, @StringRes val label: Int, val met: Double?)

const val STEPS_BASELINE = 5000L
private const val KCAL_PER_STEP_PER_KG = 0.0004

object Activities {
    val TYPES: List<ActivityType> = listOf(
        ActivityType("walk", R.string.activity_type_walk, 3.5),
        ActivityType("brisk_walk", R.string.activity_type_brisk_walk, 4.8),
        ActivityType("run", R.string.activity_type_run, 9.8),
        ActivityType("bike", R.string.activity_type_bike, 7.5),
        ActivityType("swim", R.string.activity_type_swim, 7.0),
        ActivityType("strength", R.string.activity_type_strength, 5.0),
        ActivityType("yoga", R.string.activity_type_yoga, 2.5),
        ActivityType("football", R.string.activity_type_football, 7.0),
        ActivityType("tennis", R.string.activity_type_tennis, 7.3),
        ActivityType("dance", R.string.activity_type_dance, 5.0),
        ActivityType("chores", R.string.activity_type_chores, 3.5),
        ActivityType("other", R.string.activity_type_other, null),
    )

    fun byKey(key: String): ActivityType = TYPES.firstOrNull { it.key == key } ?: TYPES.last()

    /**
     * Calories of the steps beyond [STEPS_BASELINE]: everyday walking is already in the activity level of the
     * profile, so only the extra steps raise the budget. Walking costs ~0.5 kcal per kg per km, ~1,300 steps/km.
     */
    fun stepsKcal(steps: Long, weightKg: Double): Double = (steps - STEPS_BASELINE).coerceAtLeast(0) * KCAL_PER_STEP_PER_KG * weightKg

    /** kcal = MET × body weight (kg) × duration (h). */
    fun burned(met: Double, weightKg: Double, minutes: Int): Double = met * weightKg * minutes / 60.0
}
