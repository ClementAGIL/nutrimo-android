package com.clement.nutrimo.ui.common.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GapAxisTest {
    private val today = LocalDate.of(2026, 11, 17)

    @Test
    fun `photos close together stay on one continuous axis`() {
        val axis = GapAxis(listOf(today.minusDays(120), today.minusDays(30), today))
        assertEquals(1, axis.segments.size)
        assertTrue(axis.breaks.isEmpty())
        assertEquals(120 + 2 * MARGIN_DAYS, axis.totalDays)
    }

    @Test
    fun `more than six months without photos becomes a short break`() {
        val old = LocalDate.of(2022, 5, 4)
        val recent = today.minusDays(20)
        val axis = GapAxis(listOf(old, recent, today))
        assertEquals(2, axis.segments.size)
        assertEquals(1, axis.breaks.size)
        // Each kept period is its photos ± the margin, plus the break: years shrink to a few weeks.
        assertEquals(2 * MARGIN_DAYS + BREAK_DAYS + 20 + 2 * MARGIN_DAYS, axis.totalDays)
        assertNull(axis.position(LocalDate.of(2024, 1, 1)))
        assertEquals(MARGIN_DAYS, axis.position(old))
        assertEquals(2 * MARGIN_DAYS + BREAK_DAYS + MARGIN_DAYS, axis.position(recent))
    }

    @Test
    fun `today keeps its own place after a long pause`() {
        val axis = GapAxis(listOf(today.minusDays(400), today))
        assertEquals(2, axis.segments.size)
        assertTrue(today in axis.segments.last())
    }

    @Test
    fun `a date in a collapsed stretch is placed on its break`() {
        val axis = GapAxis(listOf(LocalDate.of(2022, 5, 4), today))
        assertEquals(axis.breaks.single(), axis.place(LocalDate.of(2024, 1, 1)))
        assertEquals(0L, axis.place(LocalDate.of(2000, 1, 1)))
        assertEquals(axis.totalDays, axis.place(today.plusYears(1)))
    }

    @Test
    fun `positions grow from left to right`() {
        val dates = listOf(LocalDate.of(2022, 5, 4), LocalDate.of(2022, 6, 26), LocalDate.of(2025, 9, 20), LocalDate.of(2026, 9, 18))
        val axis = GapAxis(dates + today)
        val positions = dates.map { axis.position(it)!! }
        assertEquals(positions.sorted(), positions)
        assertTrue(positions.last() <= axis.totalDays)
    }

    @Test
    fun `dateAt reverses position and snaps a break to its closer edge`() {
        val old = LocalDate.of(2022, 5, 4)
        val axis = GapAxis(listOf(old, today))
        assertEquals(old, axis.dateAt(axis.position(old)!!))
        assertEquals(today, axis.dateAt(axis.position(today)!!))
        val breakMiddle = axis.breaks.single()
        assertEquals(axis.segments[0].endInclusive, axis.dateAt(breakMiddle - 1))
        assertEquals(axis.segments[1].start, axis.dateAt(breakMiddle + 1))
        assertEquals(axis.segments.last().endInclusive, axis.dateAt(axis.totalDays + 50))
        assertEquals(axis.segments.first().start, axis.dateAt(-5))
    }
}
