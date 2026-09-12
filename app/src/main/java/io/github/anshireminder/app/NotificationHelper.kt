package io.github.anshireminder.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.time.LocalDate

const val CHANNEL_ID = "pill_reminder_channel"
const val STRONG_CHANNEL_ID = "pill_reminder_strong_channel"

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
    manager.notify(1, builder.build())
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
    manager.notify(2, notification)
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
