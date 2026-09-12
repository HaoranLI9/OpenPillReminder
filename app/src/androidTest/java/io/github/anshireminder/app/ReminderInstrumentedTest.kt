package io.github.anshireminder.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.anshireminder.app.data.PillLogRepository
import io.github.anshireminder.app.data.SettingsRepository
import io.github.anshireminder.app.model.PillLog
import io.github.anshireminder.app.model.SettingsState
import io.github.anshireminder.app.worker.ReminderScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Device-level checks for what unit tests cannot reach: that a reminder really
 * lands in the notification shade on Android 15 with the bedtime action
 * attached, that postponing registers a real exact alarm, and that the nag
 * chain stays inside its deadline.
 */
@RunWith(AndroidJUnit4::class)
class ReminderInstrumentedTest {

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @Before
    fun setUp() {
        createNotificationChannel(context)
        cancelPillNotifications(context)
        // Alarms survive between tests in the same run, so start clean.
        ReminderScheduler.cancelPillAlarm(context)
        shell("logcat -c")
    }

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor)
            .use { it.bufferedReader().readText() }
    }

    private fun activePillNotification(id: Int) =
        notificationManager.activeNotifications.firstOrNull { it.id == id }

    @Test
    fun strongReminderCarriesTheBedtimeAction() {
        sendPillNotification(
            context = context,
            userName = "Test",
            isBreakDay = false,
            date = LocalDate.now(),
            strong = true,
            attempt = 1,
            allowPostpone = true,
        )

        val posted = activePillNotification(100 + 1)
        assertTrue("strong reminder was not posted", posted != null)
        assertEquals(STRONG_CHANNEL_ID, posted!!.notification.channelId)
        assertEquals("bedtime action missing", 1, posted.notification.actions?.size ?: 0)
        assertEquals(
            context.getString(R.string.action_take_before_bed),
            posted.notification.actions?.first()?.title?.toString(),
        )
    }

    @Test
    fun anAlreadyPostponedReminderOffersNoFurtherPostponement() {
        sendPillNotification(
            context = context,
            userName = "Test",
            isBreakDay = false,
            date = LocalDate.now(),
            strong = true,
            attempt = 1,
            allowPostpone = false,
        )

        val posted = activePillNotification(100 + 1)
        assertTrue("reminder was not posted", posted != null)
        assertEquals(
            "the postponed reminder should not offer the action again",
            0,
            posted!!.notification.actions?.size ?: 0,
        )
    }

    @Test
    fun bedtimePostponementRegistersAnExactAlarmAtItsWindow() {
        grantExactAlarmAccess()

        val tonight = LocalDate.now()
        val start = tonight.atTime(LocalTime.of(23, 30))
        val deadline = tonight.atTime(LocalTime.of(23, 55))

        ReminderScheduler.scheduleBedtimeReminder(
            context = context,
            strongReminder = true,
            start = start,
            deadline = deadline,
        )

        val next = alarmManager.nextAlarmClock
        assertTrue("no alarm clock was registered", next != null)
        assertEquals(
            "alarm should fire at the start of the bedtime window",
            start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            next!!.triggerTime,
        )
    }

    /**
     * The nag chain has to stop at the deadline: a window shorter than the
     * repeat interval means the first attempt fires but no repeat is queued.
     *
     * The check reads the scheduler's own log rather than AlarmManager, because
     * a nag is an inexact alarm and never shows up in getNextAlarmClock().
     */
    @Test
    fun bedtimeChainDoesNotQueueANagPastItsDeadline() = runBlocking {
        grantExactAlarmAccess()

        val today = LocalDate.now()
        SettingsRepository(context).saveSettings(
            SettingsState(
                hasRequestedPermissions = true,
                pillReminderEnabled = true,
                strongReminderEnabled = true,
                activePills = 21,
                breakDays = 7,
                firstPillDate = today,
                // Kept hours away so nothing else fires during the test.
                reminderTime = LocalDateTime.now().plusHours(6).toLocalTime(),
            )
        )
        // An unlogged pill is what makes the reminder fire.
        PillLogRepository(context).saveLog(PillLog(date = today, taken = false, note = ""))

        ReminderScheduler.scheduleBedtimeReminder(
            context = context,
            strongReminder = true,
            start = LocalDateTime.now().plusSeconds(8),
            deadline = LocalDateTime.now().plusSeconds(25),
        )

        val fired = waitFor(90_000) { activePillNotification(100 + 1) != null }
        assertTrue("the bedtime reminder never fired", fired)

        // Give a would-be repeat a moment to be logged.
        Thread.sleep(2_000)
        val logs = shell("logcat -d -s AnshiReminder")
        assertTrue(
            "a nag was queued past the bedtime deadline:\n$logs",
            !logs.contains(Regex("repeat \\d+ scheduled")),
        )
    }

    private fun grantExactAlarmAccess() {
        shell("appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow")
        assertTrue(
            "SCHEDULE_EXACT_ALARM was not granted, the alarm-clock path cannot run",
            alarmManager.canScheduleExactAlarms(),
        )
    }

    private fun waitFor(timeoutMillis: Long, condition: () -> Boolean): Boolean {
        val deadlineAt = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadlineAt) {
            if (condition()) return true
            Thread.sleep(500)
        }
        return condition()
    }
}
