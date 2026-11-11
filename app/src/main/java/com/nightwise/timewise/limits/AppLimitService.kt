package com.nightwise.timewise.limits

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.limits.AppLimitRepository
import com.nightwise.timewise.data.limits.LimitLogic
import com.nightwise.timewise.data.usage.ScreenTimeRepository
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Watches which app is in front and, once an app with a limit has used up today's time,
 * covers it with [LimitReachedActivity]. Time used comes from Android's usage stats, the
 * same numbers the rest of TimeWise shows.
 */
class AppLimitService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var worker: ExecutorService
    private lateinit var limits: AppLimitRepository
    private lateinit var screenTime: ScreenTimeRepository
    private var foreground: String? = null
    private var receiverRegistered = false

    private val check = Runnable { checkForeground() }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> handler.removeCallbacks(check)
                Intent.ACTION_USER_PRESENT -> scheduleCheck(0L)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        worker = Executors.newSingleThreadExecutor()
        limits = AppLimitRepository.get(this)
        screenTime = ScreenTimeRepository(applicationContext)
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
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || !::limits.isInitialized) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in SYSTEM_UI || pkg == currentKeyboard() || pkg == foreground) return
        foreground = pkg
        scheduleCheck(0L)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        running = null
        handler.removeCallbacks(check)
        if (receiverRegistered) unregisterReceiver(screenReceiver)
        receiverRegistered = false
        if (::worker.isInitialized) worker.shutdownNow()
        super.onDestroy()
    }

    /** After extra time is earned the app comes back to the front; start timing it again. */
    private fun resume(packageName: String) {
        foreground = packageName
        scheduleCheck(RESUME_DELAY_MS)
    }

    private fun scheduleCheck(delayMs: Long) {
        handler.removeCallbacks(check)
        handler.postDelayed(check, delayMs)
    }

    private fun checkForeground() {
        val pkg = foreground ?: return
        if (pkg == packageName) return
        val state = limits.state.value
        val limit = state.limitFor(pkg) ?: return
        if (!state.enabled) return
        worker.execute {
            val now = System.currentTimeMillis()
            val today = FocusRepository.currentEpochDay(now)
            val used = screenTime.appTimeBetween(FocusRepository.startOfDayMillis(today), now, exclude = emptySet())
                .firstOrNull { it.packageName == pkg }?.millis ?: 0L
            val extra = limits.state.value.extraMinutes(pkg, today)
            handler.post {
                if (foreground != pkg) return@post
                if (LimitLogic.isExhausted(used, limit.minutes, extra)) {
                    startActivity(LimitReachedActivity.intent(this, pkg, used))
                } else {
                    scheduleCheck(LimitLogic.nextCheckDelay(LimitLogic.remainingMillis(used, limit.minutes, extra)))
                }
            }
        }
    }

    /** Keyboards open their own windows on top of the app being typed in; they don't change what's in front. */
    private fun currentKeyboard(): String? =
        Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')

    companion object {
        private const val RESUME_DELAY_MS = 2_000L
        private val SYSTEM_UI = setOf("com.android.systemui")

        @Volatile
        private var running: AppLimitService? = null

        fun onExtraGranted(packageName: String) {
            val service = running ?: return
            service.handler.post { service.resume(packageName) }
        }
    }
}
