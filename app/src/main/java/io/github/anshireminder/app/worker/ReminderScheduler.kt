package io.github.anshireminder.app.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.anshireminder.app.MainActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {

    private const val TAG = "AnshiReminder"

    const val EXTRA_REPEAT_INDEX = "REPEAT_INDEX"

    private const val ALARM_REQUEST_CODE = 1001
    private const val BUYING_ALARM_REQUEST_CODE = 1002
    private const val REPEAT_ALARM_REQUEST_CODE = 1003
    private const val SHOW_INTENT_REQUEST_CODE = 1004

    /** How long a strong reminder waits before nagging again. */
    private const val REPEAT_INTERVAL_MINUTES = 5L

    /** Strong reminders stop nagging after this many extra attempts. */
    const val MAX_STRONG_REPEATS = 5

    fun schedulePillReminder(
        context: Context,
        reminderTime: LocalTime,
        firstPillDate: LocalDate,
        strongReminder: Boolean = false,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = LocalDateTime.now()
        val nextDate = if (firstPillDate.isAfter(now.toLocalDate())) {
            firstPillDate
        } else {
            now.toLocalDate()
        }
        var next = nextDate.atTime(reminderTime)

        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }

        val triggerTimeMillis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Deliberately does not touch a pending nag: this also runs whenever the
        // settings are written, and cancelling here would silently kill a nag
        // that is already under way.
        setAlarm(
            context = context,
            alarmManager = alarmManager,
            triggerTimeMillis = triggerTimeMillis,
            pendingIntent = pillAlarmPendingIntent(context, repeatIndex = 0),
            asAlarmClock = strongReminder,
        )
    }

    /**
     * Strong reminders re-fire while the pill is still unlogged. Each repeat
     * carries its index so the receiver knows how many attempts are left.
     */
    fun scheduleRepeatPillReminder(context: Context, repeatIndex: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTimeMillis = LocalDateTime.now()
            .plusMinutes(REPEAT_INTERVAL_MINUTES)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        setAlarm(
            context = context,
            alarmManager = alarmManager,
            triggerTimeMillis = triggerTimeMillis,
            pendingIntent = pillAlarmPendingIntent(context, repeatIndex),
            asAlarmClock = false,
        )
        Log.i(TAG, "repeat $repeatIndex scheduled for $triggerTimeMillis")
    }

    private fun setAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerTimeMillis: Long,
        pendingIntent: PendingIntent,
        asAlarmClock: Boolean,
    ) {
        try {
            if (asAlarmClock && alarmManager.canScheduleExactAlarms()) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    SHOW_INTENT_REQUEST_CODE,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                // Registered as an alarm clock: the system keeps it visible and
                // will leave low-power modes to deliver it on time.
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTimeMillis, showIntent),
                    pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Exact alarms need "Alarms & reminders" access. Rather than drop
            // the reminder entirely, fall back to an inexact alarm.
            Log.w(TAG, "exact alarm denied, falling back to inexact", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
        }
    }

    private fun pillAlarmPendingIntent(context: Context, repeatIndex: Int): PendingIntent {
        val intent = Intent(context, PillAlarmReceiver::class.java).apply {
            putExtra(EXTRA_REPEAT_INDEX, repeatIndex)
        }
        return PendingIntent.getBroadcast(
            context,
            if (repeatIndex == 0) ALARM_REQUEST_CODE else REPEAT_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancelPillAlarm(context: Context) {
        cancelRepeatAlarm(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pillAlarmPendingIntent(context, repeatIndex = 0))
    }

    fun cancelRepeatAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pillAlarmPendingIntent(context, repeatIndex = 1)
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleBuyingReminder(context: Context, time: LocalTime) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(time)

        if (!next.isAfter(now)) next = next.plusDays(1)

        val triggerTimeMillis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, BuyingAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BUYING_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
        } catch (e: SecurityException) {
            // TODO: handle Android 14+ case where SCHEDULE_EXACT_ALARM permission is revoked
        }
    }

    fun cancelBuyingAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, BuyingAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BUYING_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
