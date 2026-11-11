package com.nightwise.timewise.snapshot

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.usage.ScreenTimeRepository
import java.util.Calendar
import kotlin.concurrent.thread

/**
 * Saves each finished day's summary in the background, so history keeps growing even when
 * the app isn't opened before Android prunes its usage events (about 10 days).
 */
object DailySnapshotScheduler {

    private const val ACTION_SNAPSHOT = "com.nightwise.timewise.action.DAILY_SNAPSHOT"
    private const val REQUEST_CODE = 7003
    /** Just after midnight, once the previous day is complete. */
    private const val RUN_AT_MINUTES = 20

    fun schedule(context: Context, now: Long = System.currentTimeMillis()) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val next = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, RUN_AT_MINUTES)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, snapshotIntent(context))
    }

    private fun snapshotIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, DailySnapshotReceiver::class.java).setAction(ACTION_SNAPSHOT),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

class DailySnapshotReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        thread {
            try {
                // Loading merges recent days into saved history as a side effect.
                runCatching {
                    ScreenTimeRepository(appContext).load()
                    FocusRepository.get(appContext).markBackgroundSave()
                }
            } finally {
                DailySnapshotScheduler.schedule(appContext)
                pending.finish()
            }
        }
    }
}
