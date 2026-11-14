package com.nightwise.timewise.data.usage

/** Mirrors the `UsageEvents.Event` type codes so aggregation stays plain Kotlin and testable. */
object EventType {
    const val ACTIVITY_RESUMED = 1
    const val ACTIVITY_PAUSED = 2
    const val SCREEN_INTERACTIVE = 15
    const val SCREEN_NON_INTERACTIVE = 16
    const val KEYGUARD_SHOWN = 17
    const val KEYGUARD_HIDDEN = 18
    const val ACTIVITY_STOPPED = 23
    const val DEVICE_SHUTDOWN = 26
}

data class RawEvent(
    val packageName: String,
    val type: Int,
    val timestamp: Long,
    /** Activity class for activity events; one app can have several on screen in turn. */
    val className: String? = null
)

data class UsageSession(val packageName: String, val start: Long, val end: Long) {
    val durationMillis: Long get() = end - start
}

data class AppUsage(val packageName: String, val millis: Long)

data class DayUsage(
    val epochDay: Long,
    val totalMillis: Long,
    /** Ranked, most used first. */
    val apps: List<AppUsage>,
    /** Null when the device can't report unlocks (Android 8.1 and older). */
    val unlocks: Int?,
    /** Minutes of screen time in each of the 24 local hours. */
    val hourlyMinutes: List<Int>
) {
    companion object {
        fun empty(epochDay: Long, unlocks: Int? = 0) =
            DayUsage(epochDay, 0L, emptyList(), unlocks, List(24) { 0 })
    }
}

/** A continuous screen-off stretch, used as the night's sleep. */
data class SleepGap(val phoneDownAt: Long, val pickedUpAt: Long) {
    val durationMillis: Long get() = pickedUpAt - phoneDownAt
}

object UsageAggregator {

    private const val MERGE_GAP_MS = 2_000L
    private const val HOUR_MS = 60 * 60 * 1000L

    /**
     * Rebuilds foreground sessions, counting one app at a time so the total never exceeds
     * screen-on time. An app stays in front until none of its activities are resumed:
     * moving between screens stops the old activity only after the new one resumes.
     * Packages in [ignored] (launchers, system UI) are never counted, and don't end the
     * current session either, since a floating or split-screen app stays visible over them.
     */
    fun sessions(
        events: List<RawEvent>,
        windowStart: Long,
        windowEnd: Long,
        ignored: Set<String> = emptySet()
    ): List<UsageSession> {
        val result = mutableListOf<UsageSession>()
        var currentPkg: String? = null
        var currentStart = 0L
        val resumedActivities = mutableSetOf<String?>()

        fun close(at: Long) {
            val pkg = currentPkg ?: return
            val start = maxOf(currentStart, windowStart)
            val end = minOf(at, windowEnd)
            if (end > start) {
                val last = result.lastOrNull()
                if (last != null && last.packageName == pkg && start - last.end <= MERGE_GAP_MS) {
                    result[result.lastIndex] = last.copy(end = end)
                } else {
                    result += UsageSession(pkg, start, end)
                }
            }
            currentPkg = null
            resumedActivities.clear()
        }

        for (event in events.sortedBy { it.timestamp }) {
            if (event.timestamp > windowEnd) break
            when (event.type) {
                EventType.ACTIVITY_RESUMED -> {
                    if (event.packageName in ignored) continue
                    if (event.packageName != currentPkg) {
                        close(event.timestamp)
                        currentPkg = event.packageName
                        currentStart = event.timestamp
                    }
                    resumedActivities += event.className
                }
                EventType.ACTIVITY_PAUSED, EventType.ACTIVITY_STOPPED -> {
                    if (event.packageName != currentPkg) continue
                    resumedActivities -= event.className
                    if (resumedActivities.isEmpty()) close(event.timestamp)
                }
                EventType.SCREEN_NON_INTERACTIVE, EventType.KEYGUARD_SHOWN, EventType.DEVICE_SHUTDOWN ->
                    close(event.timestamp)
            }
        }
        close(windowEnd)
        return result
    }

    /** Splits sessions into the given day windows; [dayStarts] must be ascending, one per day. */
    fun summarizeDays(
        sessions: List<UsageSession>,
        events: List<RawEvent>,
        epochDays: List<Long>,
        dayStarts: List<Long>,
        dayEnds: List<Long>,
        unlocksSupported: Boolean
    ): List<DayUsage> = epochDays.indices.map { i ->
        val start = dayStarts[i]
        val end = dayEnds[i]
        val perApp = mutableMapOf<String, Long>()
        val hourly = LongArray(24)
        for (session in sessions) {
            val s = maxOf(session.start, start)
            val e = minOf(session.end, end)
            if (e <= s) continue
            perApp[session.packageName] = (perApp[session.packageName] ?: 0L) + (e - s)
            addToHours(hourly, start, s, e)
        }
        DayUsage(
            epochDay = epochDays[i],
            totalMillis = perApp.values.sum(),
            apps = perApp.map { AppUsage(it.key, it.value) }.sortedByDescending { it.millis },
            unlocks = if (unlocksSupported) countUnlocks(events, start, end) else null,
            hourlyMinutes = hourly.map { (it / 60_000L).toInt() }
        )
    }

    /** Unlocks, falling back to screen-ons on devices without a secure lock screen. */
    fun countUnlocks(events: List<RawEvent>, start: Long, end: Long): Int {
        val inWindow = events.filter { it.timestamp in start until end }
        val unlocks = inWindow.count { it.type == EventType.KEYGUARD_HIDDEN }
        if (unlocks > 0) return unlocks
        return inWindow.count { it.type == EventType.SCREEN_INTERACTIVE }
    }

    /** Longest screen-off stretch inside the window, or null if none reaches [minMillis]. */
    fun longestScreenOff(
        events: List<RawEvent>,
        windowStart: Long,
        windowEnd: Long,
        minMillis: Long
    ): SleepGap? {
        var offSince: Long? = null
        var best: SleepGap? = null
        for (event in events.sortedBy { it.timestamp }) {
            if (event.timestamp !in windowStart..windowEnd) continue
            when (event.type) {
                EventType.SCREEN_NON_INTERACTIVE -> if (offSince == null) offSince = event.timestamp
                EventType.SCREEN_INTERACTIVE -> {
                    val since = offSince ?: continue
                    val gap = SleepGap(since, event.timestamp)
                    if (best == null || gap.durationMillis > best.durationMillis) best = gap
                    offSince = null
                }
            }
        }
        return best?.takeIf { it.durationMillis >= minMillis }
    }

    private fun addToHours(hourly: LongArray, dayStart: Long, from: Long, to: Long) {
        var cursor = from
        while (cursor < to) {
            val hour = ((cursor - dayStart) / HOUR_MS).toInt().coerceIn(0, 23)
            val hourEnd = if (hour == 23) to else minOf(to, dayStart + (hour + 1) * HOUR_MS)
            hourly[hour] += hourEnd - cursor
            cursor = hourEnd
        }
    }
}
