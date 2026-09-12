package io.github.anshireminder.app.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PillReminderPolicyTest {
    private val firstPillDate = LocalDate.of(2026, 1, 1)

    @Test
    fun `new schedules are paused by default`() {
        val settings = SettingsState(firstPillDate = firstPillDate)

        assertFalse(PillReminderPolicy.shouldSend(settings, firstPillDate, alreadyTaken = false))
    }

    @Test
    fun `paused schedules never send reminders`() {
        val settings = activeSettings().copy(pillReminderEnabled = false)

        assertFalse(PillReminderPolicy.shouldSend(settings, firstPillDate, alreadyTaken = false))
    }

    @Test
    fun `future schedules do not send reminders before their start date`() {
        val settings = activeSettings()

        assertFalse(
            PillReminderPolicy.shouldSend(
                settings,
                firstPillDate.minusDays(1),
                alreadyTaken = false,
            )
        )
    }

    @Test
    fun `active pill days send reminders when the pill is not logged`() {
        val settings = activeSettings()

        assertTrue(PillReminderPolicy.shouldSend(settings, firstPillDate, alreadyTaken = false))
    }

    @Test
    fun `already taken pills do not send reminders`() {
        val settings = activeSettings()

        assertFalse(PillReminderPolicy.shouldSend(settings, firstPillDate, alreadyTaken = true))
    }

    @Test
    fun `strong reminders do not repeat when the option is off`() {
        assertFalse(
            PillReminderPolicy.shouldRepeat(
                strongReminderEnabled = false,
                repeatIndex = 0,
                maxRepeats = 5,
            )
        )
    }

    @Test
    fun `strong reminders repeat while attempts remain`() {
        assertTrue(
            PillReminderPolicy.shouldRepeat(
                strongReminderEnabled = true,
                repeatIndex = 0,
                maxRepeats = 5,
            )
        )
        assertTrue(
            PillReminderPolicy.shouldRepeat(
                strongReminderEnabled = true,
                repeatIndex = 4,
                maxRepeats = 5,
            )
        )
    }

    @Test
    fun `strong reminders stop nagging once the attempt limit is reached`() {
        assertFalse(
            PillReminderPolicy.shouldRepeat(
                strongReminderEnabled = true,
                repeatIndex = 5,
                maxRepeats = 5,
            )
        )
        assertFalse(
            PillReminderPolicy.shouldRepeat(
                strongReminderEnabled = true,
                repeatIndex = 9,
                maxRepeats = 5,
            )
        )
    }

    @Test
    fun `break days stay quiet unless placebo reminders are enabled`() {
        val breakDay = firstPillDate.plusDays(21)

        assertFalse(
            PillReminderPolicy.shouldSend(
                activeSettings(placebo = false),
                breakDay,
                alreadyTaken = false,
            )
        )
        assertTrue(
            PillReminderPolicy.shouldSend(
                activeSettings(placebo = true),
                breakDay,
                alreadyTaken = false,
            )
        )
    }

    private fun activeSettings(placebo: Boolean = false) = SettingsState(
        pillReminderEnabled = true,
        activePills = 21,
        breakDays = 7,
        placebo = placebo,
        firstPillDate = firstPillDate,
    )
}
