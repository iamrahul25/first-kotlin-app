package com.nightwise.scrollguard.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nightwise.scrollguard.MainActivity
import com.nightwise.scrollguard.R
import com.nightwise.scrollguard.permissions.PermissionUtils

object LimitNotifier {
    private const val CHANNEL_ID = "daily_limits"
    private const val NOTIFICATION_ID = 42

    @SuppressLint("MissingPermission")
    fun notifyLimitReached(context: Context, title: String, message: String) {
        if (!PermissionUtils.hasNotificationPermission(context)) return
        ensureChannel(context)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo_leaf)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Daily limits", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Lets you know when you reach your daily reels or time limit."
            }
        )
    }
}
