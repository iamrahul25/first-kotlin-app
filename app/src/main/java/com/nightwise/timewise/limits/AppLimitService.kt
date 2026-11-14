package com.nightwise.timewise.limits

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.nightwise.timewise.challenge.ChallengeOverlay
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.limits.AppLimitRepository
import com.nightwise.timewise.data.limits.LimitLogic
import com.nightwise.timewise.data.usage.AppInfoCache
import com.nightwise.timewise.data.usage.ScreenTimeRepository
import com.nightwise.timewise.notifications.Channel
import com.nightwise.timewise.notifications.Notifier
import com.nightwise.timewise.ui.formatDuration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Covers an app with [LimitReachedActivity] once it has used up today's time, and shows the
 * running focus challenge in a [ChallengeOverlay] over every app.
 *
 * Window events only say *something* changed: overlays from other apps (floating timers,
 * other blockers) send them too. The app actually in front is read from Android's usage
 * events, where only real activities appear. All state lives on [executor]'s single thread.
 */
class AppLimitService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var executor: ScheduledExecutorService
    private lateinit var limits: AppLimitRepository
    private lateinit var screenTime: ScreenTimeRepository
    private lateinit var usageStats: UsageStatsManager
    private var overlay: ChallengeOverlay? = null
    private var receiverRegistered = false

    private var foreground: String? = null
    private var lastEventAt = 0L
    private var pendingCheck: ScheduledFuture<*>? = null
    private val warned = mutableSetOf<String>()

    private val refresh = Runnable { executor.execute { refreshForeground() } }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> executor.execute { cancelCheck() }
                Intent.ACTION_USER_PRESENT -> executor.execute { refreshAndCheck() }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        executor = Executors.newSingleThreadScheduledExecutor()
        limits = AppLimitRepository.get(this)
        screenTime = ScreenTimeRepository(applicationContext)
        usageStats = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
        running = this
        overlay = ChallengeOverlay(this, executor).also { it.start() }
        executor.execute {
            lastEventAt = System.currentTimeMillis() - STARTUP_LOOKBACK_MS
            refreshAndCheck()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || !::executor.isInitialized) return
        handler.removeCallbacks(refresh)
        handler.postDelayed(refresh, EVENT_SETTLE_MS)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        running = null
        handler.removeCallbacks(refresh)
        overlay?.stop()
        overlay = null
        if (receiverRegistered) unregisterReceiver(screenReceiver)
        receiverRegistered = false
        if (::executor.isInitialized) executor.shutdownNow()
        super.onDestroy()
    }

    /** Reads new usage events; when a different app came to the front, checks it. Returns whether it did. */
    private fun refreshForeground(): Boolean {
        val now = System.currentTimeMillis()
        val events = usageStats.queryEvents(lastEventAt, now) ?: return false
        val event = UsageEvents.Event()
        var latest: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.timeStamp < lastEventAt) continue
            lastEventAt = event.timeStamp
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED && event.packageName !in SYSTEM_UI) {
                latest = event.packageName
            }
        }
        if (latest == null || latest == foreground) return false
        foreground = latest
        handler.post { overlay?.onForegroundChanged(latest) }
        check()
        return true
    }

    private fun refreshAndCheck() {
        if (!refreshForeground()) check()
    }

    private fun check() {
        cancelCheck()
        val pkg = foreground ?: return
        if (pkg == packageName) return
        val state = limits.state.value
        val limit = state.limitFor(pkg) ?: return
        if (!state.enabled) return

        val now = System.currentTimeMillis()
        val today = FocusRepository.currentEpochDay(now)
        val used = screenTime.appTimeBetween(FocusRepository.startOfDayMillis(today), now, exclude = emptySet())
            .firstOrNull { it.packageName == pkg }?.millis ?: 0L
        val extra = state.extraMinutes(pkg, today)
        val remaining = LimitLogic.remainingMillis(used, limit.minutes, extra)
        val delay = if (remaining == 0L) {
            handler.post { startActivity(LimitReachedActivity.intent(this, pkg, used)) }
            // Once the block shows, TimeWise is in front and this stops; if the launch was refused, it tries again.
            RETRY_BLOCK_MS
        } else {
            if (LimitLogic.shouldWarn(remaining)) warnOnce(pkg, today, extra, remaining)
            LimitLogic.nextCheckDelay(remaining)
        }
        pendingCheck = executor.schedule({ refreshAndCheck() }, delay, TimeUnit.MILLISECONDS)
    }

    /** One heads-up per app per allowance: again after extra time is earned. */
    private fun warnOnce(pkg: String, today: Long, extraMinutes: Int, remaining: Long) {
        if (!warned.add("$today:$pkg:$extraMinutes")) return
        val label = AppInfoCache.get(this, pkg).label
        Notifier.post(
            this,
            Channel.APP_LIMITS,
            pkg.hashCode(),
            title = "${formatDuration(remaining.coerceAtLeast(60_000L))} left on $label",
            message = "Your daily limit is almost up. Finish what you're doing and close it.",
            category = NotificationCompat.CATEGORY_REMINDER
        )
    }

    private fun cancelCheck() {
        pendingCheck?.cancel(false)
        pendingCheck = null
    }

    companion object {
        /** Usage events land a moment after the window changes. */
        private const val EVENT_SETTLE_MS = 400L
        private const val RETRY_BLOCK_MS = 3_000L
        private const val RESUME_DELAY_MS = 1_500L
        /** Finds the app already open when the service starts. */
        private const val STARTUP_LOOKBACK_MS = 6 * 60 * 60 * 1000L
        private val SYSTEM_UI = setOf("com.android.systemui")

        @Volatile
        private var running: AppLimitService? = null

        /** After extra time is earned the app comes back to the front; time it again from there. */
        fun onExtraGranted() {
            val service = running ?: return
            service.executor.schedule({ service.refreshAndCheck() }, RESUME_DELAY_MS, TimeUnit.MILLISECONDS)
        }
    }
}
