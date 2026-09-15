package com.you.reelblocker.service

import android.view.accessibility.AccessibilityNodeInfo

class FingerprintMatcher {
    fun isShortsScreen(root: AccessibilityNodeInfo, resourceIds: Set<String>): Boolean {
        val textContent = buildString {
            collectText(root, this)
        }
        return matchesShortsHints(resourceIds, textContent)
    }

    fun isInstagramReelsScreen(root: AccessibilityNodeInfo): Boolean {
        val rootBounds = android.graphics.Rect().also(root::getBoundsInScreen)
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)

        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
            val fillsViewer = bounds.width() >= rootBounds.width() * 0.9f &&
                bounds.height() >= rootBounds.height() * 0.75f

            if (node.isVisibleToUser && fillsViewer &&
                matchesInstagramReelsHints(setOfNotNull(node.viewIdResourceName))) {
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
        ): Boolean {
            return resourceIds.any { id ->
                if (id !in visibleResourceIds) return@any false
                id.substringAfterLast(':').substringAfterLast('/').equals(
                    INSTAGRAM_REELS_VIEWER_ID,
                    ignoreCase = true
                )
            }
        }

        private const val INSTAGRAM_REELS_VIEWER_ID = "clips_viewer_view_pager"
    }
}
