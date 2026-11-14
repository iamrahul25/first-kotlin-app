package com.nightwise.timewise.data.report

import com.nightwise.timewise.data.SleepEntry
import com.nightwise.timewise.data.challenge.ChallengeResult
import com.nightwise.timewise.data.challenge.ChallengeType
import com.nightwise.timewise.data.usage.AppUsage
import com.nightwise.timewise.data.usage.DayUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReportBuilderTest {

    private val hour = 60 * 60_000L

    private fun day(epochDay: Long, hours: Long, vararg apps: Pair<String, Long>, unlocks: Int? = 10) =
        DayUsage(epochDay, hours * hour, apps.map { AppUsage(it.first, it.second * hour) }, unlocks, List(24) { 0 })

    @Test
    fun weeklyReportCoversTheSevenDaysBeforeToday() {
        val today = 100L
        val history = (86L..100L).map { day(it, hours = if (it >= 93) 2 else 4, "a" to 1) }
        val report = ReportBuilder.build(ReportPeriod.WEEK, history, emptyList(), emptyList(), today)

        assertEquals(93L, report.firstDay)
        assertEquals(99L, report.lastDay)
        assertEquals(14 * hour, report.totalMillis)
        assertEquals(28 * hour, report.previousTotalMillis)
        assertEquals(2 * hour, report.dailyAverageMillis)
        assertEquals(70, report.unlocks)
    }

    @Test
    fun averagesSkipDaysWithoutData() {
        val history = listOf(day(97, 3), day(98, 0), day(99, 1))
        val report = ReportBuilder.build(ReportPeriod.WEEK, history, emptyList(), emptyList(), today = 100)
        assertEquals(2, report.trackedDays)
        assertEquals(2 * hour, report.dailyAverageMillis)
        assertEquals(97L, report.busiestDay?.epochDay)
        assertEquals(99L, report.calmestDay?.epochDay)
    }

    @Test
    fun topAppsAreSummedAcrossDays() {
        val history = listOf(day(98, 3, "a" to 1, "b" to 2), day(99, 3, "a" to 2, "c" to 1))
        val report = ReportBuilder.build(ReportPeriod.WEEK, history, emptyList(), emptyList(), today = 100)
        assertEquals(listOf(AppUsage("a", 3 * hour), AppUsage("b", 2 * hour), AppUsage("c", hour)), report.topApps)
    }

    @Test
    fun focusAndSleepAreLimitedToThePeriod() {
        val challenges = listOf(
            ChallengeResult(ChallengeType.ONE_HOUR, 0, 1, success = true, epochDay = 99),
            ChallengeResult(ChallengeType.ONE_HOUR, 0, 1, success = false, epochDay = 98),
            ChallengeResult(ChallengeType.ONE_HOUR, 0, 1, success = true, epochDay = 100)
        )
        val sleep = listOf(SleepEntry(99, 0, 8 * hour), SleepEntry(98, 0, 6 * hour), SleepEntry(80, 0, hour))
        val report = ReportBuilder.build(ReportPeriod.WEEK, listOf(day(99, 1)), challenges, sleep, today = 100)
        assertEquals(1, report.focusWins)
        assertEquals(2, report.focusAttempts)
        assertEquals(7 * hour, report.averageSleepMillis)
    }

    @Test
    fun emptyHistoryHasNoData() {
        val report = ReportBuilder.build(ReportPeriod.MONTH, emptyList(), emptyList(), emptyList(), today = 100)
        assertEquals(false, report.hasData)
        assertNull(report.unlocks)
        assertNull(report.averageSleepMillis)
    }

    @Test
    fun monthlyAveragesGroupByCalendarMonth() {
        // Epoch day 0 is 1 Jan 1970; day 31 is 1 Feb 1970.
        val history = listOf(day(0, 2), day(1, 4), day(2, 0), day(31, 1))
        val months = ReportBuilder.monthlyAverages(history)
        assertEquals(listOf(MonthAverage(1970, 0, 3 * hour, 2), MonthAverage(1970, 1, hour, 1)), months)
    }
}
