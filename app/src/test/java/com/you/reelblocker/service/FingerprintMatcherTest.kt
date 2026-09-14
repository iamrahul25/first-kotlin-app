package com.you.reelblocker.service

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
}
