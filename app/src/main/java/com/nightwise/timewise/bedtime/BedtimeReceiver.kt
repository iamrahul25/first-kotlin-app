package com.nightwise.timewise.bedtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nightwise.timewise.challenge.ChallengeScheduler
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.challenge.ChallengeRepository

/** Shows the bedtime reminder, then arms the next one. */
class BedtimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = FocusRepository.get(context).settings.value
        if (intent.action == BedtimeScheduler.ACTION_BEDTIME_REMINDER && settings.bedtimeEnabled) {
            BedtimeNotifier.showReminder(context, settings)
        }
        BedtimeScheduler.sync(context, settings)
    }
}

/** Alarms are cleared on reboot and drift on clock changes, so re-arm everything. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        BedtimeScheduler.sync(context)
        ChallengeRepository.get(context).state.value.active?.let { ChallengeScheduler.schedule(context, it) }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}
