package com.you.reelblocker.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.atomic.AtomicLong

class ShortsAccessibilityService : AccessibilityService() {
    private val matcher = FingerprintMatcher()
    private val lastActionTime = AtomicLong(0L)
    private var currentResourceIds: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        // DO NOT reassign serviceInfo here — doing so with a new AccessibilityServiceInfo()
        // clears capabilities=0 and strips CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT,
        // which makes event.source, windows, and rootInActiveWindow all return null.
        // The XML (accessibility_service_config.xml) handles all config correctly.
        val cap = serviceInfo?.capabilities ?: -1
        val flags = serviceInfo?.flags ?: -1
        Log.d(TAG, "✅ Service connected. capabilities=$cap, flags=$flags")
        ServiceStateBus.update { it.copy(isServiceRunning = true) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg != YOUTUBE_PACKAGE_NAME) return

        val root = resolveRootNode(event) ?: return

        currentResourceIds = collectResourceIds(root)
        Log.d(TAG, "📺 YouTube active — resourceIds=${currentResourceIds.size}, class=${event.className}")

        val isShorts = matcher.isShortsScreen(root, currentResourceIds)

        if (isShorts) {
            Log.d(TAG, "🎬 Reel/Shorts detected! Preparing to block.")
            val now = System.currentTimeMillis()
            val delta = now - lastActionTime.get()
            if (delta >= COOLDOWN_MS) {
                Log.d(TAG, "🚫 Blocking Reel — pressing BACK. delta=${delta}ms")
                performGlobalAction(GLOBAL_ACTION_BACK)
                lastActionTime.set(now)
                ServiceStateBus.update { it.copy(blockedTotal = it.blockedTotal + 1) }
                Log.d(TAG, "✅ Reel blocked. totalBlocked=${ServiceStateBus.state.value.blockedTotal}")
            } else {
                Log.d(TAG, "⏳ Cooldown active — skipping. remaining=${COOLDOWN_MS - delta}ms")
            }
        }
    }

    /**
     * Resolves the root AccessibilityNodeInfo using three strategies:
     * 1. Walk up from event.source — most reliable, works without window focus.
     * 2. Scan windows list for the YouTube window — works when YouTube isn't the active window.
     * 3. Fall back to rootInActiveWindow — standard but often null for YouTube.
     */
    private fun resolveRootNode(event: AccessibilityEvent): AccessibilityNodeInfo? {
        // Strategy 1: Walk up from event.source
        event.source?.let { source ->
            var node = source
            while (node.parent != null) node = node.parent
            return node
        }

        // Strategy 2: Scan windows list for YouTube
        windows?.firstOrNull { it.root?.packageName?.toString() == YOUTUBE_PACKAGE_NAME }
            ?.root?.let { return it }

        // Strategy 3: Standard fallback
        return rootInActiveWindow
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

    private fun collectResourceIds(node: AccessibilityNodeInfo?): Set<String> {
        val ids = linkedSetOf<String>()
        fun walk(current: AccessibilityNodeInfo?) {
            if (current == null) return
            current.viewIdResourceName?.let(ids::add)
            for (i in 0 until current.childCount) {
                walk(current.getChild(i))
            }
        }
        walk(node)
        return ids
    }

    companion object {
        const val TAG = "ReelBlockerService"
        const val YOUTUBE_PACKAGE_NAME = "com.google.android.youtube"
        const val COOLDOWN_MS = 3000L
    }
}
