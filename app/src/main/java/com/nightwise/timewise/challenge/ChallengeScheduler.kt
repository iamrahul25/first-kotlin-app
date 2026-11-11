package com.nightwise.timewise.challenge

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nightwise.timewise.data.challenge.ActiveChallenge
import com.nightwise.timewise.data.challenge.ChallengeOutcome
import com.nightwise.timewise.data.challenge.ChallengeRepository
import com.nightwise.timewise.notifications.Channel
import com.nightwise.timewise.notifications.Notifier
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.formatLimitMinutes
import kotlin.concurrent.thread

object ChallengeScheduler {

    private const val ACTION_CHALLENGE_END = "com.nightwise.timewise.action.CHALLENGE_END"
    private const val REQUEST_CODE = 7002
    private const val NOTIFICATION_ID = 44
    private const val COUNTDOWN_NOTIFICATION_ID = 45

    /** Sets the end-of-challenge alarm and shows the time left in the notification shade. */
    fun schedule(context: Context, active: ActiveChallenge) {
        context.getSystemService(AlarmManager::class.java)
            ?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, active.endAt, endIntent(context))
        if (active.endAt > System.currentTimeMillis()) {
            Notifier.postCountdown(
                context,
                Channel.CHALLENGES,
                COUNTDOWN_NOTIFICATION_ID,
                title = "${active.type.label} in progress",
                message = "Stay off your phone. Up to ${formatLimitMinutes(active.type.graceMinutes)} in other apps is OK.",
                endAt = active.endAt
            )
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(endIntent(context))
        Notifier.cancel(context, COUNTDOWN_NOTIFICATION_ID)
    }

    fun notifyOutcome(context: Context, outcome: ChallengeOutcome) {
        Notifier.cancel(context, COUNTDOWN_NOTIFICATION_ID)
        val type = outcome.result.type
        val title = if (outcome.result.success) "You did it! ${type.label} complete" else "${type.label} ended early"
        val details = if (outcome.result.success) {
            "Only ${formatDuration(outcome.progress.distractionMillis)} in other apps. Great focus."
        } else {
            "You spent ${formatDuration(outcome.progress.distractionMillis)} in other apps. Try again when you're ready."
        }
        val badges = if (outcome.newBadges.isEmpty()) "" else
            " New badge: ${outcome.newBadges.joinToString { it.title }}."
        Notifier.post(context, Channel.CHALLENGES, NOTIFICATION_ID, title, details + badges, NotificationCompat.CATEGORY_STATUS)
    }

    private fun endIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ChallengeReceiver::class.java).setAction(ACTION_CHALLENGE_END),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

/** Scores the challenge when its time is up, even if the app isn't open. */
class ChallengeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        thread {
            try {
                ChallengeRepository.get(appContext).evaluate()?.let { ChallengeScheduler.notifyOutcome(appContext, it) }
            } finally {
                pending.finish()
            }
        }
    }
}
