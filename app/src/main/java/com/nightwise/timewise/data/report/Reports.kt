package com.nightwise.timewise.data.report

import com.nightwise.timewise.data.SleepEntry
import com.nightwise.timewise.data.challenge.ChallengeResult
import com.nightwise.timewise.data.usage.AppUsage
import com.nightwise.timewise.data.usage.DayUsage
import java.util.Calendar
import java.util.TimeZone

enum class ReportPeriod(val days: Int, val title: String, val previousLabel: String) {
    WEEK(7, "Weekly report", "the week before"),
    MONTH(30, "Monthly report", "the 30 days before")
}

/** A recap of the [period] days ending yesterday, so every day in it is complete. */
data class PeriodReport(
    val period: ReportPeriod,
    val firstDay: Long,
    val lastDay: Long,
    val totalMillis: Long,
    val previousTotalMillis: Long,
    /** Days with any recorded screen time; averages use these so missing history doesn't drag them down. */
    val trackedDays: Int,
    val dailyAverageMillis: Long,
    val previousDailyAverageMillis: Long,
    /** Null when the device can't report unlocks. */
    val unlocks: Int?,
    val topApps: List<AppUsage>,
    val busiestDay: DayUsage?,
    val calmestDay: DayUsage?,
    val focusWins: Int,
    val focusAttempts: Int,
    val averageSleepMillis: Long?
) {
    val hasData: Boolean get() = trackedDays > 0
}

data class MonthAverage(val year: Int, /** 0-based, as in [Calendar]. */ val month: Int, val dailyAverageMillis: Long, val trackedDays: Int)

object ReportBuilder {

    private const val TOP_APPS = 5
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun build(
        period: ReportPeriod,
        history: List<DayUsage>,
        challenges: List<ChallengeResult>,
        sleep: List<SleepEntry>,
        today: Long
    ): PeriodReport {
        val lastDay = today - 1
        val firstDay = lastDay - period.days + 1
        val byDay = history.associateBy { it.epochDay }
        val days = (firstDay..lastDay).mapNotNull { byDay[it] }
        val previous = (firstDay - period.days until firstDay).mapNotNull { byDay[it] }
        val tracked = days.filter { it.totalMillis > 0 }
        val previousTracked = previous.filter { it.totalMillis > 0 }

        val topApps = tracked.flatMap { it.apps }
            .groupBy { it.packageName }
            .map { (pkg, usages) -> AppUsage(pkg, usages.sumOf { it.millis }) }
            .sortedByDescending { it.millis }
            .take(TOP_APPS)

        val inPeriod = { day: Long -> day in firstDay..lastDay }
        val periodChallenges = challenges.filter { inPeriod(it.epochDay) }
        val periodSleep = sleep.filter { inPeriod(it.wakeEpochDay) }

        return PeriodReport(
            period = period,
            firstDay = firstDay,
            lastDay = lastDay,
            totalMillis = tracked.sumOf { it.totalMillis },
            previousTotalMillis = previousTracked.sumOf { it.totalMillis },
            trackedDays = tracked.size,
            dailyAverageMillis = average(tracked.map { it.totalMillis }),
            previousDailyAverageMillis = average(previousTracked.map { it.totalMillis }),
            unlocks = tracked.mapNotNull { it.unlocks }.takeIf { it.isNotEmpty() }?.sum(),
            topApps = topApps,
            busiestDay = tracked.maxByOrNull { it.totalMillis },
            calmestDay = tracked.takeIf { it.size >= 2 }?.minByOrNull { it.totalMillis },
            focusWins = periodChallenges.count { it.success },
            focusAttempts = periodChallenges.size,
            averageSleepMillis = periodSleep.takeIf { it.isNotEmpty() }?.let { average(it.map(SleepEntry::durationMillis)) }
        )
    }

    /** Average daily screen time per calendar month, oldest first, for months with any data. */
    fun monthlyAverages(history: List<DayUsage>): List<MonthAverage> {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        return history.filter { it.totalMillis > 0 }
            .groupBy { day ->
                calendar.timeInMillis = day.epochDay * DAY_MS
                calendar.get(Calendar.YEAR) to calendar.get(Calendar.MONTH)
            }
            .map { (key, days) -> MonthAverage(key.first, key.second, average(days.map { it.totalMillis }), days.size) }
            .sortedWith(compareBy({ it.year }, { it.month }))
    }

    private fun average(values: List<Long>): Long = if (values.isEmpty()) 0L else values.sum() / values.size
}
