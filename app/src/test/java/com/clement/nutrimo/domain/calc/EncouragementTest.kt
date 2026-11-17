package com.clement.nutrimo.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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

    @Test
    fun `moving into a lower BMI category is celebrated first`() {
        // 180 cm: BMI 40 = 129.6 kg.
        val weights = listOf(WeightPoint(today.minusDays(3), 130.0), WeightPoint(today, 129.4))
        val n = Encouragement.pick(facts(weights = weights, goalStartKg = 130.5, heightCm = 180.0))
        assertTrue(n.title, n.title.startsWith("Nouvelle catégorie"))
        assertTrue(n.text, n.text.contains("Obésité sévère"))
    }

    @Test
    fun `a new whole kilo lost since the goal start is celebrated`() {
        val weights = listOf(WeightPoint(today.minusDays(2), 133.2), WeightPoint(today.minusDays(1), 132.9))
        val n = Encouragement.pick(facts(weights = weights, goalStartKg = 134.0))
        assertEquals("1 kg de moins 🎉", n.title)
    }

    @Test
    fun `an old weigh-in is not news`() {
        val weights = listOf(WeightPoint(today.minusDays(9), 133.2), WeightPoint(today.minusDays(5), 132.0))
        val n = Encouragement.pick(facts(weights = weights, goalStartKg = 134.0))
        assertTrue(n.title, !n.title.contains("kg"))
    }

    @Test
    fun `an open steps streak says how many steps are left`() {
        val records = listOf(day(2).copy(steps = 11_000), day(1).copy(steps = 12_000), day(0).copy(steps = 7_700))
        val n = Encouragement.pick(facts(records))
        assertEquals("2 jours d'affilée 🔥", n.title)
        assertTrue(n.text, n.text.startsWith("Objectif de pas"))
        assertTrue(n.text, n.text.contains("2 300 pas") || n.text.contains("2 300 pas") || n.text.contains("2 300 pas"))
    }

    @Test
    fun `steps already beaten today make you proud`() {
        val records = listOf(day(0).copy(steps = 12_500))
        val n = Encouragement.pick(facts(records))
        assertEquals("Objectif de pas atteint 👟", n.title)
    }

    @Test
    fun `yesterday within the calorie budget is celebrated`() {
        val records = listOf(day(1).copy(eatenKcal = 2000.0, budgetKcal = 2400.0), day(0).copy(eatenKcal = 500.0, budgetKcal = 2400.0))
        val n = Encouragement.pick(facts(records))
        assertEquals("Belle journée hier ✨", n.title)
    }

    @Test
    fun `a day over budget gets a gentle word, not a reproach`() {
        val records = listOf(day(1).copy(eatenKcal = 3000.0, budgetKcal = 2400.0), day(0).copy(eatenKcal = 400.0, budgetKcal = 2400.0))
        val n = Encouragement.pick(facts(records))
        assertEquals("Une nouvelle page 🌅", n.title)
    }

    @Test
    fun `days without logging welcome you back`() {
        val n = Encouragement.pick(facts(listOf(day(5).copy(eatenKcal = 2000.0, budgetKcal = 2400.0))))
        assertEquals("Bon retour 🌱", n.title)
    }

    @Test
    fun `weight up on the last weigh-in points to the trend`() {
        val weights = listOf(WeightPoint(today.minusDays(3), 133.0), WeightPoint(today, 133.6))
        val n = Encouragement.pick(facts(listOf(day(0).copy(eatenKcal = 300.0, budgetKcal = 2400.0)), weights))
        assertEquals("Regardez la tendance 📉", n.title)
    }

    @Test
    fun `with nothing to say there is still a kind message`() {
        val n = Encouragement.pick(facts(listOf(day(0).copy(eatenKcal = 300.0, budgetKcal = 2400.0))))
        assertTrue(n.title.isNotBlank() && n.text.isNotBlank())
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
        assertTrue(n.text, n.text.startsWith("Journée notée 2 jours de suite"))
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
        assertEquals(listOf(80L, 40L, 0L), Trend.recent(points).map { java.time.temporal.ChronoUnit.DAYS.between(it.date, last) })
    }

    @Test
    fun `next BMI category down and its weight`() {
        val (zone, kg) = Bmi.nextLower(134.0, 180.0)!!
        assertEquals("Obésité sévère", zone.label)
        assertEquals(129.6, kg, 0.01)
    }

    @Test
    fun `no next category from normal weight`() {
        assertEquals(null, Bmi.nextLower(70.0, 180.0))
    }
}
