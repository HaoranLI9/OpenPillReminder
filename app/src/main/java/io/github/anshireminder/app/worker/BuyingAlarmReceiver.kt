package io.github.anshireminder.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.anshireminder.app.data.SettingsRepository
import io.github.anshireminder.app.sendBuyingNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BuyingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context).settingsFlow.first()
                if (!settings.pillReminderEnabled || !settings.buyingReminder) return@launch

                val today = LocalDate.now()
                val cycleLength = (settings.activePills + settings.breakDays).coerceAtLeast(1).toLong()
                val daysSinceStart = ChronoUnit.DAYS.between(settings.firstPillDate, today)

                val daysBefore = settings.buyingReminderSchedule.ordinal.toLong()

                val isReminderDay = if (daysSinceStart + daysBefore >= 0) {
                    (daysSinceStart + daysBefore) % cycleLength == 0L
                } else false

                if (isReminderDay) {
                    sendBuyingNotification(context, settings.userName)
                }

                // reschedule for tomorrow to try again
                ReminderScheduler.scheduleBuyingReminder(context, settings.buyingReminderTime)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
