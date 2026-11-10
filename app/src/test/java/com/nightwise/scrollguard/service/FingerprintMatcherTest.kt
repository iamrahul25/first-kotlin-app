package com.nightwise.scrollguard.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FingerprintMatcherTest {
    @Test
    fun `recognizes known Shorts markers`() {
        val hints = setOf(
            "com.google.android.youtube:id/player_view",
            "com.google.android.youtube:id/shorts_player_view"
        )

        assertTrue(
            FingerprintMatcher.matchesShortsHints(
                resourceIds = hints,
                treeSummary = "shorts player_view fullscreen video"
            )
        )
    }

    @Test
    fun `rejects unrelated screen trees`() {
        assertFalse(
            FingerprintMatcher.matchesShortsHints(
                resourceIds = emptySet(),
                treeSummary = "home feed subscriptions channel list"
            )
        )
    }

    @Test
    fun `recognizes Instagram Reels viewer`() {
        assertTrue(
            FingerprintMatcher.matchesInstagramReelsHints(
                setOf("com.instagram.android:id/clips_viewer_view_pager")
            )
        )
    }

    @Test
    fun `rejects Instagram screens without Reels viewer`() {
        assertFalse(
            FingerprintMatcher.matchesInstagramReelsHints(
                setOf("com.instagram.android:id/home_tab", "com.instagram.android:id/feed")
            )
        )
    }

    @Test
    fun `rejects an Instagram home tree with a hidden Reels viewer`() {
        assertFalse(
            FingerprintMatcher.matchesInstagramReelsHints(
                resourceIds = setOf("com.instagram.android:id/clips_viewer_view_pager"),
                visibleResourceIds = emptySet()
            )
        )
    }

    @Test
    fun `recognizes YouTube Shorts URLs shown in a browser address bar`() {
        listOf(
            "m.youtube.com/shorts/xuS8V4q0WcM",
            "https://www.youtube.com/shorts/xuS8V4q0WcM?feature=share",
            "youtube.com/shorts"
        ).forEach { assertTrue(it, FingerprintMatcher.matchesYouTubeShortsUrl(it)) }
    }

    @Test
    fun `rejects non-Shorts browser URLs`() {
        listOf(
            "m.youtube.com",
            "m.youtube.com/watch?v=xuS8V4q0WcM",
            "m.youtube.com/results?search_query=shorts",
            "notyoutube.com/shorts/abc",
            "m.youtube.com/shortsfan"
        ).forEach { assertFalse(it, FingerprintMatcher.matchesYouTubeShortsUrl(it)) }
    }

    @Test
    fun `recognizes Snapchat Spotlight viewer`() {
        assertTrue(
            FingerprintMatcher.matchesSnapchatSpotlightHints(
                setOf("com.snapchat.android:id/spotlight_container")
            )
        )
    }

    @Test
    fun `rejects Snapchat screens without Spotlight viewer`() {
        assertFalse(
            FingerprintMatcher.matchesSnapchatSpotlightHints(
                setOf(
                    "com.snapchat.android:id/ngs_spotlight_icon_container",
                    "com.snapchat.android:id/ff_item"
                )
            )
        )
    }
}
