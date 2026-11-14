package com.nightwise.timewise.data.profile

import com.nightwise.timewise.data.usage.DayUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileLogicTest {

    private val hour = 60 * 60 * 1000L
    private val today = 100L

    private fun day(epochDay: Long, hours: Double) =
        DayUsage(epochDay, (hours * hour).toLong(), emptyList(), null, List(24) { 0 })

    @Test
    fun levelsFollowDailyAverage() {
        assertEquals(UsageProfile.MONK, ProfileLogic.profileFor(2 * hour - 1))
        assertEquals(UsageProfile.MINDFUL, ProfileLogic.profileFor(2 * hour))
        assertEquals(UsageProfile.HOOKED, ProfileLogic.profileFor(5 * hour))
        assertEquals(UsageProfile.ADDICT, ProfileLogic.profileFor(7 * hour))
        assertEquals(UsageProfile.ZOMBIE, ProfileLogic.profileFor(8 * hour))
        assertEquals(UsageProfile.ZOMBIE, ProfileLogic.profileFor(20 * hour))
    }

    @Test
    fun averageUsesFinishedDaysWithData() {
        val history = listOf(day(today - 3, 4.0), day(today - 2, 0.0), day(today - 1, 6.0), day(today, 15.0))
        val summary = ProfileLogic.summarize(history, today)!!
        assertEquals(5 * hour, summary.dailyAverageMillis)
        assertEquals(2, summary.trackedDays)
        assertEquals(UsageProfile.HOOKED, summary.profile)
        assertNull(summary.previousDailyAverageMillis)
    }

    @Test
    fun comparesWithTheWeekBefore() {
        val history = (today - 14 until today).map { day(it, if (it < today - 7) 6.0 else 3.0) }
        val summary = ProfileLogic.summarize(history, today)!!
        assertEquals(3 * hour, summary.dailyAverageMillis)
        assertEquals(6 * hour, summary.previousDailyAverageMillis)
        assertEquals(UsageProfile.MINDFUL, summary.profile)
    }

    @Test
    fun cutToNextLevelGetsJustUnderItsLimit() {
        val summary = ProfileLogic.summarize((today - 7 until today).map { day(it, 5.0) }, today)!!
        assertEquals(hour + 60_000L, summary.cutToNextLevelMillis)
        assertEquals(76L, summary.daysPerYear)
        val monk = ProfileLogic.summarize(listOf(day(today - 1, 1.0)), today)!!
        assertNull(monk.cutToNextLevelMillis)
    }

    @Test
    fun noSummaryWithoutFinishedDays() {
        assertNull(ProfileLogic.summarize(listOf(day(today, 3.0)), today))
    }
}
