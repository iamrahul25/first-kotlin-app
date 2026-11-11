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

data class ProfileSummary(
    val profile: UsageProfile,
    /** Average over the finished days of the last week that have data. */
    val dailyAverageMillis: Long,
    val trackedDays: Int,
    /** Same average for the week before, or null without data. */
    val previousDailyAverageMillis: Long?,
    val thirtyDayAverageMillis: Long?
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
            thirtyDayAverageMillis = tracked(byDay, today - MONTH, today).takeIf { it.size > WEEK }?.meanMillis()
        )
    }

    private fun tracked(byDay: Map<Long, DayUsage>, from: Long, until: Long): List<Long> =
        (from until until).mapNotNull { byDay[it]?.totalMillis?.takeIf { millis -> millis > 0 } }

    private fun List<Long>.meanMillis(): Long = sum() / size
}
