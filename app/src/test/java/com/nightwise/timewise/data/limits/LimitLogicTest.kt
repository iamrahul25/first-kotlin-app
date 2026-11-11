package com.nightwise.timewise.data.limits

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitLogicTest {

    private val minute = 60_000L

    @Test
    fun extraTimeAddsToTheLimit() {
        assertFalse(LimitLogic.isExhausted(44 * minute, limitMinutes = 45, extraMinutes = 0))
        assertTrue(LimitLogic.isExhausted(45 * minute, limitMinutes = 45, extraMinutes = 0))
        assertFalse(LimitLogic.isExhausted(45 * minute, limitMinutes = 45, extraMinutes = 15))
        assertEquals(15 * minute, LimitLogic.remainingMillis(45 * minute, limitMinutes = 45, extraMinutes = 15))
        assertEquals(0L, LimitLogic.remainingMillis(90 * minute, limitMinutes = 45, extraMinutes = 15))
    }

    @Test
    fun checksLandWhenTimeRunsOutWithinBounds() {
        assertEquals(5_000L, LimitLogic.nextCheckDelay(0L))
        assertEquals(12_000L, LimitLogic.nextCheckDelay(12_000L))
        assertEquals(30_000L, LimitLogic.nextCheckDelay(20 * minute))
    }

    @Test
    fun warnsInTheLastFiveMinutesOnly() {
        assertFalse(LimitLogic.shouldWarn(6 * minute))
        assertTrue(LimitLogic.shouldWarn(5 * minute))
        assertTrue(LimitLogic.shouldWarn(1_000L))
        assertFalse(LimitLogic.shouldWarn(0L))
    }

    @Test
    fun stepsAreFinerUnderAnHour() {
        assertEquals(35, LimitLogic.stepUp(30))
        assertEquals(75, LimitLogic.stepUp(60))
        assertEquals(55, LimitLogic.stepDown(60))
        assertEquals(60, LimitLogic.stepDown(75))
        assertEquals(LimitLogic.MIN_LIMIT_MINUTES, LimitLogic.stepDown(LimitLogic.MIN_LIMIT_MINUTES))
        assertEquals(LimitLogic.MAX_LIMIT_MINUTES, LimitLogic.stepUp(LimitLogic.MAX_LIMIT_MINUTES))
    }

    @Test
    fun extrasOnlyCountOnTheDayTheyWereEarned() {
        val state = LimitsState(extrasDay = 100, extras = mapOf("app" to 15))
        assertEquals(15, state.extraMinutes("app", 100))
        assertEquals(0, state.extraMinutes("app", 101))
        assertEquals(0, state.extraMinutes("other", 100))
    }
}
