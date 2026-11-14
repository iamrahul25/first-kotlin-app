package com.nightwise.timewise.data.journey

import com.nightwise.timewise.data.usage.DayUsage

enum class BaselineSource { WEEK_BEFORE_INSTALL, FIRST_WEEK }

/** Usage before TimeWise could make a difference, frozen once measured. */
data class Baseline(
    val dailyAverageMillis: Long,
    val days: Int,
    /** Null when the device can't count unlocks. */
    val unlocksPerDay: Int?,
    val source: BaselineSource
)

enum class Verdict {
    /** No baseline yet: waiting for the first week after install. */
    MEASURING,
    /** Baseline known, but too few days since to compare. */
    TOO_EARLY,
    WORKING,
    STEADY,
    WORSE
}

data class JourneyProgress(
    /** 0 on the install day. */
    val daysWithApp: Int,
    val baseline: Baseline?,
    /** Days of the first week collected so far, while [Verdict.MEASURING]. */
    val baselineDaysCollected: Int,
    /** Average of the last 7 finished days since tracking started. */
    val recentAverageMillis: Long?,
    val recentUnlocksPerDay: Int?,
    /** Baseline minus actual, summed over every finished day since tracking started; negative means extra time. */
    val savedMillis: Long,
    /** Average daily screen time per 7-day block since tracking started, oldest first. */
    val weeklyAverages: List<Long>,
    val verdict: Verdict
) {
    /** Recent change against the baseline in percent; negative is less phone. */
    val changePercent: Int?
        get() {
            val base = baseline?.dailyAverageMillis?.takeIf { it > 0 } ?: return null
            val recent = recentAverageMillis ?: return null
            return Math.round((recent - base) * 100.0 / base).toInt()
        }
}

object JourneyLogic {

    private const val WEEK = 7
    private const val MIN_DAYS_TO_COMPARE = 3
    /** Changes within this many percent of the baseline count as no change. */
    private const val STEADY_PERCENT = 10
    private const val MAX_WEEKS = 12

    /**
     * The week before install if Android still has it, else the first full week after install.
     * The install day itself is mixed, so it's never part of either.
     */
    fun baseline(history: List<DayUsage>, installDay: Long, today: Long): Baseline? {
        val byDay = history.associateBy { it.epochDay }
        val before = tracked(byDay, installDay - WEEK, installDay)
        if (before.isNotEmpty()) return build(before, BaselineSource.WEEK_BEFORE_INSTALL)
        val firstWeekEnd = installDay + 1 + WEEK
        if (firstWeekEnd > today) return null
        val firstWeek = tracked(byDay, installDay + 1, firstWeekEnd)
        return if (firstWeek.isEmpty()) null else build(firstWeek, BaselineSource.FIRST_WEEK)
    }

    fun progress(history: List<DayUsage>, baseline: Baseline?, installDay: Long, today: Long): JourneyProgress {
        val byDay = history.associateBy { it.epochDay }
        val daysWithApp = (today - installDay).toInt().coerceAtLeast(0)
        if (baseline == null) {
            return JourneyProgress(
                daysWithApp = daysWithApp,
                baseline = null,
                baselineDaysCollected = tracked(byDay, installDay + 1, today).size.coerceAtMost(WEEK),
                recentAverageMillis = null,
                recentUnlocksPerDay = null,
                savedMillis = 0L,
                weeklyAverages = emptyList(),
                verdict = Verdict.MEASURING
            )
        }

        val start = if (baseline.source == BaselineSource.FIRST_WEEK) installDay + 1 + WEEK else installDay + 1
        val since = tracked(byDay, start, today)
        val recent = since.takeLast(WEEK)
        val recentAverage = recent.takeIf { it.isNotEmpty() }?.map { it.totalMillis }?.mean()
        val weeks = (start until today step WEEK.toLong())
            .map { from -> tracked(byDay, from, minOf(from + WEEK, today)) }
            .filter { it.isNotEmpty() }
            .map { days -> days.map { it.totalMillis }.mean() }
            .takeLast(MAX_WEEKS)

        val verdict = when {
            since.size < MIN_DAYS_TO_COMPARE || recentAverage == null -> Verdict.TOO_EARLY
            recentAverage * 100 <= baseline.dailyAverageMillis * (100 - STEADY_PERCENT) -> Verdict.WORKING
            recentAverage * 100 >= baseline.dailyAverageMillis * (100 + STEADY_PERCENT) -> Verdict.WORSE
            else -> Verdict.STEADY
        }

        return JourneyProgress(
            daysWithApp = daysWithApp,
            baseline = baseline,
            baselineDaysCollected = baseline.days,
            recentAverageMillis = recentAverage,
            recentUnlocksPerDay = recent.mapNotNull { it.unlocks }.takeIf { it.isNotEmpty() }?.let { it.sum() / it.size },
            savedMillis = since.sumOf { baseline.dailyAverageMillis - it.totalMillis },
            weeklyAverages = weeks,
            verdict = verdict
        )
    }

    private fun build(days: List<DayUsage>, source: BaselineSource) = Baseline(
        dailyAverageMillis = days.map { it.totalMillis }.mean(),
        days = days.size,
        unlocksPerDay = days.mapNotNull { it.unlocks }.takeIf { it.isNotEmpty() }?.let { it.sum() / it.size },
        source = source
    )

    /** Days in [from, until) with recorded screen time, oldest first. */
    private fun tracked(byDay: Map<Long, DayUsage>, from: Long, until: Long): List<DayUsage> =
        (from until until).mapNotNull { day -> byDay[day]?.takeIf { it.totalMillis > 0 } }

    private fun List<Long>.mean(): Long = sum() / size
}
