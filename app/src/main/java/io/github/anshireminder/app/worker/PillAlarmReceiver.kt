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

                if (PillReminderPolicy.shouldSend(settings, today, alreadyTaken)) {
                    sendPillNotification(context, settings.userName, isBreakDay, today)
                }

                if (settings.pillReminderEnabled) {
                    ReminderScheduler.schedulePillReminder(
                        context,
                        settings.reminderTime,
                        settings.firstPillDate,
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
