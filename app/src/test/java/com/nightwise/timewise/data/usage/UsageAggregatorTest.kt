package com.nightwise.timewise.data.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageAggregatorTest {

    private val min = 60_000L
    private val hour = 60 * min

    private fun resumed(pkg: String, at: Long, cls: String? = null) = RawEvent(pkg, EventType.ACTIVITY_RESUMED, at, cls)
    private fun paused(pkg: String, at: Long, cls: String? = null) = RawEvent(pkg, EventType.ACTIVITY_PAUSED, at, cls)
    private fun stopped(pkg: String, at: Long, cls: String? = null) = RawEvent(pkg, EventType.ACTIVITY_STOPPED, at, cls)
    private fun screenOff(at: Long) = RawEvent("android", EventType.SCREEN_NON_INTERACTIVE, at)
    private fun screenOn(at: Long) = RawEvent("android", EventType.SCREEN_INTERACTIVE, at)
    private fun unlock(at: Long) = RawEvent("android", EventType.KEYGUARD_HIDDEN, at)

    @Test
    fun switchingAppsSplitsSessions() {
        val events = listOf(resumed("a", 0), resumed("b", 10 * min), paused("b", 25 * min))
        val sessions = UsageAggregator.sessions(events, 0, hour)
        assertEquals(listOf(UsageSession("a", 0, 10 * min), UsageSession("b", 10 * min, 25 * min)), sessions)
    }

    @Test
    fun launcherTimeIsNotCounted() {
        val events = listOf(
            resumed("a", 0), paused("a", 5 * min), resumed("launcher", 5 * min),
            resumed("b", 20 * min), paused("b", 30 * min)
        )
        val sessions = UsageAggregator.sessions(events, 0, hour, ignored = setOf("launcher"))
        assertEquals(5 * min, sessions.filter { it.packageName == "a" }.sumOf { it.durationMillis })
        assertEquals(10 * min, sessions.filter { it.packageName == "b" }.sumOf { it.durationMillis })
        assertEquals(0, sessions.count { it.packageName == "launcher" })
    }

    @Test
    fun stoppingThePreviousScreenDoesNotEndTheApp() {
        // Opening a chat and going back: the old activity stops only after the new one resumes.
        val events = listOf(
            resumed("slack", 0, "Home"),
            paused("slack", 1 * min, "Home"), resumed("slack", 1 * min, "Chat"), stopped("slack", 1 * min + 500, "Home"),
            paused("slack", 2 * min, "Chat"), resumed("slack", 2 * min, "Home"), stopped("slack", 2 * min + 500, "Chat"),
            screenOff(12 * min)
        )
        assertEquals(listOf(UsageSession("slack", 0, 12 * min)), UsageAggregator.sessions(events, 0, hour))
    }

    @Test
    fun floatingWindowOverHomeScreenKeepsCounting() {
        val events = listOf(
            screenOn(0), resumed("slack", 1_000, "Home"), resumed("launcher", 1_000, "Launcher"),
            screenOff(10 * min)
        )
        val sessions = UsageAggregator.sessions(events, 0, hour, ignored = setOf("launcher"))
        assertEquals(listOf(UsageSession("slack", 1_000, 10 * min)), sessions)
    }

    @Test
    fun screenOffClosesSession() {
        val events = listOf(resumed("a", 0), screenOff(15 * min))
        assertEquals(listOf(UsageSession("a", 0, 15 * min)), UsageAggregator.sessions(events, 0, hour))
    }

    @Test
    fun openSessionIsClippedToWindow() {
        val events = listOf(resumed("a", -30 * min))
        assertEquals(listOf(UsageSession("a", 0, hour)), UsageAggregator.sessions(events, 0, hour))
    }

    @Test
    fun activityHopsInsideOneAppAreMerged() {
        val events = listOf(resumed("a", 0), paused("a", 5 * min), resumed("a", 5 * min + 300), paused("a", 9 * min))
        assertEquals(listOf(UsageSession("a", 0, 9 * min)), UsageAggregator.sessions(events, 0, hour))
    }

    @Test
    fun summarizeSplitsAcrossMidnightAndBucketsHours() {
        val day = 24 * hour
        val sessions = listOf(UsageSession("a", day - 30 * min, day + 90 * min))
        val events = listOf(unlock(day - 40 * min), unlock(day + 5 * min), unlock(day + 10 * min))
        val days = UsageAggregator.summarizeDays(
            sessions, events,
            epochDays = listOf(0, 1),
            dayStarts = listOf(0, day),
            dayEnds = listOf(day, 2 * day),
            unlocksSupported = true
        )
        assertEquals(30 * min, days[0].totalMillis)
        assertEquals(90 * min, days[1].totalMillis)
        assertEquals(30, days[0].hourlyMinutes[23])
        assertEquals(listOf(60, 30), days[1].hourlyMinutes.take(2))
        assertEquals(1, days[0].unlocks)
        assertEquals(2, days[1].unlocks)
    }

    @Test
    fun appsAreRankedByTime() {
        val sessions = listOf(UsageSession("a", 0, 5 * min), UsageSession("b", 5 * min, 25 * min), UsageSession("a", 30 * min, 32 * min))
        val day = UsageAggregator.summarizeDays(sessions, emptyList(), listOf(0), listOf(0), listOf(hour), true).single()
        assertEquals(listOf(AppUsage("b", 20 * min), AppUsage("a", 7 * min)), day.apps)
        assertEquals(27 * min, day.totalMillis)
    }

    @Test
    fun unlocksFallBackToScreenOnWithoutKeyguard() {
        val events = listOf(screenOn(1), screenOn(2), screenOn(3))
        assertEquals(3, UsageAggregator.countUnlocks(events, 0, 10))
    }

    @Test
    fun longestScreenOffIsTheNight() {
        val events = listOf(
            screenOff(0), screenOn(1 * hour),
            screenOff(2 * hour), screenOn(9 * hour),
            screenOff(10 * hour), screenOn(10 * hour + 5 * min)
        )
        val gap = UsageAggregator.longestScreenOff(events, 0, 12 * hour, minMillis = 2 * hour)
        assertEquals(SleepGap(2 * hour, 9 * hour), gap)
    }

    @Test
    fun shortGapsAreNotSleep() {
        val events = listOf(screenOff(0), screenOn(30 * min))
        assertNull(UsageAggregator.longestScreenOff(events, 0, hour, minMillis = 2 * hour))
    }
}
