package com.nightwise.timewise.bedtime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.FocusSettings
import java.util.Calendar

object BedtimeScheduler {

    const val ACTION_BEDTIME_REMINDER = "com.nightwise.timewise.action.BEDTIME_REMINDER"
    private const val REQUEST_CODE = 7001

    /** Arms (or cancels) the next reminder to match the saved settings. */
    fun sync(context: Context, settings: FocusSettings = FocusRepository.get(context).settings.value) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = reminderIntent(context)
        alarms.cancel(pending)
        if (!settings.bedtimeEnabled) return
        // Inexact on purpose: exact alarms need a special permission, and a reminder a few
        // minutes late is fine.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextReminderAt(settings), pending)
    }

    fun nextReminderAt(settings: FocusSettings, now: Long = System.currentTimeMillis()): Long {
        val reminderMinutes = Math.floorMod(settings.bedtimeMinutes - settings.reminderLeadMinutes, 24 * 60)
        val next = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, reminderMinutes / 60)
            set(Calendar.MINUTE, reminderMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (next.timeInMillis <= now) next.add(Calendar.DAY_OF_YEAR, 1)
        return next.timeInMillis
    }

    private fun reminderIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, BedtimeReceiver::class.java).setAction(ACTION_BEDTIME_REMINDER),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
