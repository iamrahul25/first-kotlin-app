package com.nightwise.scrollguard.service

import android.view.accessibility.AccessibilityNodeInfo

class FingerprintMatcher {
    fun isShortsScreen(root: AccessibilityNodeInfo, resourceIds: Set<String>): Boolean {
        val textContent = buildString {
            collectText(root, this)
        }
        return matchesShortsHints(resourceIds, textContent)
    }

    fun isInstagramReelsScreen(root: AccessibilityNodeInfo): Boolean =
        hasFullScreenViewer(root) { matchesInstagramReelsHints(it) }

    fun isSnapchatSpotlightScreen(root: AccessibilityNodeInfo): Boolean =
        hasFullScreenViewer(root) { matchesSnapchatSpotlightHints(it) }

    /** Returns the Shorts URL shown in a Chromium-based browser's address bar, or null. */
    fun browserYouTubeShortsUrl(root: AccessibilityNodeInfo, browserPackage: String): String? =
        root.findAccessibilityNodeInfosByViewId("$browserPackage:id/url_bar").firstNotNullOfOrNull { node ->
            // While the user is typing, the address bar holds their input rather than the page URL.
            node.text?.toString()?.takeIf { !node.isFocused && matchesYouTubeShortsUrl(it) }
        }

    private fun hasFullScreenViewer(
        root: AccessibilityNodeInfo,
        matches: (Set<String>) -> Boolean
    ): Boolean {
        val rootBounds = android.graphics.Rect().also(root::getBoundsInScreen)
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)

        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
            val fillsViewer = bounds.width() >= rootBounds.width() * 0.9f &&
                bounds.height() >= rootBounds.height() * 0.75f

            if (node.isVisibleToUser && fillsViewer && matches(setOfNotNull(node.viewIdResourceName))) {
                return true
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(stack::add)
            }
        }
        return false
    }

    private fun collectText(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null) return
        node.contentDescription?.let { sb.append(it).append(' ') }
        node.text?.let { sb.append(it).append(' ') }
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), sb)
        }
    }

    companion object {
        fun matchesShortsHints(resourceIds: Set<String>, treeSummary: String): Boolean {
            val normalized = treeSummary.lowercase()
            val knownShortsMarkers = listOf(
                "shorts",
                "player_view",
                "fullscreen",
                "reel",
                "video"
            )

            val idMatches = resourceIds.any { id ->
                val value = id.lowercase()
                value.contains("shorts") || value.contains("player_view") || value.contains("fullscreen")
            }

            val textMatches = knownShortsMarkers.any { marker -> normalized.contains(marker) }
            return idMatches || textMatches
        }

        fun matchesInstagramReelsHints(
            resourceIds: Set<String>,
            visibleResourceIds: Set<String> = resourceIds
        ): Boolean = matchesViewerId(resourceIds, visibleResourceIds, INSTAGRAM_REELS_VIEWER_ID)

        fun matchesSnapchatSpotlightHints(
            resourceIds: Set<String>,
            visibleResourceIds: Set<String> = resourceIds
        ): Boolean = matchesViewerId(resourceIds, visibleResourceIds, SNAPCHAT_SPOTLIGHT_VIEWER_ID)

        fun matchesYouTubeShortsUrl(url: String): Boolean = YOUTUBE_SHORTS_URL.containsMatchIn(url)

        private val YOUTUBE_SHORTS_URL =
            Regex("""(^|[/.])youtube\.com/shorts(/|\?|$)""", RegexOption.IGNORE_CASE)

        private fun matchesViewerId(
            resourceIds: Set<String>,
            visibleResourceIds: Set<String>,
            viewerId: String
        ): Boolean = resourceIds.any { id ->
            if (id !in visibleResourceIds) return@any false
            id.substringAfterLast(':').substringAfterLast('/').equals(viewerId, ignoreCase = true)
        }

        private const val INSTAGRAM_REELS_VIEWER_ID = "clips_viewer_view_pager"
        private const val SNAPCHAT_SPOTLIGHT_VIEWER_ID = "spotlight_container"
    }
}
