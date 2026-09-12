package io.github.anshireminder.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.anshireminder.app.data.PillLogRepository
import io.github.anshireminder.app.data.SettingsRepository
import io.github.anshireminder.app.model.PillReminderPolicy
import io.github.anshireminder.app.sendPillNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class PillAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val repeatIndex = intent.getIntExtra(ReminderScheduler.EXTRA_REPEAT_INDEX, 0)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context).settingsFlow.first()

                val cycleLength = (settings.activePills + settings.breakDays).coerceAtLeast(1)
                val today = LocalDate.now()
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(settings.firstPillDate, today)
                val positionInCycle = (daysSinceStart % cycleLength).toInt()
                val isBreakDay = daysSinceStart >= 0 && positionInCycle >= settings.activePills

                val todayLog = PillLogRepository(context).pillLogsFlow.first()[today]
                val alreadyTaken = todayLog?.taken == true

                val shouldSend = PillReminderPolicy.shouldSend(settings, today, alreadyTaken)

                // Only the daily alarm schedules the next day, and doing so also
                // clears any nag left over from the previous cycle.
                if (repeatIndex == 0 && settings.pillReminderEnabled) {
                    ReminderScheduler.schedulePillReminder(
                        context,
                        settings.reminderTime,
                        settings.firstPillDate,
                        settings.strongReminderEnabled,
                    )
                }

                if (shouldSend) {
                    sendPillNotification(
                        context,
                        settings.userName,
                        isBreakDay,
                        today,
                        strong = settings.strongReminderEnabled,
                    )

                    // Strong reminders keep nagging until the pill is logged.
                    if (
                        PillReminderPolicy.shouldRepeat(
                            settings.strongReminderEnabled,
                            repeatIndex,
                            ReminderScheduler.MAX_STRONG_REPEATS,
                        )
                    ) {
                        ReminderScheduler.scheduleRepeatPillReminder(context, repeatIndex + 1)
                    }
                } else {
                    ReminderScheduler.cancelRepeatAlarm(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
