package com.you.reelblocker.service

import android.view.accessibility.AccessibilityNodeInfo

class FingerprintMatcher {
    fun isShortsScreen(root: AccessibilityNodeInfo, resourceIds: Set<String>): Boolean {
        val textContent = buildString {
            collectText(root, this)
        }
        return matchesShortsHints(resourceIds, textContent)
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
    }
}
