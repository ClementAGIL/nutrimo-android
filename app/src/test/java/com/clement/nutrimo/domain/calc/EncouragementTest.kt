package com.clement.nutrimo.domain.calc

import com.clement.nutrimo.R
import com.clement.nutrimo.util.UiText
import com.clement.nutrimo.util.count
import com.clement.nutrimo.util.uiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Messages are compared as resource ids + arguments: the wording lives in strings.xml. */
class EncouragementTest {
    private val today = LocalDate.of(2026, 11, 17)
    private val targets = GoalTargets(steps = 10_000, sportKcal = 300, waterMl = 2000)

    private fun facts(
        records: List<DayRecord> = emptyList(),
        weights: List<WeightPoint> = emptyList(),
        goalStartKg: Double? = null,
        heightCm: Double? = null,
    ) = EncouragementFacts(today, records.associateBy { it.date }, targets, weights, today.minusDays(60), goalStartKg, heightCm)

    private fun day(daysAgo: Long) = DayRecord(today.minusDays(daysAgo))

    private fun id(t: UiText) = (t as UiText.Res).id

    @Test
    fun `moving into a lower BMI category is celebrated first`() {
        // 180 cm: BMI 40 = 129.6 kg.
        val weights = listOf(WeightPoint(today.minusDays(3), 130.0), WeightPoint(today, 129.4))
        val n = Encouragement.pick(facts(weights = weights, goalStartKg = 130.5, heightCm = 180.0))
        assertEquals(uiText(R.string.enc_bmi_title), n.title)
        assertEquals(uiText(R.string.enc_bmi_text, uiText(R.string.bmi_obesity_2)), n.text)
    }

    @Test
    fun `a new whole kilo lost since the goal start is celebrated`() {
        val weights = listOf(WeightPoint(today.minusDays(2), 133.2), WeightPoint(today.minusDays(1), 132.9))
        val n = Encouragement.pick(facts(weights = weights, goalStartKg = 134.0))
        assertEquals(uiText(R.string.enc_kilo_title, 1), n.title)
    }

    @Test
    fun `an old weigh-in is not news`() {
        val weights = listOf(WeightPoint(today.minusDays(9), 133.2), WeightPoint(today.minusDays(5), 132.0))
        val n = Encouragement.pick(facts(weights = weights, goalStartKg = 134.0))
        val weightNews = listOf(R.string.enc_kilo_title, R.string.enc_low_title, R.string.enc_down_title, R.string.enc_bmi_title)
        assertTrue(id(n.title) !in weightNews)
    }

    @Test
    fun `an open steps streak says how many steps are left`() {
        val records = listOf(day(2).copy(steps = 11_000), day(1).copy(steps = 12_000), day(0).copy(steps = 7_700))
        val n = Encouragement.pick(facts(records))
        assertEquals(uiText(R.string.enc_streak_title, 2), n.title)
        val left = uiText(R.string.enc_left_steps, count(2_300))
        assertEquals(uiText(R.string.enc_streak_text, uiText(R.string.enc_streak_steps), 2, left), n.text)
    }

    @Test
    fun `steps already beaten today make you proud`() {
        val n = Encouragement.pick(facts(listOf(day(0).copy(steps = 12_500))))
        assertEquals(uiText(R.string.enc_steps_title), n.title)
        assertEquals(uiText(R.string.enc_steps_text, count(12_500), count(2_500)), n.text)
    }

    @Test
    fun `yesterday within the calorie budget is celebrated`() {
        val records = listOf(day(1).copy(eatenKcal = 2000.0, budgetKcal = 2400.0), day(0).copy(eatenKcal = 500.0, budgetKcal = 2400.0))
        assertEquals(uiText(R.string.enc_yesterday_title), Encouragement.pick(facts(records)).title)
    }

    @Test
    fun `a day over budget gets a gentle word, not a reproach`() {
        val records = listOf(day(1).copy(eatenKcal = 3000.0, budgetKcal = 2400.0), day(0).copy(eatenKcal = 400.0, budgetKcal = 2400.0))
        assertEquals(uiText(R.string.enc_new_page_title), Encouragement.pick(facts(records)).title)
    }

    @Test
    fun `days without logging welcome you back`() {
        val n = Encouragement.pick(facts(listOf(day(5).copy(eatenKcal = 2000.0, budgetKcal = 2400.0))))
        assertEquals(uiText(R.string.enc_back_title), n.title)
    }

    @Test
    fun `weight up on the last weigh-in points to the trend`() {
        val weights = listOf(WeightPoint(today.minusDays(3), 133.0), WeightPoint(today, 133.6))
        val n = Encouragement.pick(facts(listOf(day(0).copy(eatenKcal = 300.0, budgetKcal = 2400.0)), weights))
        assertEquals(uiText(R.string.enc_trend_title), n.title)
    }

    @Test
    fun `with nothing to say there is still a kind message`() {
        val n = Encouragement.pick(facts(listOf(day(0).copy(eatenKcal = 300.0, budgetKcal = 2400.0))))
        assertTrue(n.title is UiText.Res && n.text is UiText.Res)
    }
}

class JournalGoalTest {
    private val today = LocalDate.of(2026, 11, 17)
    private val targets = GoalTargets()

    @Test
    fun `a day counts as rated with a mood, a text, or both`() {
        assertEquals(true, DayRecord(today, mood = 3).achieved(DailyGoal.JOURNAL, targets))
        assertEquals(true, DayRecord(today, journaled = true).achieved(DailyGoal.JOURNAL, targets))
        assertEquals(null, DayRecord(today).achieved(DailyGoal.JOURNAL, targets))
    }

    @Test
    fun `an open journal streak invites to rate today`() {
        val records = listOf(2L, 1L).map { DayRecord(today.minusDays(it), mood = 4) }.associateBy { it.date }
        assertEquals(2, streak(DailyGoal.JOURNAL, records, targets, today).current)
        val n = Encouragement.pick(EncouragementFacts(today, records, targets, emptyList()))
        assertEquals(uiText(R.string.enc_streak_text, uiText(R.string.enc_streak_journal), 2, uiText(R.string.enc_left_journal)), n.text)
    }
}

class TrendAndBmiTest {
    private val last = LocalDate.of(2026, 11, 16)

    @Test
    fun `trend uses the last 30 days`() {
        val points = listOf(140, 60, 28, 27, 0).map { WeightPoint(last.minusDays(it.toLong()), 130.0) }
        assertEquals(3, Trend.recent(points).size)
    }

    @Test
    fun `too few recent weigh-ins borrow older ones within 90 days`() {
        val points = listOf(120, 80, 40, 0).map { WeightPoint(last.minusDays(it.toLong()), 130.0) }
        assertEquals(listOf(80L, 40L, 0L), Trend.recent(points).map { ChronoUnit.DAYS.between(it.date, last) })
    }

    @Test
    fun `next BMI category down and its weight`() {
        val (zone, kg) = Bmi.nextLower(134.0, 180.0)!!
        assertEquals(R.string.bmi_obesity_2, zone.label)
        assertEquals(129.6, kg, 0.01)
    }

    @Test
    fun `no next category from normal weight`() {
        assertEquals(null, Bmi.nextLower(70.0, 180.0))
    }
}
