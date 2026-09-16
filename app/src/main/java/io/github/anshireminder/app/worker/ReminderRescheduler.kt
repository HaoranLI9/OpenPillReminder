package io.github.anshireminder.app.worker

import android.content.Context
import io.github.anshireminder.app.data.SettingsRepository
import io.github.anshireminder.app.model.SettingsState
import kotlinx.coroutines.flow.first

object ReminderRescheduler {
    fun applySettings(context: Context, settings: SettingsState) {
        val appContext = context.applicationContext

        if (!settings.pillReminderEnabled) {
            ReminderScheduler.cancelPillAlarm(appContext)
            ReminderScheduler.cancelBuyingAlarm(appContext)
            return
        }

        ReminderScheduler.schedulePillReminder(
            appContext,
            settings.reminderTime,
            settings.firstPillDate,
            settings.strongReminderEnabled,
        )

        if (settings.buyingReminder) {
            ReminderScheduler.scheduleBuyingReminder(appContext, settings.buyingReminderTime)
        } else {
            ReminderScheduler.cancelBuyingAlarm(appContext)
        }
    }

    suspend fun restoreSavedSettings(context: Context) {
        val appContext = context.applicationContext
        val settings = SettingsRepository(appContext).settingsFlow.first()
        applySettings(appContext, settings)
    }
}
