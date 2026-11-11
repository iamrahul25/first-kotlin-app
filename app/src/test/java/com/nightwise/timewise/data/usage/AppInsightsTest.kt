package com.nightwise.timewise.data.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppInsightsTest {

    private val hour = 60 * 60 * 1000L
    private val today = 100L

    private fun day(epochDay: Long, vararg apps: Pair<String, Double>): DayUsage {
        val usages = apps.map { (pkg, hours) -> AppUsage(pkg, (hours * hour).toLong()) }
        return DayUsage(epochDay, usages.sumOf { it.millis }, usages.sortedByDescending { it.millis }, null, List(24) { 0 })
    }

    private val history = listOf(
        day(today - 10, "insta" to 4.0),
        day(today - 8, "insta" to 2.0, "yt" to 1.0),
        day(today - 3, "insta" to 1.0, "yt" to 3.0),
        day(today - 1, "yt" to 2.0),
        day(today, "insta" to 0.5)
    )

    @Test
    fun weekRankingIncludesTodayAndComparesWithTheWeekBefore() {
        val ranking = AppInsights.ranking(history, AppPeriod.WEEK, today)
        assertEquals(listOf("yt", "insta"), ranking.apps.map { it.packageName })
        val yt = ranking.apps[0]
        assertEquals(5 * hour, yt.totalMillis)
        assertEquals(2, yt.activeDays)
        assertEquals(1 * hour, yt.previousMillis)
        assertEquals((1.5 * hour).toLong(), ranking.apps[1].totalMillis)
        assertEquals(6 * hour, ranking.previousOf("insta"))
        assertEquals(3, ranking.trackedDays)
    }

    @Test
    fun allTimeHasNoPreviousPeriod() {
        val ranking = AppInsights.ranking(history, AppPeriod.ALL, today)
        assertEquals("insta", ranking.apps.first().packageName)
        assertEquals((7.5 * hour).toLong(), ranking.apps.first().totalMillis)
        assertNull(ranking.apps.first().previousMillis)
    }

    @Test
    fun detailTotalsAndAverageCountDaysWithoutTheApp() {
        val detail = AppInsights.detail(history, "insta", today)
        assertEquals(hour / 2, detail.todayMillis)
        assertEquals((1.5 * hour).toLong(), detail.weekMillis)
        assertEquals(6 * hour, detail.previousWeekMillis)
        assertEquals((7.5 * hour).toLong(), detail.allTimeMillis)
        assertEquals(5, detail.trackedDays)
        assertEquals(4, detail.activeDays)
        assertEquals((1.5 * hour).toLong(), detail.dailyAverageMillis)
        assertEquals(today - 10, detail.sinceDay)
        assertEquals(today - 10, detail.busiestDay?.epochDay)
        assertEquals(30, detail.daily.size)
        assertEquals(today, detail.daily.last().epochDay)
        assertEquals(0L, detail.daily[detail.daily.size - 2].millis)
    }

    private fun AppRanking.previousOf(pkg: String) = apps.first { it.packageName == pkg }.previousMillis
}
