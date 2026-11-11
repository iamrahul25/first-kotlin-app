package com.nightwise.timewise.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.SleepEntry
import com.nightwise.timewise.data.StoredDay
import com.nightwise.timewise.permissions.PermissionUtils

data class UsageSnapshot(
    /** Recent days with full detail, oldest first; the last entry is today. */
    val days: List<DayUsage>,
    /**
     * Long history ending today, oldest first. Days older than [days] come from saved
     * summaries, so they have no hourly breakdown and at most the top apps.
     */
    val history: List<DayUsage>,
    /** Most recent night first. */
    val sleepLog: List<SleepEntry>
) {
    val today: DayUsage get() = days.last()
    val yesterday: DayUsage get() = days[days.lastIndex - 1]
    val thisWeek: List<DayUsage> get() = days.takeLast(7)
    val lastWeek: List<DayUsage> get() = days.dropLast(7).takeLast(7)
    val last30Days: List<DayUsage> get() = history.takeLast(30)
    val previous30Days: List<DayUsage> get() = history.dropLast(30).takeLast(30)

    fun day(epochDay: Long): DayUsage? = history.firstOrNull { it.epochDay == epochDay }
}

class ScreenTimeRepository(private val context: Context) {

    private val store = FocusRepository.get(context)

    /** Reads usage events; call off the main thread. */
    fun load(now: Long = System.currentTimeMillis()): UsageSnapshot {
        val today = FocusRepository.currentEpochDay(now)
        val epochDays = ((today - HISTORY_DAYS + 1)..today).toList()
        val unlocksSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        if (!PermissionUtils.hasUsageAccess(context)) {
            val empty = epochDays.map { DayUsage.empty(it, if (unlocksSupported) 0 else null) }
            return UsageSnapshot(empty, savedHistory(epochDays.first()) + empty, emptyList())
        }

        val dayStarts = epochDays.map(FocusRepository::startOfDayMillis)
        val dayEnds = dayStarts.drop(1) + now
        val events = queryEvents(dayStarts.first() - LOOKBACK_MS, now)
        val sessions = UsageAggregator.sessions(events, dayStarts.first(), now, ignoredPackages())
        val computed = UsageAggregator.summarizeDays(sessions, events, epochDays, dayStarts, dayEnds, unlocksSupported)

        val days = computed.map { day -> mergeWithStore(day, today) }
        val sleepLog = if (unlocksSupported) sleepLog(events, today, now) else emptyList()
        store.pruneOldDays(today)
        return UsageSnapshot(days, savedHistory(epochDays.first()) + days, sleepLog)
    }

    /** Saved summaries for the long-history days before [firstDetailedDay]. */
    private fun savedHistory(firstDetailedDay: Long): List<DayUsage> {
        val today = firstDetailedDay + HISTORY_DAYS - 1
        return ((today - LONG_HISTORY_DAYS + 1) until firstDetailedDay).map { day ->
            store.storedDay(day)?.toDayUsage(day) ?: DayUsage.empty(day, unlocks = null)
        }
    }

    /** Ranked time per app in [start, end], skipping [exclude]. Call off the main thread. */
    fun appTimeBetween(start: Long, end: Long, exclude: Set<String>): List<AppUsage> {
        if (end <= start || !PermissionUtils.hasUsageAccess(context)) return emptyList()
        val events = queryEvents(start - LOOKBACK_MS, end)
        return UsageAggregator.sessions(events, start, end, ignoredPackages() + exclude)
            .groupBy { it.packageName }
            .map { (pkg, sessions) -> AppUsage(pkg, sessions.sumOf { it.durationMillis }) }
            .sortedByDescending { it.millis }
    }

    /**
     * Past days keep whichever version is fuller: Android prunes old events, so a fresh
     * computation of an old day can come up short of what was saved earlier.
     */
    private fun mergeWithStore(day: DayUsage, today: Long): DayUsage {
        if (day.epochDay >= today) return day
        val stored = store.storedDay(day.epochDay)
        if (stored == null || day.totalMillis >= stored.totalMillis) {
            if (day.totalMillis > (stored?.totalMillis ?: 0L)) {
                store.saveDay(day.epochDay, StoredDay(day.totalMillis, day.unlocks, day.apps))
            }
            return day
        }
        return stored.toDayUsage(day.epochDay).copy(
            unlocks = stored.unlocks ?: day.unlocks,
            hourlyMinutes = day.hourlyMinutes
        )
    }

    private fun sleepLog(events: List<RawEvent>, today: Long, now: Long): List<SleepEntry> =
        ((today - SLEEP_LOG_DAYS + 1)..today).reversed().mapNotNull { wakeDay ->
            store.storedSleep(wakeDay)?.let { return@mapNotNull it }
            if (wakeDay < today - RELIABLE_DAYS) return@mapNotNull null
            val windowStart = FocusRepository.startOfDayMillis(wakeDay - 1) + NIGHT_START_MS
            val windowEnd = minOf(FocusRepository.startOfDayMillis(wakeDay) + NIGHT_END_MS, now)
            if (windowEnd <= windowStart) return@mapNotNull null
            val gap = UsageAggregator.longestScreenOff(events, windowStart, windowEnd, MIN_SLEEP_MS)
                ?: return@mapNotNull null
            val nightOver = windowEnd < now
            if (nightOver) store.saveSleep(wakeDay, gap)
            SleepEntry(wakeDay, gap.phoneDownAt, gap.pickedUpAt)
        }

    private fun queryEvents(start: Long, end: Long): List<RawEvent> {
        val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usage.queryEvents(start, end) ?: return emptyList()
        val event = UsageEvents.Event()
        val result = ArrayList<RawEvent>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType in TRACKED_TYPES) {
                result += RawEvent(event.packageName ?: "", event.eventType, event.timeStamp)
            }
        }
        return result
    }

    private fun ignoredPackages(): Set<String> {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val launchers = context.packageManager.queryIntentActivities(home, 0)
            .mapNotNull { it.activityInfo?.packageName }
        return launchers.toSet() + SYSTEM_UI
    }

    companion object {
        const val HISTORY_DAYS = 14
        /** Enough for six months of the monthly trend. */
        const val LONG_HISTORY_DAYS = 180
        private const val HOUR_MS = 60 * 60 * 1000L
        /** Catches apps already open at the start of the first day. */
        private const val LOOKBACK_MS = 6 * HOUR_MS
        /** Android keeps detailed events for about 10 days; older nights may be partial. */
        private const val RELIABLE_DAYS = 6
        private const val SLEEP_LOG_DAYS = 30
        private const val NIGHT_START_MS = 18 * HOUR_MS
        private const val NIGHT_END_MS = 14 * HOUR_MS
        private const val MIN_SLEEP_MS = 2 * HOUR_MS
        private val SYSTEM_UI = setOf("com.android.systemui")
        private val TRACKED_TYPES = setOf(
            EventType.ACTIVITY_RESUMED,
            EventType.ACTIVITY_PAUSED,
            EventType.ACTIVITY_STOPPED,
            EventType.SCREEN_INTERACTIVE,
            EventType.SCREEN_NON_INTERACTIVE,
            EventType.KEYGUARD_SHOWN,
            EventType.KEYGUARD_HIDDEN,
            EventType.DEVICE_SHUTDOWN
        )
    }
}
