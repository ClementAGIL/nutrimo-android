// Physical activities and the energy they burn (MET values from the Compendium of Physical Activities).
package com.clement.nutrimo.domain.calc

/** [met] = energy cost relative to rest; null means the user types the kcal ("Autre"). */
data class ActivityType(val key: String, val label: String, val met: Double?)

const val STEPS_BASELINE = 5000L
private const val KCAL_PER_STEP_PER_KG = 0.0004

object Activities {
    val TYPES: List<ActivityType> = listOf(
        ActivityType("walk", "Marche", 3.5),
        ActivityType("brisk_walk", "Marche rapide", 4.8),
        ActivityType("run", "Course à pied", 9.8),
        ActivityType("bike", "Vélo", 7.5),
        ActivityType("swim", "Natation", 7.0),
        ActivityType("strength", "Musculation", 5.0),
        ActivityType("yoga", "Yoga / étirements", 2.5),
        ActivityType("football", "Football", 7.0),
        ActivityType("tennis", "Tennis", 7.3),
        ActivityType("dance", "Danse", 5.0),
        ActivityType("chores", "Ménage / jardinage", 3.5),
        ActivityType("other", "Autre (kcal saisies)", null),
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
