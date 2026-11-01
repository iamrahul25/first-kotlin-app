package com.you.reelblocker.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.you.reelblocker.data.FocusRepository
import com.you.reelblocker.data.FocusSettings
import com.you.reelblocker.data.Platform

class ShortsAccessibilityService : AccessibilityService() {

    private enum class BlockReason { INSTANT, DAILY_TIMER, REELS_LIMIT }

    private class WatchSession(val platform: Platform, var lastTick: Long) {
        var pendingMs: Long = 0L
    }

    private lateinit var repository: FocusRepository
    private val handler = Handler(Looper.getMainLooper())

    private var lastActionTime = 0L
    private var lastReelAt = 0L
    private var session: WatchSession? = null
    private var lastEndedPlatform: Platform? = null
    private var lastEndedAt = 0L
    private var receiverRegistered = false

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            endSession(SystemClock.elapsedRealtime())
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            onTick()
            if (session != null) handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
        repository = FocusRepository.get(this)
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
        ServiceStateBus.update { it.copy(isServiceRunning = true) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !::repository.isInitialized) return
        val packageName = event.packageName?.toString() ?: return
        val now = SystemClock.elapsedRealtime()

        val platform = Platform.fromPackage(packageName)
        if (platform == null) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                packageName != SYSTEM_UI_PACKAGE
            ) {
                endSession(now)
            }
            return
        }

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED
        ) return

        val rootNode = rootInActiveWindow ?: return
        val onShortForm = try {
            isShortFormScreen(platform, rootNode)
        } catch (e: Exception) {
            Log.e(TAG, "Error during short-form check", e)
            false
        }

        if (onShortForm) {
            handleShortForm(platform, eventType, now)
        } else {
            endSession(now)
        }
    }

    private fun handleShortForm(platform: Platform, eventType: Int, now: Long) {
        val settings = repository.settings.value
        val current = session
        val isNewSession = current == null || current.platform != platform
        val resumedRecently = lastEndedPlatform == platform && now - lastEndedAt < RESUME_GRACE_MS
        val isNewReel = (isNewSession && !resumedRecently) ||
            (!isNewSession && eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED &&
                now - lastReelAt > REEL_DEBOUNCE_MS)

        val reason = blockReason(settings, platform, isNewReel)
        if (reason != null) {
            block(reason, now)
            return
        }

        if (isNewSession) {
            endSession(now)
            session = WatchSession(platform, now)
            handler.removeCallbacks(tick)
            handler.postDelayed(tick, TICK_MS)
        } else {
            current?.let { accumulate(it, now) }
        }

        if (isNewReel) {
            repository.recordReel()
            lastReelAt = now
        }
    }

    private fun blockReason(settings: FocusSettings, platform: Platform, isNewReel: Boolean): BlockReason? {
        if (!settings.protectionEnabled || !settings.isBlocked(platform)) return null
        if (!settings.hasAnyLimit) return BlockReason.INSTANT

        val today = repository.stats.value.today
        val watchedSeconds = today.seconds + (session?.pendingMs ?: 0L) / 1000
        if (settings.dailyTimerEnabled && watchedSeconds >= settings.dailyTimerMinutes * 60L) {
            return BlockReason.DAILY_TIMER
        }
        if (settings.reelsLimitEnabled && isNewReel && today.reels >= settings.reelsLimit) {
            return BlockReason.REELS_LIMIT
        }
        return null
    }

    private fun block(reason: BlockReason, now: Long) {
        val sinceLast = now - lastActionTime
        if (sinceLast < COOLDOWN_MS) {
            Log.d(TAG, "Cooldown active, skipping. remaining=${COOLDOWN_MS - sinceLast}ms")
            return
        }
        endSession(now)
        Log.d(TAG, "Blocking short-form video ($reason), pressing BACK")
        performGlobalAction(GLOBAL_ACTION_BACK)
        lastActionTime = now
        repository.recordBlocked()

        val message = when (reason) {
            BlockReason.INSTANT -> return
            BlockReason.DAILY_TIMER -> "You've reached your daily timer for short videos."
            BlockReason.REELS_LIMIT -> "You've reached your daily reels limit."
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        if (repository.claimLimitNotification()) {
            LimitNotifier.notifyLimitReached(
                this,
                title = "Daily limit reached",
                message = "$message Short videos stay blocked until tomorrow."
            )
        }
    }

    private fun onTick() {
        val current = session ?: return
        val now = SystemClock.elapsedRealtime()
        accumulate(current, now)
        flush(current)

        val root = rootInActiveWindow
        val stillWatching = root != null &&
            root.packageName?.toString() == current.platform.packageName &&
            runCatching { isShortFormScreen(current.platform, root) }.getOrDefault(false)
        if (!stillWatching) {
            endSession(now)
            return
        }

        val reason = blockReason(repository.settings.value, current.platform, isNewReel = false)
        if (reason == BlockReason.DAILY_TIMER) block(reason, now)
    }

    private fun accumulate(session: WatchSession, now: Long) {
        session.pendingMs += (now - session.lastTick).coerceIn(0L, MAX_GAP_MS)
        session.lastTick = now
        if (session.pendingMs >= FLUSH_MS) flush(session)
    }

    private fun flush(session: WatchSession) {
        val seconds = session.pendingMs / 1000
        if (seconds > 0) {
            repository.addWatchSeconds(seconds)
            session.pendingMs -= seconds * 1000
        }
    }

    private fun endSession(now: Long) {
        val current = session ?: return
        accumulate(current, now)
        flush(current)
        session = null
        handler.removeCallbacks(tick)
        lastEndedPlatform = current.platform
        lastEndedAt = now
    }

    private fun isShortFormScreen(platform: Platform, root: AccessibilityNodeInfo): Boolean = when (platform) {
        Platform.YOUTUBE -> checkForShorts(root)
        Platform.INSTAGRAM -> FingerprintMatcher().isInstagramReelsScreen(root)
        else -> false
    }

    /** Iteratively walks the node tree looking for YouTube Shorts markers. */
    private fun checkForShorts(rootNode: AccessibilityNodeInfo): Boolean {
        val stack = mutableListOf<AccessibilityNodeInfo>()
        stack.add(rootNode)

        val rootRect = android.graphics.Rect()
        rootNode.getBoundsInScreen(rootRect)
        val screenHeight = rootRect.height()
        val screenWidth = rootRect.width()
        val rect = android.graphics.Rect()

        while (stack.isNotEmpty()) {
            val node = stack.removeAt(stack.size - 1)

            val text = node.text?.toString()
            val desc = node.contentDescription?.toString()
            val viewId = node.viewIdResourceName

            node.getBoundsInScreen(rect)
            val isAlmostFullScreen = rect.height() >= screenHeight * 0.9 && rect.width() >= screenWidth * 0.9

            for (keyword in SHORTS_KEYWORDS) {
                val matchesText = text?.contains(keyword, ignoreCase = true) == true
                val matchesDesc = desc?.contains(keyword, ignoreCase = true) == true
                val matchesId = viewId?.contains(keyword, ignoreCase = true) == true

                if (matchesText || matchesDesc || matchesId) {
                    if (isAlmostFullScreen || matchesId) {
                        return true
                    }
                }
            }

            for (i in 0 until node.childCount) {
                try {
                    node.getChild(i)?.let { stack.add(it) }
                } catch (_: Exception) { }
            }
        }
        return false
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
        endSession(SystemClock.elapsedRealtime())
    }

    override fun onDestroy() {
        endSession(SystemClock.elapsedRealtime())
        if (receiverRegistered) {
            unregisterReceiver(screenOffReceiver)
            receiverRegistered = false
        }
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
        ServiceStateBus.update { it.copy(isServiceRunning = false) }
    }

    companion object {
        const val TAG = "ReelBlockerService"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        private const val COOLDOWN_MS = 3000L
        private const val TICK_MS = 5000L
        private const val FLUSH_MS = 5000L
        private const val MAX_GAP_MS = 30_000L
        private const val REEL_DEBOUNCE_MS = 800L
        private const val RESUME_GRACE_MS = 5000L

        // YouTube Shorts-specific view IDs and keywords
        val SHORTS_KEYWORDS = listOf(
            "shorts",
            "reel_player",
            "shorts_player",
            "reel_watch_player",
            "short_video",
        )
    }
}
