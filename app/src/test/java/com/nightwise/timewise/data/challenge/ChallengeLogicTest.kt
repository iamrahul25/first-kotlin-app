package com.nightwise.timewise.data.challenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeLogicTest {

    private fun result(day: Long, success: Boolean = true) =
        ChallengeResult(ChallengeType.ONE_HOUR, startAt = day, endAt = day + 1, success = success, epochDay = day)

    @Test
    fun graceDecidesSuccess() {
        assertTrue(ChallengeLogic.isSuccess(ChallengeType.ONE_HOUR, ChallengeType.ONE_HOUR.graceMillis))
        assertFalse(ChallengeLogic.isSuccess(ChallengeType.ONE_HOUR, ChallengeType.ONE_HOUR.graceMillis + 1))
    }

    @Test
    fun streakCountsBackFromToday() {
        val results = listOf(result(10), result(9), result(8), result(6))
        assertEquals(3, ChallengeLogic.currentStreak(results, today = 10))
    }

    @Test
    fun streakStaysAliveUntilTodayIsOver() {
        val results = listOf(result(9), result(8))
        assertEquals(2, ChallengeLogic.currentStreak(results, today = 10))
        assertEquals(0, ChallengeLogic.currentStreak(results, today = 11))
    }

    @Test
    fun failuresDoNotCount() {
        val results = listOf(result(10, success = false), result(9))
        assertEquals(1, ChallengeLogic.currentStreak(results, today = 10))
        assertEquals(1, ChallengeLogic.longestStreak(results))
    }

    @Test
    fun badgesFollowWinsAndLongestStreak() {
        assertEquals(emptySet<Badge>(), ChallengeLogic.earnedBadges(listOf(result(1, success = false))))
        assertEquals(setOf(Badge.FIRST_FOCUS), ChallengeLogic.earnedBadges(listOf(result(1))))

        val threeDays = listOf(result(1), result(2), result(3))
        assertEquals(setOf(Badge.FIRST_FOCUS, Badge.STREAK_3), ChallengeLogic.earnedBadges(threeDays))

        val fiveWinsBrokenStreak = listOf(result(1), result(2), result(4), result(5), result(5))
        assertEquals(setOf(Badge.FIRST_FOCUS, Badge.FIVE_SESSIONS), ChallengeLogic.earnedBadges(fiveWinsBrokenStreak))
    }
}
