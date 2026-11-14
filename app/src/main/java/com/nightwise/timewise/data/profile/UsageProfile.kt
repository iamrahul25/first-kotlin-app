package com.nightwise.timewise.data.profile

import com.nightwise.timewise.data.usage.DayUsage

private const val HOUR = 60 * 60 * 1000L

/** Levels by average daily screen time, best first. [maxDailyMillis] is exclusive. */
enum class UsageProfile(val title: String, val tagline: String, val maxDailyMillis: Long) {
    MONK("Monk", "Your time is yours. The phone serves you, not the other way round.", 2 * HOUR),
    MINDFUL("Mindful", "Mostly in control. Trim a little more and you'll reach Monk.", 4 * HOUR),
    HOOKED("Hooked", "The hook is in. Your phone decides more of your day than you do.", 6 * HOUR),
    ADDICT("Addict", "Half your waking day vanishes into a screen. This habit can be broken.", 8 * HOUR),
    ZOMBIE("Zombie", "Your phone is living your life for you. Put it down and take it back.", Long.MAX_VALUE);

    val minDailyMillis: Long get() = entries.getOrNull(ordinal - 1)?.maxDailyMillis ?: 0L

    /** The next level up, or null for [MONK]. */
    val better: UsageProfile? get() = entries.getOrNull(ordinal - 1)
}

/** One past week: its average daily screen time and the level that average falls in. */
data class WeekStatus(
    /** 1 is the last finished week, larger is further back. */
    val weeksAgo: Int,
    /** First and last day of the week, both inclusive, as epoch days. */
    val startEpochDay: Long,
    val endEpochDay: Long,
    val dailyAverageMillis: Long,
    val profile: UsageProfile
)

data class ProfileSummary(
    val profile: UsageProfile,
    /** Average over the finished days of the last week that have data. */
    val dailyAverageMillis: Long,
    val trackedDays: Int,
    /** Same average for the week before, or null without data. */
    val previousDailyAverageMillis: Long?,
    val thirtyDayAverageMillis: Long?,
    /** Oldest first. Only weeks that have data are included. */
    val weeklyStatuses: List<WeekStatus> = emptyList()
) {
    /** How much less a day it takes to reach the next level, or null at the top. */
    val cutToNextLevelMillis: Long?
        get() = profile.better?.let { dailyAverageMillis - it.maxDailyMillis + MINUTE }

    /** Share of a 16-hour waking day spent on screen. */
    val wakingShare: Float get() = dailyAverageMillis.toFloat() / WAKING_DAY_MILLIS

    /** Days a year this average adds up to. */
    val daysPerYear: Long get() = dailyAverageMillis * 365 / DAY_MILLIS

    companion object {
        private const val MINUTE = 60_000L
        private const val DAY_MILLIS = 24 * HOUR
        private const val WAKING_DAY_MILLIS = 16 * HOUR
    }
}

object ProfileLogic {

    private const val WEEK = 7
    private const val MONTH = 30
    /** How many past weeks the weekly status graph shows. */
    const val WEEKS_SHOWN = 6

    fun profileFor(dailyAverageMillis: Long): UsageProfile =
        UsageProfile.entries.first { dailyAverageMillis < it.maxDailyMillis }

    /** Null until at least one finished day has data. Today is left out since it isn't over. */
    fun summarize(history: List<DayUsage>, today: Long): ProfileSummary? {
        val byDay = history.associateBy { it.epochDay }
        val week = tracked(byDay, today - WEEK, today)
        if (week.isEmpty()) return null
        val average = week.meanMillis()
        return ProfileSummary(
            profile = profileFor(average),
            dailyAverageMillis = average,
            trackedDays = week.size,
            previousDailyAverageMillis = tracked(byDay, today - 2 * WEEK, today - WEEK).takeIf { it.isNotEmpty() }?.meanMillis(),
            thirtyDayAverageMillis = tracked(byDay, today - MONTH, today).takeIf { it.size > WEEK }?.meanMillis(),
            weeklyStatuses = weeklyStatuses(byDay, today)
        )
    }

    /** One status per past week with data, oldest first. Weeks are 7-day blocks ending today. */
    private fun weeklyStatuses(byDay: Map<Long, DayUsage>, today: Long): List<WeekStatus> =
        (WEEKS_SHOWN downTo 1).mapNotNull { weeksAgo ->
            val from = today - WEEK * weeksAgo
            tracked(byDay, from, from + WEEK).takeIf { it.isNotEmpty() }?.let { days ->
                val average = days.meanMillis()
                WeekStatus(weeksAgo, from, from + WEEK - 1, average, profileFor(average))
            }
        }

    private fun tracked(byDay: Map<Long, DayUsage>, from: Long, until: Long): List<Long> =
        (from until until).mapNotNull { byDay[it]?.totalMillis?.takeIf { millis -> millis > 0 } }

    private fun List<Long>.meanMillis(): Long = sum() / size
}
