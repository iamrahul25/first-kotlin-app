package com.nightwise.timewise.bedtime

import android.content.Context
import androidx.core.app.NotificationCompat
import com.nightwise.timewise.data.FocusSettings
import com.nightwise.timewise.notifications.Channel
import com.nightwise.timewise.notifications.Notifier
import com.nightwise.timewise.ui.formatMinutesOfDay

object BedtimeNotifier {
    private const val NOTIFICATION_ID = 43

    fun showReminder(context: Context, settings: FocusSettings) {
        val title = if (settings.reminderLeadMinutes > 0) {
            "Bedtime in ${settings.reminderLeadMinutes} minutes"
        } else {
            "It's bedtime"
        }
        val message = "Start winding down and put your phone away. " +
            "Bedtime ${formatMinutesOfDay(context, settings.bedtimeMinutes)}, " +
            "wake up ${formatMinutesOfDay(context, settings.wakeMinutes)}."
        Notifier.post(context, Channel.BEDTIME, NOTIFICATION_ID, title, message, NotificationCompat.CATEGORY_REMINDER)
    }
}
