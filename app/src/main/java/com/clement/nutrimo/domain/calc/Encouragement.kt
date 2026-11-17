// The daily encouragement: one kind message chosen from the user's real data. Celebrate weight milestones first, then
// protect a streak still open today, then be proud of goals beaten, then comfort after a hard day, else a gentle word.
// Wording is gender-neutral (no "fier / fière") and never guilt-inducing.
package com.clement.nutrimo.domain.calc

import androidx.annotation.StringRes
import com.clement.nutrimo.R
import com.clement.nutrimo.util.UiText
import com.clement.nutrimo.util.count
import com.clement.nutrimo.util.kg
import com.clement.nutrimo.util.uiText
import java.time.LocalDate
import kotlin.math.floor

data class Nudge(val title: UiText, val text: UiText)

data class EncouragementFacts(
    val today: LocalDate,
    val records: Map<LocalDate, DayRecord>,
    val targets: GoalTargets,
    /** Oldest first. */
    val weights: List<WeightPoint>,
    val goalStart: LocalDate? = null,
    val goalStartKg: Double? = null,
    val heightCm: Double? = null,
)

/** A streak is worth protecting from this many days in a row. */
private const val MIN_STREAK = 2
/** A weigh-in this recent (days) is still news. */
private const val RECENT_WEIGH_IN_DAYS = 1L
/** Days without anything logged before the "welcome back" message. */
private const val QUIET_DAYS = 2L

object Encouragement {
    fun pick(f: EncouragementFacts): Nudge =
        weightNews(f) ?: streakToKeep(f) ?: proudOfToday(f) ?: proudOfYesterday(f) ?: hardMoment(f) ?: gentle(f)

    private fun weightNews(f: EncouragementFacts): Nudge? {
        if (f.weights.size < 2) return null
        val last = f.weights.last()
        val previous = f.weights[f.weights.size - 2]
        if (last.date.isBefore(f.today.minusDays(RECENT_WEIGH_IN_DAYS)) || last.kg >= previous.kg) return null
        f.heightCm?.let { h ->
            val now = Bmi.zoneOf(Bmi.of(last.kg, h))
            if (Bmi.ZONES.indexOf(now) < Bmi.ZONES.indexOf(Bmi.zoneOf(Bmi.of(previous.kg, h)))) {
                return nudge(R.string.enc_bmi_title, uiText(R.string.enc_bmi_text, uiText(now.label)))
            }
        }
        f.goalStartKg?.let { start ->
            val lostNow = floor(start - last.kg).toInt()
            if (lostNow >= 1 && lostNow > floor(start - previous.kg).toInt()) {
                return Nudge(uiText(R.string.enc_kilo_title, lostNow), uiText(R.string.enc_kilo_text, lostNow))
            }
        }
        val sinceStart = f.weights.filter { f.goalStart == null || !it.date.isBefore(f.goalStart) }.dropLast(1)
        if (sinceStart.isNotEmpty() && last.kg < sinceStart.minOf { it.kg }) {
            return nudge(R.string.enc_low_title, uiText(R.string.enc_low_text, kg(last.kg)))
        }
        return Nudge(uiText(R.string.enc_down_title, kg(previous.kg - last.kg)), uiText(R.string.enc_down_text))
    }

    /** The longest streak still open today (not yet reached), with what is left to keep it. */
    private fun streakToKeep(f: EncouragementFacts): Nudge? {
        val today = f.records[f.today]
        val open = listOf(DailyGoal.STEPS, DailyGoal.WATER, DailyGoal.SPORT, DailyGoal.JOURNAL)
            .map { it to streak(it, f.records, f.targets, f.today).current }
            .filter { (goal, days) -> days >= MIN_STREAK && today?.achieved(goal, f.targets) != true }
            .maxByOrNull { it.second } ?: return null
        val (goal, days) = open
        val left = when (goal) {
            DailyGoal.STEPS -> uiText(R.string.enc_left_steps, number(f.targets.steps - (today?.steps ?: 0)))
            DailyGoal.WATER -> uiText(R.string.enc_left_water, number(f.targets.waterMl - (today?.waterMl ?: 0)))
            DailyGoal.JOURNAL -> uiText(R.string.enc_left_journal)
            else -> uiText(R.string.enc_left_sport, number(f.targets.sportKcal - (today?.sportKcal ?: 0.0).toInt()))
        }
        val what = when (goal) {
            DailyGoal.STEPS -> R.string.enc_streak_steps
            DailyGoal.WATER -> R.string.enc_streak_water
            DailyGoal.JOURNAL -> R.string.enc_streak_journal
            else -> R.string.enc_streak_sport
        }
        return Nudge(uiText(R.string.enc_streak_title, days), uiText(R.string.enc_streak_text, uiText(what), days, left))
    }

    /** Goals already beaten today (steps, sport, water), in that order. */
    private fun proudOfToday(f: EncouragementFacts): Nudge? {
        val d = f.records[f.today] ?: return null
        val steps = d.steps?.takeIf { d.achieved(DailyGoal.STEPS, f.targets) == true }
        if (steps != null) {
            return nudge(R.string.enc_steps_title, uiText(R.string.enc_steps_text, number(steps), number(steps - f.targets.steps)))
        }
        if (d.achieved(DailyGoal.SPORT, f.targets) == true) {
            return nudge(R.string.enc_sport_title, uiText(R.string.enc_sport_text, number(d.sportKcal.toInt())))
        }
        if (d.achieved(DailyGoal.WATER, f.targets) == true) return nudge(R.string.enc_water_title, uiText(R.string.enc_water_text))
        return null
    }

    private fun proudOfYesterday(f: EncouragementFacts): Nudge? {
        val d = f.records[f.today.minusDays(1)] ?: return null
        if (d.achieved(DailyGoal.CALORIES, f.targets) == true) {
            val eaten = d.eatenKcal ?: return null
            val budget = d.budgetKcal ?: return null
            return nudge(R.string.enc_yesterday_title, uiText(R.string.enc_yesterday_text, number(eaten.toInt()), number(budget.toInt())))
        }
        if (d.achieved(DailyGoal.SPORT, f.targets) == true) {
            return nudge(R.string.enc_yesterday_sport_title, uiText(R.string.enc_yesterday_sport_text))
        }
        return null
    }

    private fun hardMoment(f: EncouragementFacts): Nudge? {
        val quiet = (1..QUIET_DAYS).none { f.records.containsKey(f.today.minusDays(it)) } && !f.records.containsKey(f.today)
        if (quiet && f.records.isNotEmpty()) return nudge(R.string.enc_back_title, uiText(R.string.enc_back_text))
        if (f.records[f.today.minusDays(1)]?.achieved(DailyGoal.CALORIES, f.targets) == false) {
            return nudge(R.string.enc_new_page_title, uiText(R.string.enc_new_page_text))
        }
        if (f.weights.size >= 2 && f.weights.last().kg > f.weights[f.weights.size - 2].kg &&
            !f.weights.last().date.isBefore(f.today.minusDays(RECENT_WEIGH_IN_DAYS))
        ) {
            return nudge(R.string.enc_trend_title, uiText(R.string.enc_trend_text))
        }
        return null
    }

    private fun gentle(f: EncouragementFacts): Nudge = GENTLE[Math.floorMod(f.today.toEpochDay(), GENTLE.size.toLong()).toInt()]

    private val GENTLE = listOf(
        nudge(R.string.enc_gentle_1_title, uiText(R.string.enc_gentle_1_text)),
        nudge(R.string.enc_gentle_2_title, uiText(R.string.enc_gentle_2_text)),
        nudge(R.string.enc_gentle_3_title, uiText(R.string.enc_gentle_3_text)),
        nudge(R.string.enc_gentle_4_title, uiText(R.string.enc_gentle_4_text)),
        nudge(R.string.enc_gentle_5_title, uiText(R.string.enc_gentle_5_text)),
        nudge(R.string.enc_gentle_6_title, uiText(R.string.enc_gentle_6_text)),
        nudge(R.string.enc_gentle_7_title, uiText(R.string.enc_gentle_7_text)),
    )

    private fun nudge(@StringRes title: Int, text: UiText) = Nudge(uiText(title), text)

    /** Counts in messages are never negative ("encore 0 pas" rather than "-200"). */
    private fun number(n: Number): String = count(n.toLong().coerceAtLeast(0))
}
