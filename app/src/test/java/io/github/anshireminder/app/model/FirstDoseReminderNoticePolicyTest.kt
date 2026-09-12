package io.github.anshireminder.app.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class FirstDoseReminderNoticePolicyTest {
    private val today = LocalDate.of(2026, 9, 12)
    private val now = LocalDateTime.of(today, LocalTime.of(15, 7))

    @Test
    fun `shows when the first pill is today, its time has passed, and it is unlogged`() {
        val settings = activeSettings(firstPillDate = today, reminderTime = LocalTime.of(15, 3))

        assertTrue(FirstDoseReminderNoticePolicy.shouldShow(settings, now, alreadyTaken = false))
    }

    @Test
    fun `does not show when the first pill time is still ahead`() {
        val settings = activeSettings(firstPillDate = today, reminderTime = LocalTime.of(15, 8))

        assertFalse(FirstDoseReminderNoticePolicy.shouldShow(settings, now, alreadyTaken = false))
    }

    @Test
    fun `does not show when today's pill was manually logged`() {
        val settings = activeSettings(firstPillDate = today, reminderTime = LocalTime.of(15, 3))

        assertFalse(FirstDoseReminderNoticePolicy.shouldShow(settings, now, alreadyTaken = true))
    }

    @Test
    fun `does not show for a different first pill date or a paused schedule`() {
        val pastFirstPill = activeSettings(
            firstPillDate = today.minusDays(1),
            reminderTime = LocalTime.of(15, 3),
        )
        val paused = activeSettings(
            firstPillDate = today,
            reminderTime = LocalTime.of(15, 3),
        ).copy(pillReminderEnabled = false)

        assertFalse(FirstDoseReminderNoticePolicy.shouldShow(pastFirstPill, now, alreadyTaken = false))
        assertFalse(FirstDoseReminderNoticePolicy.shouldShow(paused, now, alreadyTaken = false))
    }

    private fun activeSettings(firstPillDate: LocalDate, reminderTime: LocalTime) = SettingsState(
        pillReminderEnabled = true,
        firstPillDate = firstPillDate,
        reminderTime = reminderTime,
    )
}
