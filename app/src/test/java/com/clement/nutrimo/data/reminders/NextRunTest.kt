package com.clement.nutrimo.data.reminders

import com.clement.nutrimo.data.repository.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class NextRunTest {
    /** A Tuesday. */
    private val now = LocalDateTime.of(2026, 11, 17, 18, 30, 15)

    @Test
    fun `daily reminder later today runs today`() {
        assertEquals(LocalDateTime.of(2026, 11, 17, 19, 0), nextRun(now, ReminderSettings(true, 19, 0), weekly = false))
    }

    @Test
    fun `daily reminder already past runs tomorrow`() {
        assertEquals(LocalDateTime.of(2026, 11, 18, 10, 0), nextRun(now, ReminderSettings(true, 10, 0), weekly = false))
    }

    @Test
    fun `weekly reminder runs on its next day`() {
        val r = ReminderSettings(true, 8, 0, DayOfWeek.FRIDAY)
        assertEquals(LocalDateTime.of(2026, 11, 20, 8, 0), nextRun(now, r, weekly = true))
    }

    @Test
    fun `weekly reminder on today but already past waits a week`() {
        val r = ReminderSettings(true, 8, 0, DayOfWeek.TUESDAY)
        assertEquals(LocalDateTime.of(2026, 11, 24, 8, 0), nextRun(now, r, weekly = true))
    }

    @Test
    fun `every reminder kind has its own job, notification and settings key`() {
        val kinds = ReminderKind.entries
        assertEquals(kinds.size, kinds.map { it.workName }.distinct().size)
        assertEquals(kinds.size, kinds.map { it.notificationId }.distinct().size)
        assertEquals(kinds.size, kinds.map { it.key }.distinct().size)
    }
}
