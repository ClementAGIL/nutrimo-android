// Small enums of the domain; their labels are string resources (resolved by the UI).
package com.clement.nutrimo.domain.model

import androidx.annotation.StringRes
import com.clement.nutrimo.R

enum class Sex(@StringRes val label: Int) { FEMALE(R.string.sex_female), MALE(R.string.sex_male) }

/** Multipliers of the basal metabolic rate (standard Harris-Benedict / Mifflin activity factors). */
enum class ActivityLevel(val factor: Double, @StringRes val label: Int, @StringRes val description: Int) {
    SEDENTARY(1.2, R.string.activity_sedentary, R.string.activity_sedentary_hint),
    LIGHT(1.375, R.string.activity_light, R.string.activity_light_hint),
    MODERATE(1.55, R.string.activity_moderate, R.string.activity_moderate_hint),
    HIGH(1.725, R.string.activity_high, R.string.activity_high_hint),
    VERY_HIGH(1.9, R.string.activity_very_high, R.string.activity_very_high_hint),
}

enum class Meal(@StringRes val label: Int) {
    BREAKFAST(R.string.meal_breakfast),
    LUNCH(R.string.meal_lunch),
    DINNER(R.string.meal_dinner),
    SNACK(R.string.meal_snack),
    ;

    companion object {
        /** Meal most likely being eaten at [hour] (0-23), to preselect it. */
        fun atHour(hour: Int): Meal = when (hour) {
            in 4..10 -> BREAKFAST
            in 11..14 -> LUNCH
            in 15..17 -> SNACK
            else -> DINNER
        }
    }
}

/** Food ids are "<source>:<key>", e.g. "off:3017620422003" or "custom:<uuid>" (same ids as the Nutrimo API). */
enum class FoodSource(@StringRes val label: Int) {
    CIQUAL(R.string.source_ciqual),
    OFF(R.string.source_off),
    CUSTOM(R.string.source_custom),
    ;

    private val prefix get() = "${name.lowercase()}:"

    fun id(key: String) = prefix + key

    fun owns(id: String) = id.startsWith(prefix)
}

/** How a diary quantity is expressed: grams, recipe parts, or a number of a named portion. */
enum class QuantityUnit { GRAM, PART, PORTION }

enum class GoalMode { PACE, DATE }

enum class GoalStatus(@StringRes val label: Int) { ACTIVE(R.string.goal_status_active), ACHIEVED(R.string.goal_status_achieved), ARCHIVED(R.string.goal_status_archived) }

/** Pose of a progress photo; comparisons usually pair photos of the same pose. */
enum class PhotoPose(@StringRes val label: Int) { FRONT(R.string.pose_front), SIDE(R.string.pose_side), BACK(R.string.pose_back) }
