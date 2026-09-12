package io.github.anshireminder.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.anshireminder.app.data.PillLogRepository
import io.github.anshireminder.app.data.SettingsRepository
import io.github.anshireminder.app.model.PillReminderPolicy
import io.github.anshireminder.app.model.ReminderTiming
import io.github.anshireminder.app.sendPillNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class PillAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val repeatIndex = intent.getIntExtra(ReminderScheduler.EXTRA_REPEAT_INDEX, 0)
        val deadlineMillis = intent.getLongExtra(ReminderScheduler.EXTRA_DEADLINE, 0L)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context).settingsFlow.first()

                // A postponed bedtime reminder has to stop at its deadline
                // instead of nagging into the next day.
                val deadline = if (deadlineMillis > 0L) {
                    Instant.ofEpochMilli(deadlineMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                } else {
                    null
                }
                val now = LocalDateTime.now()

                if (deadline != null && now.isAfter(deadline)) {
                    Log.i(TAG, "attempt ${repeatIndex + 1} arrived past the bedtime deadline, dropping")
                    ReminderScheduler.cancelRepeatAlarm(context)
                    return@launch
                }

                val cycleLength = (settings.activePills + settings.breakDays).coerceAtLeast(1)
                val today = LocalDate.now()
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(settings.firstPillDate, today)
                val positionInCycle = (daysSinceStart % cycleLength).toInt()
                val isBreakDay = daysSinceStart >= 0 && positionInCycle >= settings.activePills

                val todayLog = PillLogRepository(context).pillLogsFlow.first()[today]
                val alreadyTaken = todayLog?.taken == true

                val shouldSend = PillReminderPolicy.shouldSend(settings, today, alreadyTaken)

                Log.i(
                    TAG,
                    "pill alarm fired attempt=${repeatIndex + 1} repeatIndex=$repeatIndex " +
                        "strong=${settings.strongReminderEnabled} enabled=${settings.pillReminderEnabled} " +
                        "taken=$alreadyTaken shouldSend=$shouldSend"
                )

                // Only the daily alarm schedules the next day. A bedtime attempt
                // shares repeatIndex 0 but must not move the daily schedule.
                if (repeatIndex == 0 && settings.pillReminderEnabled) {
                    ReminderScheduler.schedulePillReminder(
                        context,
                        settings.reminderTime,
                        settings.firstPillDate,
                        settings.strongReminderEnabled,
                    )
                }

                if (shouldSend) {
                    // Queue the next nag before posting anything, so a failure
                    // while showing the notification cannot break the chain.
                    if (
                        PillReminderPolicy.shouldRepeat(
                            settings.strongReminderEnabled,
                            repeatIndex,
                            ReminderScheduler.MAX_STRONG_REPEATS,
                        ) &&
                        ReminderTiming.fitsBefore(
                            ReminderScheduler.nextRepeatMoment(now),
                            deadline,
                        )
                    ) {
                        ReminderScheduler.scheduleRepeatPillReminder(
                            context,
                            repeatIndex + 1,
                            deadlineMillis,
                        )
                    }

                    try {
                        sendPillNotification(
                            context,
                            settings.userName,
                            isBreakDay,
                            today,
                            strong = settings.strongReminderEnabled,
                            attempt = repeatIndex + 1,
                            allowPostpone = deadlineMillis == 0L,
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "failed to post reminder notification", e)
                    }
                } else {
                    Log.i(TAG, "nothing to send, clearing any pending nag")
                    ReminderScheduler.cancelRepeatAlarm(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "pill alarm handling failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AnshiReminder"
    }
}
