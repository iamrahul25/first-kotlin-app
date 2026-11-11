package com.nightwise.timewise.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nightwise.timewise.MainActivity
import com.nightwise.timewise.R
import com.nightwise.timewise.permissions.PermissionUtils

enum class Channel(val id: String, val title: String, val description: String) {
    BEDTIME("bedtime", "Bedtime reminders", "Reminds you when it's time to put the phone down for the night."),
    CHALLENGES("focus_challenges", "Focus challenges", "Tells you when a focus challenge ends and when you earn a badge.")
}

object Notifier {

    @SuppressLint("MissingPermission")
    fun post(context: Context, channel: Channel, id: Int, title: String, message: String, category: String) {
        if (!PermissionUtils.hasNotificationPermission(context)) return
        ensureChannel(context, channel)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setCategory(category)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
        }
    }

    /** A silent, sticky notification whose timer counts down to [endAt] by itself. */
    @SuppressLint("MissingPermission")
    fun postCountdown(context: Context, channel: Channel, id: Int, title: String, message: String, endAt: Long) {
        if (!PermissionUtils.hasNotificationPermission(context)) return
        ensureChannel(context, channel)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setWhen(endAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setTimeoutAfter((endAt - System.currentTimeMillis()).coerceAtLeast(1L))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
        }
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)

    private fun ensureChannel(context: Context, channel: Channel) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(channel.id) != null) return
        manager.createNotificationChannel(
            NotificationChannel(channel.id, channel.title, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = channel.description
            }
        )
    }
}
