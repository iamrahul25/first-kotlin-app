package com.nightwise.timewise.data.challenge

enum class ChallengeType(
    val label: String,
    val durationMillis: Long,
    /** Screen time in other apps allowed before the challenge fails. */
    val graceMillis: Long
) {
    ONE_HOUR("1-hour focus", 60 * MINUTE, 5 * MINUTE),
    EIGHT_HOURS("8-hour focus", 8 * 60 * MINUTE, 30 * MINUTE),
    ONE_DAY("1-day detox", 24 * 60 * MINUTE, 60 * MINUTE);

    val durationMinutes: Int get() = (durationMillis / MINUTE).toInt()
    val graceMinutes: Int get() = (graceMillis / MINUTE).toInt()

    companion object {
        fun fromName(name: String?): ChallengeType? = entries.firstOrNull { it.name == name }
    }
}

private const val MINUTE = 60_000L

data class ActiveChallenge(val type: ChallengeType, val startAt: Long) {
    val endAt: Long get() = startAt + type.durationMillis

    fun remainingMillis(now: Long): Long = (endAt - now).coerceIn(0L, type.durationMillis)

    /** Share of the challenge already behind you, 0 to 1. */
    fun elapsedFraction(now: Long): Float = 1f - remainingMillis(now).toFloat() / type.durationMillis
}

data class ChallengeResult(
    val type: ChallengeType,
    val startAt: Long,
    val endAt: Long,
    val success: Boolean,
    /** Epoch day (local) the challenge ended on; streaks are counted in these days. */
    val epochDay: Long
)

enum class Badge(val title: String, val description: String) {
    FIRST_FOCUS("First Focus", "Finish your first focus challenge"),
    STREAK_3("3-Day Streak", "Finish a challenge 3 days in a row"),
    FIVE_SESSIONS("Focused Five", "Finish 5 focus challenges"),
    STREAK_7("7-Day Streak", "Finish a challenge 7 days in a row")
}

object ChallengeLogic {

    fun isSuccess(type: ChallengeType, distractionMillis: Long): Boolean = distractionMillis <= type.graceMillis

    /** Consecutive days with a win, ending today, or yesterday if today has none yet. */
    fun currentStreak(results: List<ChallengeResult>, today: Long): Int {
        val days = successDays(results)
        var day = if (today in days) today else today - 1
        var streak = 0
        while (day in days) {
            streak++
            day--
        }
        return streak
    }

    fun longestStreak(results: List<ChallengeResult>): Int {
        val days = successDays(results).sorted()
        var best = 0
        var run = 0
        var previous: Long? = null
        for (day in days) {
            run = if (previous != null && day == previous + 1) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        return best
    }

    fun earnedBadges(results: List<ChallengeResult>): Set<Badge> {
        val wins = results.count { it.success }
        val longest = longestStreak(results)
        return buildSet {
            if (wins >= 1) add(Badge.FIRST_FOCUS)
            if (longest >= 3) add(Badge.STREAK_3)
            if (wins >= 5) add(Badge.FIVE_SESSIONS)
            if (longest >= 7) add(Badge.STREAK_7)
        }
    }

    private fun successDays(results: List<ChallengeResult>): Set<Long> =
        results.filter { it.success }.map { it.epochDay }.toSet()
}
