package com.clement.nutrimo.ui.weight

import com.clement.nutrimo.data.db.MeasurementEntity
import com.clement.nutrimo.data.db.WeightEntity
import com.clement.nutrimo.domain.calc.TrendLine
import com.clement.nutrimo.ui.common.chart.ChartPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class WeightHubModelTest {
    private val first = LocalDate.of(1994, 7, 6)
    private val last = LocalDate.of(2026, 11, 16)

    @Test
    fun `a chip puts the last weigh-in at three quarters of the width`() {
        val (start, end) = presetRange(ChartPeriod.M6, first, last)
        assertEquals(last.minusMonths(6), start)
        val share = ChronoUnit.DAYS.between(start, last).toDouble() / ChronoUnit.DAYS.between(start, end)
        assertEquals(0.75, share, 0.01)
    }

    @Test
    fun `all starts at the first weigh-in`() {
        assertEquals(first, presetRange(ChartPeriod.ALL, first, last).first)
    }

    @Test
    fun `a period longer than the history starts at the first weigh-in`() {
        val recent = last.minusDays(20)
        assertEquals(recent, presetRange(ChartPeriod.Y1, recent, last).first)
    }

    @Test
    fun `a single weigh-in still shows a week`() {
        val (start, end) = presetRange(ChartPeriod.M3, last, last)
        assertEquals(last.minusDays(MIN_PRESET_DAYS), start)
        assertTrue(end.isAfter(last))
    }

    @Test
    fun `a trend that does not go down still runs a month ahead`() {
        val trend = TrendLine(last.minusDays(28), 133.0, 0.02)
        val goalEnd = last.minusDays(1)
        assertEquals(trend.origin to last.plusDays(30), trendLineDates(trend, null, goalEnd, last))
    }

    @Test
    fun `a tap selects the closest day with data within the tolerance`() {
        val weights = listOf(WeightEntity(date = last.minusDays(10), kg = 133.0), WeightEntity(date = last, kg = 134.0))
        val measurements = listOf(MeasurementEntity(date = last.minusDays(4), waist = 140.0))
        val day = nearestDay(last.minusDays(3), 2, weights, measurements, emptyList())
        assertEquals(last.minusDays(4), day?.date)
        assertEquals(false, day?.weighed)
        assertEquals(140.0, day?.measurement?.waist)
        assertNull(nearestDay(last.minusDays(7), 2, weights, measurements, emptyList()))
    }
}
