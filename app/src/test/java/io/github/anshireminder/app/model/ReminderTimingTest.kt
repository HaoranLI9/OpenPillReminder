package io.github.anshireminder.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

class ReminderTimingTest {
    private val date = LocalDate.of(2026, 9, 12)

    @Test
    fun `bedtime starts at 23_30 and ends at 23_55`() {
        assertEquals(LocalTime.of(23, 30), ReminderTiming.bedtimeStart(date).toLocalTime())
        assertEquals(LocalTime.of(23, 55), ReminderTiming.bedtimeDeadline(date).toLocalTime())
    }

    @Test
    fun `the bedtime window is wide enough for five five minute repeats`() {
        // Five extra attempts, one every five minutes, have to land inside the
        // window so a postponed dose never runs into the next day.
        val window = Duration.between(
            ReminderTiming.bedtimeStart(date),
            ReminderTiming.bedtimeDeadline(date),
        )

        assertEquals(Duration.ofMinutes(25), window)
    }

    @Test
    fun `an attempt exactly at the deadline still fits`() {
        assertTrue(
            ReminderTiming.fitsBefore(
                nextAttempt = ReminderTiming.bedtimeDeadline(date),
                deadline = ReminderTiming.bedtimeDeadline(date),
            )
        )
    }

    @Test
    fun `an attempt past the deadline does not fit`() {
        val deadline = ReminderTiming.bedtimeDeadline(date)

        assertFalse(
            ReminderTiming.fitsBefore(nextAttempt = deadline.plusMinutes(5), deadline = deadline)
        )
        assertFalse(
            ReminderTiming.fitsBefore(nextAttempt = deadline.plusSeconds(1), deadline = deadline)
        )
    }

    @Test
    fun `unbounded chains ignore the deadline check`() {
        assertTrue(
            ReminderTiming.fitsBefore(
                nextAttempt = ReminderTiming.bedtimeDeadline(date).plusHours(6),
                deadline = null,
            )
        )
    }
}
