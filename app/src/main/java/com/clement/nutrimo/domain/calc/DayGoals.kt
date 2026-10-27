// Daily goals (calories, weight, sport, steps, water): success per day and streaks. A day without data for a goal
// is neither a success nor a failure, so forgetting to log a meal never counts as "under the target".
package com.clement.nutrimo.domain.calc

import java.time.LocalDate
import kotlin.math.max

/** [skipsEmptyDays]: weigh-ins are not daily, so days without one don't break the weight streak. */
enum class DailyGoal(val label: String, val skipsEmptyDays: Boolean = false) {
    CALORIES("Calories"),
    WEIGHT("Poids", skipsEmptyDays = true),
    SPORT("Sport"),
    STEPS("Pas"),
    WATER("Eau"),
}

const val DEFAULT_STEP_GOAL = 10_000
const val DEFAULT_SPORT_GOAL_KCAL = 300
const val DEFAULT_WATER_GOAL_ML = 2000

data class GoalTargets(
    val steps: Int = DEFAULT_STEP_GOAL,
    val sportKcal: Int = DEFAULT_SPORT_GOAL_KCAL,
    val waterMl: Int = DEFAULT_WATER_GOAL_ML,
)

/** Everything known about one day; null fields mean "nothing recorded". */
data class DayRecord(
    val date: LocalDate,
    val eatenKcal: Double? = null,
    /** Calorie target + the burned calories that count in the budget. */
    val budgetKcal: Double? = null,
    val sportKcal: Double = 0.0,
    val steps: Long? = null,
    val waterMl: Int? = null,
    val weightKg: Double? = null,
    /** Last weigh-in before this day, to tell whether weight went down. */
    val previousWeightKg: Double? = null,
) {
    /** True / false when the day has data for [goal], null otherwise. */
    fun achieved(goal: DailyGoal, t: GoalTargets): Boolean? = when (goal) {
        DailyGoal.CALORIES -> eatenKcal?.takeIf { it > 0 }?.let { e -> budgetKcal?.let { e <= it } }
        DailyGoal.WEIGHT -> weightKg?.let { w -> previousWeightKg?.let { w <= it } }
        DailyGoal.SPORT -> sportKcal.takeIf { it > 0 }?.let { it >= t.sportKcal }
        DailyGoal.STEPS -> steps?.takeIf { it > 0 }?.let { it >= t.steps }
        DailyGoal.WATER -> waterMl?.takeIf { it > 0 }?.let { it >= t.waterMl }
    }
}

/** [current] counts back from today, [best] over the whole history (in days, or weigh-ins for the weight). */
data class Streak(val current: Int, val best: Int)

/** Today never breaks a streak: the day is still going (steps, water and sport can still change). */
fun streak(goal: DailyGoal, records: Map<LocalDate, DayRecord>, targets: GoalTargets, today: LocalDate): Streak {
    val first = records.keys.minOrNull() ?: return Streak(0, 0)
    var run = 0
    var best = 0
    generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.forEach { date ->
        when (records[date]?.achieved(goal, targets)) {
            true -> {
                run++
                best = max(best, run)
            }
            false -> if (date != today) run = 0
            null -> if (!goal.skipsEmptyDays && date != today) run = 0
        }
    }
    return Streak(run, best)
}
