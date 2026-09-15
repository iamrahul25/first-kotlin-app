package com.you.reelblocker.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.atomic.AtomicLong

class ShortsAccessibilityService : AccessibilityService() {
    private val lastActionTime = AtomicLong(0L)

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "✅ Accessibility service connected")
        ServiceStateBus.update { it.copy(isServiceRunning = true) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName != YOUTUBE_PACKAGE_NAME && packageName != INSTAGRAM_PACKAGE_NAME) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val rootNode = rootInActiveWindow ?: return

        try {
            Log.d(TAG, "📺 $packageName active — checking for short-form video. class=${event.className}")

            val blockedContentDetected = when (packageName) {
                YOUTUBE_PACKAGE_NAME -> checkForShorts(rootNode)
                INSTAGRAM_PACKAGE_NAME -> FingerprintMatcher().isInstagramReelsScreen(rootNode)
                else -> false
            }

            if (blockedContentDetected) {
                Log.d(TAG, "🎬 Short-form video detected!")
                val now = System.currentTimeMillis()
                val delta = now - lastActionTime.get()
                if (delta >= COOLDOWN_MS) {
                    Log.d(TAG, "🚫 Blocking Reel — pressing BACK")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    lastActionTime.set(now)
                    ServiceStateBus.update {
                        it.copy(
                            blockedTotal = it.blockedTotal + 1,
                            blockedToday = it.blockedToday + 1
                        )
                    }
                    Log.d(TAG, "✅ Reel blocked. total=${ServiceStateBus.state.value.blockedTotal}, today=${ServiceStateBus.state.value.blockedToday}")
                } else {
                    Log.d(TAG, "⏳ Cooldown active — skipping. remaining=${COOLDOWN_MS - delta}ms")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during Shorts check", e)
        }
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
                        Log.d(TAG, "🎯 Matched keyword='$keyword' text=$text desc=$desc viewId=$viewId fullScreen=$isAlmostFullScreen")
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
        ServiceStateBus.update { it.copy(isServiceRunning = false) }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
        ServiceStateBus.update { it.copy(isServiceRunning = false) }
    }

    companion object {
        const val TAG = "ReelBlockerService"
        const val YOUTUBE_PACKAGE_NAME = "com.google.android.youtube"
        const val INSTAGRAM_PACKAGE_NAME = "com.instagram.android"
        const val COOLDOWN_MS = 3000L

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
