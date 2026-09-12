package io.github.anshireminder.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.github.anshireminder.app.worker.PostponeReminderReceiver
import io.github.anshireminder.app.worker.ReminderScheduler
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

const val CHANNEL_ID = "pill_reminder_channel"
const val STRONG_CHANNEL_ID = "pill_reminder_strong_channel"

private const val PILL_NOTIFICATION_ID = 1
private const val BUYING_NOTIFICATION_ID = 2
private const val POSTPONED_NOTIFICATION_ID = 3

// Strong attempts get their own ids so each one alerts again instead of quietly
// updating the previous notification.
private const val STRONG_NOTIFICATION_ID_BASE = 100

private const val POSTPONE_REQUEST_CODE = 20

private val bedtimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun createNotificationChannel(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel = NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.channel_name),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = context.getString(R.string.channel_description)
    }

    // A channel's importance and sound are frozen once it exists, so a stronger
    // reminder needs its own channel rather than an upgrade of the existing one.
    val strongChannel = NotificationChannel(
        STRONG_CHANNEL_ID,
        context.getString(R.string.channel_name_strong),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = context.getString(R.string.channel_description_strong)
        enableVibration(true)
        setBypassDnd(true)
    }

    manager.createNotificationChannels(listOf(channel, strongChannel))
}

/**
 * Full-screen intents are only pre-granted to calling and alarm apps. Every
 * other app has to ask the user, and silently falls back to a normal
 * heads-up notification until they agree.
 */
fun canUseFullScreenIntent(context: Context): Boolean {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return manager.canUseFullScreenIntent()
}

/** Do Not Disturb access is what actually makes bypassDnd take effect. */
fun hasDoNotDisturbAccess(context: Context): Boolean {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return manager.isNotificationPolicyAccessGranted
}

fun sendPillNotification(
    context: Context,
    userName: String,
    isBreakDay: Boolean,
    date: LocalDate,
    strong: Boolean = false,
    attempt: Int = 1,
    allowPostpone: Boolean = true,
) {
    val title = context.getString(
        if (isBreakDay) R.string.notif_placebo_title else R.string.notif_pill_title
    )

    val contentIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP // Don't restart the app if it's open
        putExtra("OPEN_LOG_DATE", date.toString())
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val namePart = getLocalizedNamePart(context, userName)

    val message = context.getString(
        if (isBreakDay) R.string.notif_placebo_msg else R.string.notif_pill_msg,
        namePart
    )

    val builder = NotificationCompat.Builder(
        context,
        if (strong) STRONG_CHANNEL_ID else CHANNEL_ID
    )
        .setSmallIcon(R.drawable.ic_stat_name)
        .setContentTitle(title)
        .setContentText(message)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    // A reminder that is already the postponed one offers nothing to postpone.
    if (allowPostpone) {
        builder.addAction(
            0,
            context.getString(R.string.action_take_before_bed),
            postponePendingIntent(context)
        )
    }

    if (strong) {
        builder
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (canUseFullScreenIntent(context)) {
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                1,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }
    } else {
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
    }

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (strong) {
        val notificationId = STRONG_NOTIFICATION_ID_BASE + attempt
        // Keep only the latest nag in the shade, but with a fresh id so the
        // system treats it as a new alert.
        if (attempt > 1) {
            manager.cancel(STRONG_NOTIFICATION_ID_BASE + attempt - 1)
        }
        manager.notify(notificationId, builder.build())
    } else {
        manager.notify(PILL_NOTIFICATION_ID, builder.build())
    }
}

private fun postponePendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, PostponeReminderReceiver::class.java)
    return PendingIntent.getBroadcast(
        context,
        POSTPONE_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

/** Replaces the reminder with a quiet confirmation of the new time. */
fun notifyReminderPostponed(context: Context, bedtime: LocalTime) {
    cancelPillNotifications(context)

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_name)
        .setContentTitle(context.getString(R.string.notif_postponed_title))
        .setContentText(
            context.getString(R.string.notif_postponed_msg, bedtime.format(bedtimeFormatter))
        )
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setSilent(true)
        .setAutoCancel(true)
        .build()

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(POSTPONED_NOTIFICATION_ID, notification)
}

fun cancelPillNotifications(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.cancel(PILL_NOTIFICATION_ID)
    manager.cancel(POSTPONED_NOTIFICATION_ID)
    for (attempt in 1..ReminderScheduler.MAX_STRONG_REPEATS + 1) {
        manager.cancel(STRONG_NOTIFICATION_ID_BASE + attempt)
    }
}

fun sendBuyingNotification(context: Context, userName: String) {
    val namePart = getLocalizedNamePart(context, userName)

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_name)
        .setContentTitle(context.getString(R.string.notif_buy_title))
        .setContentText(context.getString(R.string.notif_buy_msg, namePart))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(BUYING_NOTIFICATION_ID, notification)
}

fun getLocalizedNamePart(context: Context, userName: String): String {
    val trimmedName = userName.trim()
    if (trimmedName.isEmpty()) return ""

    // get the current language code ("en", "es", "ja"...)
    val currentLanguage = context.resources.configuration.locales[0].language

    return if (currentLanguage == "ja") {
        "${trimmedName}さん"
    } else {
        ", $trimmedName"
    }
}
