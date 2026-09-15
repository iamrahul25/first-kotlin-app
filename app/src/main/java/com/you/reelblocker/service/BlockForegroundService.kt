package com.you.reelblocker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.you.reelblocker.R

class BlockForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        val channelId = "reel_blocker_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reel Blocker",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, "reel_blocker_service")
            .setContentTitle("Reel Blocker")
            .setContentText("Monitoring YouTube Shorts and Instagram Reels")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
