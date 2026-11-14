package com.nightwise.timewise.data.badges

import com.nightwise.timewise.data.usage.AppUsage
import com.nightwise.timewise.data.usage.DayUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BadgeEngineTest {

    private val hour = 60 * 60 * 1000L

    // Epoch day 20003 is Monday 7 Oct 2024.
    private val monday = 20003L

    private fun day(epochDay: Long, hours: Double, unlocks: Int? = 50, nightMinutes: Int = 0): DayUsage {
        val total = (hours * hour).toLong()
        val hourly = List(24) { if (it == 2) nightMinutes else if (it == 12) (total / 60_000).toInt() else 0 }
        return DayUsage(epochDay, total, listOf(AppUsage("app", total)), unlocks, hourly)
    }

    private fun days(from: Long, hours: List<Double>) = hours.mapIndexed { i, h -> day(from + i, h) }

    private fun earned(badge: UsageBadge, history: List<DayUsage>, today: Long) =
        BadgeEngine.earnedPeriods(badge, history, today, Calendar.MONDAY)

    @Test
    fun dailyAchievementCountsEveryFinishedDay() {
        val history = days(monday, listOf(0.5, 2.0, 0.8, 0.9))
        assertEquals(setOf(monday, monday + 2), earned(UsageBadge.ZEN_MASTER, history, today = monday + 3))
    }

    @Test
    fun todayOnlyCountsForWarnings() {
        val history = listOf(day(monday, 0.2), day(monday + 1, 11.0))
        assertEquals(emptySet<Long>(), earned(UsageBadge.GHOST_MODE, history, today = monday))
        assertEquals(setOf(monday + 1), earned(UsageBadge.SCREEN_ADDICT, history, today = monday + 1))
    }

    @Test
    fun daysWithoutDataNeverEarnAchievements() {
        val history = listOf(DayUsage.empty(monday, unlocks = null))
        assertEquals(emptySet<Long>(), earned(UsageBadge.GHOST_MODE, history, today = monday + 1))
    }

    @Test
    fun streakCountsEachFullBlock() {
        val history = days(monday, List(15) { 3.0 })
        val starts = earned(UsageBadge.STEADY_SEVEN, history, today = monday + 15)
        assertEquals(setOf(monday, monday + 7), starts)
    }

    @Test
    fun brokenStreakStartsOver() {
        val history = days(monday, listOf(3.0, 3.0, 3.0, 5.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0))
        assertEquals(setOf(monday + 4), earned(UsageBadge.STEADY_SEVEN, history, today = monday + 11))
    }

    @Test
    fun streakFoundAgainAfterHistoryShiftIsNotDoubleCounted() {
        val saved = setOf(monday)
        val shifted = setOf(monday + 3)
        assertEquals(saved, BadgeEngine.merge(UsageBadge.STEADY_SEVEN, saved, shifted))
        assertEquals(setOf(monday, monday + 7), BadgeEngine.merge(UsageBadge.STEADY_SEVEN, saved, setOf(monday + 7)))
    }

    @Test
    fun weeklyAchievementNeedsAFullFinishedWeek() {
        val week = days(monday, List(7) { 1.0 })
        assertEquals(setOf(monday), earned(UsageBadge.DIGITAL_MONK, week, today = monday + 7))
        assertEquals(emptySet<Long>(), earned(UsageBadge.DIGITAL_MONK, week, today = monday + 6))
        val missingDay = week.filter { it.epochDay != monday + 3 }
        assertEquals(emptySet<Long>(), earned(UsageBadge.DIGITAL_MONK, missingDay, today = monday + 7))
    }

    @Test
    fun weeklyWarningTriggersMidWeek() {
        val history = days(monday, listOf(12.0, 12.0, 12.0, 12.0, 3.0))
        assertEquals(setOf(monday), earned(UsageBadge.BINGE_WEEK, history, today = monday + 4))
    }

    @Test
    fun weeksFollowTheFirstDayOfWeek() {
        assertEquals(monday..monday + 6, BadgeEngine.spanRange(Span.WEEK, monday + 3, Calendar.MONDAY))
        assertEquals(monday - 1..monday + 5, BadgeEngine.spanRange(Span.WEEK, monday + 3, Calendar.SUNDAY))
    }

    @Test
    fun monthsFollowTheCalendar() {
        // 1 Oct 2024 is epoch day 19997.
        assertEquals(19997L..20027L, BadgeEngine.spanRange(Span.MONTH, monday))
    }

    @Test
    fun weeklyDropComparesWithThePreviousWeek() {
        val history = days(monday - 7, List(7) { 5.0 }) + days(monday, List(7) { 3.9 })
        assertEquals(setOf(monday), earned(UsageBadge.COMEBACK_KID, history, today = monday + 7))
    }

    @Test
    fun unlocksAndNightUseUseTheirOwnMeasures() {
        val history = listOf(day(monday, 2.0, unlocks = 160, nightMinutes = 70), day(monday + 1, 2.0, unlocks = null))
        val today = monday + 2
        assertEquals(setOf(monday), earned(UsageBadge.COMPULSIVE_CHECKER, history, today))
        assertEquals(setOf(monday), earned(UsageBadge.MIDNIGHT_SCROLLER, history, today))
        assertEquals(setOf(monday + 1), earned(UsageBadge.SWEET_DREAMS, history, today))
        assertEquals(emptySet<Long>(), earned(UsageBadge.HANDS_OFF, history, today))
    }

    @Test
    fun progressShowsTodayAndRunningStreak() {
        val history = days(monday, listOf(3.0, 3.0, 3.0, 0.5))
        val today = monday + 3
        assertEquals(BadgeProgress(ProgressScope.STREAK, 3, 7), BadgeEngine.progress(UsageBadge.STEADY_SEVEN, history, today))
        assertEquals(BadgeProgress(ProgressScope.TODAY, hour / 2, hour), BadgeEngine.progress(UsageBadge.ZEN_MASTER, history, today))
        val week = BadgeEngine.progress(UsageBadge.BALANCED_WEEK, history, today)
        assertEquals(ProgressScope.THIS_WEEK, week?.scope)
        assertEquals((9.5 * hour).toLong(), week?.current)
    }

    @Test
    fun noProgressWithoutUnlockSupport() {
        assertNull(BadgeEngine.progress(UsageBadge.HANDS_OFF, listOf(day(monday, 1.0, unlocks = null)), monday))
        assertTrue(UsageBadge.entries.map { it.title }.toSet().size == UsageBadge.entries.size)
    }
}
