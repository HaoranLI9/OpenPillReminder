package io.github.anshireminder.app.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.anshireminder.app.MainActivity
import io.github.anshireminder.app.model.ReminderTiming
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {

    private const val TAG = "AnshiReminder"

    const val EXTRA_REPEAT_INDEX = "REPEAT_INDEX"

    /**
     * Optional upper bound for a nag chain, as epoch millis. Used by the
     * bedtime postponement so it cannot run into the next day.
     */
    const val EXTRA_DEADLINE = "CHAIN_DEADLINE"

    private const val ALARM_REQUEST_CODE = 1001
    private const val BUYING_ALARM_REQUEST_CODE = 1002
    private const val REPEAT_ALARM_REQUEST_CODE = 1003
    private const val SHOW_INTENT_REQUEST_CODE = 1004
    private const val BEDTIME_ALARM_REQUEST_CODE = 1005

    /** How long a strong reminder waits before nagging again. */
    private const val REPEAT_INTERVAL_MINUTES = 5L

    /** Strong reminders stop nagging after this many extra attempts. */
    const val MAX_STRONG_REPEATS = 5

    /** When the next nag would land, used to check it against a deadline. */
    fun nextRepeatMoment(now: LocalDateTime): LocalDateTime =
        now.plusMinutes(REPEAT_INTERVAL_MINUTES)

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
    fun scheduleRepeatPillReminder(context: Context, repeatIndex: Int, deadlineMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTimeMillis = nextRepeatMoment(LocalDateTime.now())
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        setAlarm(
            context = context,
            alarmManager = alarmManager,
            triggerTimeMillis = triggerTimeMillis,
            pendingIntent = pillAlarmPendingIntent(context, repeatIndex, deadlineMillis),
            asAlarmClock = false,
        )
        Log.i(TAG, "repeat $repeatIndex scheduled for $triggerTimeMillis")
    }

    /**
     * One-off reminder for the rest of today, used when the user postpones the
     * dose to bedtime. The daily schedule is left untouched, so tomorrow keeps
     * its usual time.
     */
    fun scheduleBedtimeReminder(context: Context, strongReminder: Boolean) {
        val date = LocalDate.now()
        scheduleBedtimeReminder(
            context = context,
            strongReminder = strongReminder,
            start = ReminderTiming.bedtimeStart(date),
            deadline = ReminderTiming.bedtimeDeadline(date),
        )
    }

    /**
     * The window is a parameter so instrumented tests can exercise the chain
     * with a short span instead of waiting for the real 23:30.
     */
    fun scheduleBedtimeReminder(
        context: Context,
        strongReminder: Boolean,
        start: LocalDateTime,
        deadline: LocalDateTime,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = LocalDateTime.now()

        // Tapping the action after bedtime has already begun fires straight away.
        val trigger = if (start.isAfter(now)) start else now.plusSeconds(1)
        val triggerTimeMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val deadlineMillis = deadline.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        cancelRepeatAlarm(context)

        setAlarm(
            context = context,
            alarmManager = alarmManager,
            triggerTimeMillis = triggerTimeMillis,
            pendingIntent = bedtimePendingIntent(context, deadlineMillis),
            asAlarmClock = strongReminder,
        )
        Log.i(TAG, "bedtime reminder scheduled for $trigger")
    }

    fun cancelBedtimeAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(bedtimePendingIntent(context, deadlineMillis = 0L))
    }

    private fun setAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerTimeMillis: Long,
        pendingIntent: PendingIntent,
        asAlarmClock: Boolean,
    ) {
        if (!alarmManager.canScheduleExactAlarms()) {
            scheduleInexactFallback(alarmManager, triggerTimeMillis, pendingIntent)
            return
        }

        try {
            if (asAlarmClock) {
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
            // Access can be revoked between the check and the scheduling call.
            Log.w(TAG, "exact alarm access changed while scheduling", e)
            scheduleInexactFallback(alarmManager, triggerTimeMillis, pendingIntent)
        }
    }

    private fun scheduleInexactFallback(
        alarmManager: AlarmManager,
        triggerTimeMillis: Long,
        pendingIntent: PendingIntent,
    ) {
        // This is not exact, but unlike set() it can still wake the app while
        // the device is idle instead of waiting for the app to return.
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            pendingIntent,
        )
        Log.w(TAG, "exact alarm unavailable; scheduled allow-while-idle fallback")
    }

    private fun pillAlarmPendingIntent(
        context: Context,
        repeatIndex: Int,
        deadlineMillis: Long = 0L,
    ): PendingIntent {
        val intent = Intent(context, PillAlarmReceiver::class.java).apply {
            putExtra(EXTRA_REPEAT_INDEX, repeatIndex)
            putExtra(EXTRA_DEADLINE, deadlineMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            if (repeatIndex == 0) ALARM_REQUEST_CODE else REPEAT_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun bedtimePendingIntent(context: Context, deadlineMillis: Long): PendingIntent {
        val intent = Intent(context, PillAlarmReceiver::class.java).apply {
            putExtra(EXTRA_REPEAT_INDEX, 0)
            putExtra(EXTRA_DEADLINE, deadlineMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            BEDTIME_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancelPillAlarm(context: Context) {
        cancelRepeatAlarm(context)
        cancelBedtimeAlarm(context)

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

        setAlarm(
            context = context,
            alarmManager = alarmManager,
            triggerTimeMillis = triggerTimeMillis,
            pendingIntent = pendingIntent,
            asAlarmClock = false,
        )
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
