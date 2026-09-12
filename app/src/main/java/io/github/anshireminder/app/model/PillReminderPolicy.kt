package io.github.anshireminder.app.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object PillReminderPolicy {
    /**
     * Strong reminders keep nagging while the pill stays unlogged. [repeatIndex]
     * is the number of attempts already made, so the last allowed attempt is
     * the one that sets it to [maxRepeats] - 1.
     */
    fun shouldRepeat(strongReminderEnabled: Boolean, repeatIndex: Int, maxRepeats: Int): Boolean {
        if (!strongReminderEnabled) return false
        return repeatIndex < maxRepeats
    }

    fun shouldSend(
        settings: SettingsState,
        date: LocalDate,
        alreadyTaken: Boolean,
    ): Boolean {
        if (!settings.pillReminderEnabled || alreadyTaken) return false

        val daysSinceStart = ChronoUnit.DAYS.between(settings.firstPillDate, date)
        if (daysSinceStart < 0) return false

        val cycleLength = (settings.activePills + settings.breakDays).coerceAtLeast(1)
        val positionInCycle = (daysSinceStart % cycleLength).toInt()
        val isBreakDay = positionInCycle >= settings.activePills

        return !isBreakDay || settings.placebo
    }
}
