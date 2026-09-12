package io.github.anshireminder.app.model

import java.time.LocalDateTime

/**
 * Decides whether setting up a schedule has skipped today's first reminder.
 * The scheduler moves a past time to tomorrow, so the user needs a clear
 * in-app notice unless they have already logged today's pill.
 */
object FirstDoseReminderNoticePolicy {
    fun shouldShow(
        settings: SettingsState,
        now: LocalDateTime,
        alreadyTaken: Boolean,
    ): Boolean {
        return settings.pillReminderEnabled &&
            settings.firstPillDate == now.toLocalDate() &&
            !settings.reminderTime.isAfter(now.toLocalTime()) &&
            !alreadyTaken
    }
}
