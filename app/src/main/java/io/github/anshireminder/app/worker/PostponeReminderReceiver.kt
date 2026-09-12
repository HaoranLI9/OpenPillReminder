package io.github.anshireminder.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.anshireminder.app.data.SettingsRepository
import io.github.anshireminder.app.model.ReminderTiming
import io.github.anshireminder.app.notifyReminderPostponed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Handles the "take before bed" action on a pill reminder.
 *
 * The configured time can fall at a moment the user cannot take the pill, so
 * this pushes the rest of today's reminder to bedtime. The daily schedule is
 * left alone, meaning tomorrow keeps its usual time.
 */
class PostponeReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context).settingsFlow.first()

                ReminderScheduler.scheduleBedtimeReminder(context, settings.strongReminderEnabled)
                notifyReminderPostponed(context, ReminderTiming.BEDTIME_START)

                Log.i(TAG, "reminder postponed to bedtime ${ReminderTiming.BEDTIME_START}")
            } catch (e: Exception) {
                Log.e(TAG, "failed to postpone reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AnshiReminder"
    }
}
