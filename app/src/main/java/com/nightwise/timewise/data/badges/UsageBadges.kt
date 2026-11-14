package com.nightwise.timewise.data.badges

import com.nightwise.timewise.data.usage.DayUsage
import java.util.Calendar
import java.util.TimeZone

enum class BadgeKind { ACHIEVEMENT, WARNING }

enum class BadgeTier(val label: String, val kind: BadgeKind) {
    BRONZE("Bronze", BadgeKind.ACHIEVEMENT),
    SILVER("Silver", BadgeKind.ACHIEVEMENT),
    GOLD("Gold", BadgeKind.ACHIEVEMENT),
    LEGENDARY("Legendary", BadgeKind.ACHIEVEMENT),
    MILD("Mild", BadgeKind.WARNING),
    SEVERE("Severe", BadgeKind.WARNING),
    EXTREME("Extreme", BadgeKind.WARNING)
}

enum class Measure {
    SCREEN_TIME,
    UNLOCKS,
    /** Time in the day's most used app. */
    TOP_APP,
    /** Screen time between midnight and 6 AM. */
    NIGHT_USE
}

enum class Span { DAY, WEEK, MONTH }

sealed interface BadgeRule {
    val measure: Measure

    /**
     * Achievements need the measure to stay under [threshold]; warnings trigger at or above it.
     * With [streakDays] > 1 the daily limit has to hold that many days in a row.
     */
    data class Limit(
        override val measure: Measure,
        val span: Span,
        val threshold: Long,
        val streakDays: Int = 1
    ) : BadgeRule

    /** A full week at least [percent]% below the full week before it. */
    data class WeeklyDrop(val percent: Int) : BadgeRule {
        override val measure: Measure get() = Measure.SCREEN_TIME
    }
}

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE

/** Earned again every time the rule holds, so each has a lifetime count. Ordered easiest first. */
enum class UsageBadge(val title: String, val description: String, val tier: BadgeTier, val rule: BadgeRule) {
    MINDFUL_DAY("Mindful Day", "Under 3 hours of screen time in a day", BadgeTier.BRONZE, BadgeRule.Limit(Measure.SCREEN_TIME, Span.DAY, 3 * HOUR)),
    SWEET_DREAMS("Sweet Dreams", "No screen time between midnight and 6 AM", BadgeTier.BRONZE, BadgeRule.Limit(Measure.NIGHT_USE, Span.DAY, MINUTE)),
    COMEBACK_KID("Comeback Kid", "A full week 20% below the week before", BadgeTier.BRONZE, BadgeRule.WeeklyDrop(20)),
    BALANCED_WEEK("Balanced Week", "Under 30 hours in a week", BadgeTier.BRONZE, BadgeRule.Limit(Measure.SCREEN_TIME, Span.WEEK, 30 * HOUR)),
    LIGHT_TOUCH("Light Touch", "Under 2 hours of screen time in a day", BadgeTier.SILVER, BadgeRule.Limit(Measure.SCREEN_TIME, Span.DAY, 2 * HOUR)),
    HANDS_OFF("Hands Off", "Fewer than 30 unlocks in a day", BadgeTier.SILVER, BadgeRule.Limit(Measure.UNLOCKS, Span.DAY, 30)),
    STEADY_SEVEN("Steady Seven", "Under 4 hours every day, 7 days in a row", BadgeTier.SILVER, BadgeRule.Limit(Measure.SCREEN_TIME, Span.DAY, 4 * HOUR, streakDays = 7)),
    FREE_SPIRIT("Free Spirit", "Under 20 hours in a week", BadgeTier.SILVER, BadgeRule.Limit(Measure.SCREEN_TIME, Span.WEEK, 20 * HOUR)),
    ZEN_MASTER("Zen Master", "Under 1 hour of screen time in a day", BadgeTier.GOLD, BadgeRule.Limit(Measure.SCREEN_TIME, Span.DAY, HOUR)),
    IRON_WILL("Iron Will", "Under 2 hours every day, 7 days in a row", BadgeTier.GOLD, BadgeRule.Limit(Measure.SCREEN_TIME, Span.DAY, 2 * HOUR, streakDays = 7)),
    CLEAN_MONTH("Clean Month", "Under 90 hours in a calendar month", BadgeTier.GOLD, BadgeRule.Limit(Measure.SCREEN_TIME, Span.MONTH, 90 * HOUR)),
    GHOST_MODE("Ghost Mode", "Under 30 minutes of screen time in a day", BadgeTier.LEGENDARY, BadgeRule.Limit(Measure.SCREEN_TIME, Span.DAY, 30 * MINUTE)),
    DIGITAL_MONK("Digital Monk", "Under 10 hours in a week", BadgeTier.LEGENDARY, BadgeRule.Limit(Measure.SCREEN_TIME, Span.WEEK, 10 * HOUR)),
    UNBREAKABLE("Unbreakable", "Under 3 hours every day, 30 days in a row", BadgeTier.LEGENDARY, BadgeRule.Limit(Measure.SCREEN_TIME, Span.DAY, 3 * HOUR, streakDays = 30)),
    OFFLINE_LEGEND("Offline Legend", "Under 60 hours in a calendar month", BadgeTier.LEGENDARY, BadgeRule.Limit(Measure.SCREEN_TIME, Span.MONTH, 60 * HOUR)),

    COUCH_POTATO("Couch Potato", "6+ hours of screen time in a day", BadgeTier.MILD, BadgeRule.Limit(Measure.SCREEN_TIME, Span.DAY, 6 * HOUR)),
    RABBIT_HOLE("Rabbit Hole", "3+ hours in a single app in a day", BadgeTier.MILD, BadgeRule.Limit(Measure.TOP_APP, Span.DAY, 3 * HOUR)),
    MIDNIGHT_SCROLLER("Midnight Scroller", "1+ hour on the phone between midnight and 6 AM", BadgeTier.MILD, BadgeRule.Limit(Measure.NIGHT_USE, Span.DAY, HOUR)),
    SCREEN_ADDICT("Screen Addict", "10+ hours of screen time in a day", BadgeTier.SEVERE, BadgeRule.Limit(Measure.SCREEN_TIME, Span.DAY, 10 * HOUR)),
    COMPULSIVE_CHECKER("Compulsive Checker", "150+ unlocks in a day", BadgeTier.SEVERE, BadgeRule.Limit(Measure.UNLOCKS, Span.DAY, 150)),
    BINGE_WEEK("Binge Week", "50+ hours in a week", BadgeTier.SEVERE, BadgeRule.Limit(Measure.SCREEN_TIME, Span.WEEK, 50 * HOUR)),
    GLUED_TO_GLASS("Glued to the Glass", "200+ hours in a calendar month", BadgeTier.SEVERE, BadgeRule.Limit(Measure.SCREEN_TIME, Span.MONTH, 200 * HOUR)),
    ZOMBIE_MODE("Zombie Mode", "14+ hours of screen time in a day", BadgeTier.EXTREME, BadgeRule.Limit(Measure.SCREEN_TIME, Span.DAY, 14 * HOUR)),
    DOOM_SPIRAL("Doom Spiral", "8+ hours every day, 7 days in a row", BadgeTier.EXTREME, BadgeRule.Limit(Measure.SCREEN_TIME, Span.DAY, 8 * HOUR, streakDays = 7)),
    LOST_WEEK("Lost Week", "70+ hours in a week", BadgeTier.EXTREME, BadgeRule.Limit(Measure.SCREEN_TIME, Span.WEEK, 70 * HOUR)),
    DIGITAL_PRISONER("Digital Prisoner", "300+ hours in a calendar month", BadgeTier.EXTREME, BadgeRule.Limit(Measure.SCREEN_TIME, Span.MONTH, 300 * HOUR));

    val kind: BadgeKind get() = tier.kind
}

enum class ProgressScope { TODAY, THIS_WEEK, THIS_MONTH, STREAK }

/** Where the current day, week, month or run stands against a badge's target. */
data class BadgeProgress(val scope: ProgressScope, val current: Long, val target: Long)

object BadgeEngine {

    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val NIGHT_END_HOUR = 6

    /**
     * Periods the badge was earned in, keyed by the epoch day each period starts on.
     * Achievements only count finished periods with full data; warnings count as soon as
     * they trigger, since usage only goes up.
     */
    fun earnedPeriods(
        badge: UsageBadge,
        history: List<DayUsage>,
        today: Long,
        firstDayOfWeek: Int = Calendar.MONDAY
    ): Set<Long> {
        val byDay = history.associateBy { it.epochDay }
        return when (val rule = badge.rule) {
            is BadgeRule.Limit -> when {
                rule.streakDays > 1 -> streakStarts(badge, rule, history, today)
                rule.span == Span.DAY -> history.filter { qualifies(badge, rule, it, today) }.map { it.epochDay }.toSet()
                else -> history.map { spanRange(rule.span, it.epochDay, firstDayOfWeek) }
                    .distinct()
                    .filter { range -> spanQualifies(badge, rule, range, byDay, today) }
                    .map { it.first }
                    .toSet()
            }
            is BadgeRule.WeeklyDrop -> history.map { spanRange(Span.WEEK, it.epochDay, firstDayOfWeek) }
                .distinct()
                .filter { week ->
                    val current = fullTotal(week, byDay, today) ?: return@filter false
                    val previous = fullTotal(shift(week, -7), byDay, today) ?: return@filter false
                    current <= dropTarget(previous, rule.percent)
                }
                .map { it.first }
                .toSet()
        }
    }

    /**
     * Adds newly [found] periods to the [saved] ones. A streak's start shifts if its first days
     * fall out of history, so a found streak overlapping a saved one is the same streak.
     */
    fun merge(badge: UsageBadge, saved: Set<Long>, found: Set<Long>): Set<Long> {
        val streakDays = (badge.rule as? BadgeRule.Limit)?.streakDays ?: 1
        if (streakDays <= 1) return saved + found
        val merged = saved.toMutableSet()
        for (start in found.sorted()) {
            if (merged.none { kotlin.math.abs(it - start) < streakDays }) merged += start
        }
        return merged
    }

    /** Null when there is nothing meaningful to show, such as unlocks on devices that can't count them. */
    fun progress(
        badge: UsageBadge,
        history: List<DayUsage>,
        today: Long,
        firstDayOfWeek: Int = Calendar.MONDAY
    ): BadgeProgress? {
        val byDay = history.associateBy { it.epochDay }
        return when (val rule = badge.rule) {
            is BadgeRule.Limit -> when {
                rule.streakDays > 1 -> {
                    var day = if (badge.kind == BadgeKind.WARNING && byDay[today]?.let { qualifies(badge, rule, it, today) } == true) today else today - 1
                    var run = 0L
                    while (byDay[day]?.let { qualifies(badge, rule, it, today) } == true) {
                        run++
                        day--
                    }
                    BadgeProgress(ProgressScope.STREAK, run % rule.streakDays, rule.streakDays.toLong())
                }
                rule.span == Span.DAY -> {
                    if (badge.kind == BadgeKind.ACHIEVEMENT && rule.measure == Measure.NIGHT_USE) return null
                    val current = byDay[today]?.let { rawValue(rule.measure, it) } ?: return null
                    BadgeProgress(ProgressScope.TODAY, current, rule.threshold)
                }
                else -> {
                    val range = spanRange(rule.span, today, firstDayOfWeek)
                    val current = (range.first..today).sumOf { byDay[it]?.let { day -> rawValue(rule.measure, day) } ?: 0L }
                    val scope = if (rule.span == Span.WEEK) ProgressScope.THIS_WEEK else ProgressScope.THIS_MONTH
                    BadgeProgress(scope, current, rule.threshold)
                }
            }
            is BadgeRule.WeeklyDrop -> {
                val week = spanRange(Span.WEEK, today, firstDayOfWeek)
                val previous = fullTotal(shift(week, -7), byDay, today) ?: return null
                val current = (week.first..today).sumOf { byDay[it]?.totalMillis ?: 0L }
                BadgeProgress(ProgressScope.THIS_WEEK, current, dropTarget(previous, rule.percent))
            }
        }
    }

    /** The calendar week or month holding [epochDay]. */
    fun spanRange(span: Span, epochDay: Long, firstDayOfWeek: Int = Calendar.MONDAY): LongRange = when (span) {
        Span.DAY -> epochDay..epochDay
        // Epoch day 0 was a Thursday (Calendar.THURSDAY == 5).
        Span.WEEK -> (epochDay - Math.floorMod(epochDay + 5 - firstDayOfWeek, 7)).let { it..it + 6 }
        Span.MONTH -> {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = epochDay * DAY_MS }
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val start = epochDay - dayOfMonth + 1
            start..start + calendar.getActualMaximum(Calendar.DAY_OF_MONTH) - 1
        }
    }

    private fun qualifies(badge: UsageBadge, rule: BadgeRule.Limit, day: DayUsage, today: Long): Boolean {
        val value = trackedValue(rule.measure, day) ?: return false
        return when (badge.kind) {
            BadgeKind.ACHIEVEMENT -> day.epochDay < today && value < rule.threshold
            BadgeKind.WARNING -> value >= rule.threshold
        }
    }

    private fun spanQualifies(
        badge: UsageBadge,
        rule: BadgeRule.Limit,
        range: LongRange,
        byDay: Map<Long, DayUsage>,
        today: Long
    ): Boolean = when (badge.kind) {
        BadgeKind.ACHIEVEMENT -> (fullTotal(range, byDay, today) ?: Long.MAX_VALUE) < rule.threshold
        BadgeKind.WARNING -> range.sumOf { byDay[it]?.let { day -> trackedValue(rule.measure, day) } ?: 0L } >= rule.threshold
    }

    /** Total screen time of a finished period, or null if any day in it has no data. */
    private fun fullTotal(range: LongRange, byDay: Map<Long, DayUsage>, today: Long): Long? {
        if (range.last >= today) return null
        return range.sumOf { byDay[it]?.let { day -> trackedValue(Measure.SCREEN_TIME, day) } ?: return null }
    }

    /** Starts of each back-to-back block of [BadgeRule.Limit.streakDays] qualifying days. */
    private fun streakStarts(badge: UsageBadge, rule: BadgeRule.Limit, history: List<DayUsage>, today: Long): Set<Long> {
        val days = history.filter { qualifies(badge, rule, it, today) }.map { it.epochDay }.sorted()
        val starts = mutableSetOf<Long>()
        var runStart = 0L
        var previous: Long? = null
        for (day in days) {
            if (previous == null || day != previous + 1) runStart = day
            if ((day - runStart + 1) % rule.streakDays == 0L) starts += day - rule.streakDays + 1
            previous = day
        }
        return starts
    }

    /** The day's value, or null when the day has no data for [measure]. */
    private fun trackedValue(measure: Measure, day: DayUsage): Long? {
        if (day.totalMillis <= 0) return null
        if (measure == Measure.NIGHT_USE && day.hourlyMinutes.sum() == 0) return null
        return rawValue(measure, day)
    }

    private fun rawValue(measure: Measure, day: DayUsage): Long? = when (measure) {
        Measure.SCREEN_TIME -> day.totalMillis
        Measure.UNLOCKS -> day.unlocks?.toLong()
        Measure.TOP_APP -> day.apps.maxOfOrNull { it.millis } ?: 0L
        Measure.NIGHT_USE -> day.hourlyMinutes.take(NIGHT_END_HOUR).sum() * 60_000L
    }

    private fun dropTarget(previous: Long, percent: Int): Long = previous * (100 - percent) / 100

    private fun shift(range: LongRange, days: Long): LongRange = (range.first + days)..(range.last + days)
}
