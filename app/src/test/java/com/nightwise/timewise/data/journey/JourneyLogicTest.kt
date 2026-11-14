package com.nightwise.timewise.data.journey

import com.nightwise.timewise.data.usage.DayUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JourneyLogicTest {

    private val hour = 60 * 60 * 1000L
    private val install = 100L

    private fun day(epochDay: Long, hours: Double, unlocks: Int? = 50) =
        DayUsage(epochDay, (hours * hour).toLong(), emptyList(), unlocks, List(24) { 0 })

    private fun days(from: Long, until: Long, hours: Double, unlocks: Int? = 50) =
        (from until until).map { day(it, hours, unlocks) }

    @Test
    fun baselineIsTheWeekBeforeInstall() {
        val history = days(install - 10, install, 8.0, unlocks = 80) + day(install, 1.0)
        val baseline = JourneyLogic.baseline(history, install, today = install)!!
        assertEquals(8 * hour, baseline.dailyAverageMillis)
        assertEquals(7, baseline.days)
        assertEquals(80, baseline.unlocksPerDay)
        assertEquals(BaselineSource.WEEK_BEFORE_INSTALL, baseline.source)
    }

    @Test
    fun withoutOlderDataTheFirstWeekIsTheBaseline() {
        val history = days(install, install + 8, 5.0)
        assertNull(JourneyLogic.baseline(history, install, today = install + 7))
        val baseline = JourneyLogic.baseline(history, install, today = install + 8)!!
        assertEquals(BaselineSource.FIRST_WEEK, baseline.source)
        assertEquals(5 * hour, baseline.dailyAverageMillis)
    }

    @Test
    fun measuringUntilTheFirstWeekIsDone() {
        val progress = JourneyLogic.progress(days(install, install + 4, 5.0), null, install, today = install + 4)
        assertEquals(Verdict.MEASURING, progress.verdict)
        assertEquals(3, progress.baselineDaysCollected)
    }

    @Test
    fun lessUsageIsWorkingAndCountsTimeSaved() {
        val baseline = Baseline(8 * hour, 7, 80, BaselineSource.WEEK_BEFORE_INSTALL)
        val history = days(install + 1, install + 11, 6.0, unlocks = 40)
        val progress = JourneyLogic.progress(history, baseline, install, today = install + 11)
        assertEquals(Verdict.WORKING, progress.verdict)
        assertEquals(6 * hour, progress.recentAverageMillis)
        assertEquals(-25, progress.changePercent)
        assertEquals(20 * hour, progress.savedMillis)
        assertEquals(40, progress.recentUnlocksPerDay)
        assertEquals(listOf(6 * hour, 6 * hour), progress.weeklyAverages)
    }

    @Test
    fun verdictBands() {
        val baseline = Baseline(8 * hour, 7, null, BaselineSource.WEEK_BEFORE_INSTALL)
        fun verdict(hours: Double) =
            JourneyLogic.progress(days(install + 1, install + 8, hours), baseline, install, today = install + 8).verdict
        assertEquals(Verdict.STEADY, verdict(7.5))
        assertEquals(Verdict.WORSE, verdict(9.0))
        assertEquals(Verdict.TOO_EARLY, JourneyLogic.progress(days(install + 1, install + 3, 1.0), baseline, install, today = install + 3).verdict)
    }

    @Test
    fun firstWeekBaselineIsNotComparedWithItself() {
        val baseline = Baseline(5 * hour, 7, null, BaselineSource.FIRST_WEEK)
        val history = days(install + 1, install + 8, 5.0) + days(install + 8, install + 12, 3.0)
        val progress = JourneyLogic.progress(history, baseline, install, today = install + 12)
        assertEquals(3 * hour, progress.recentAverageMillis)
        assertEquals(8 * hour, progress.savedMillis)
    }
}
