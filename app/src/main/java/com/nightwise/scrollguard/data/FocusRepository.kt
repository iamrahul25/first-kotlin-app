package com.nightwise.scrollguard.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.TimeZone

data class FocusSettings(
    val onboardingDone: Boolean = false,
    val protectionEnabled: Boolean = true,
    val youtubeBlocked: Boolean = true,
    val instagramBlocked: Boolean = true,
    val snapchatBlocked: Boolean = true,
    val dailyTimerEnabled: Boolean = false,
    val dailyTimerMinutes: Int = 120,
    val reelsLimitEnabled: Boolean = false,
    val reelsLimit: Int = 50
) {
    val hasAnyLimit: Boolean get() = dailyTimerEnabled || reelsLimitEnabled

    fun isBlocked(platform: Platform): Boolean = when (platform) {
        Platform.YOUTUBE -> youtubeBlocked
        Platform.INSTAGRAM -> instagramBlocked
        Platform.SNAPCHAT -> snapchatBlocked
        else -> false
    }

    fun withBlocked(platform: Platform, blocked: Boolean): FocusSettings = when (platform) {
        Platform.YOUTUBE -> copy(youtubeBlocked = blocked)
        Platform.INSTAGRAM -> copy(instagramBlocked = blocked)
        Platform.SNAPCHAT -> copy(snapchatBlocked = blocked)
        else -> this
    }
}

data class DayStats(
    val epochDay: Long,
    val reels: Int = 0,
    val seconds: Long = 0,
    val blocked: Int = 0
)

data class FocusStats(
    /** The last [HISTORY_DAYS] days, oldest first; the last entry is today. */
    val days: List<DayStats>,
    val blockedTotal: Int
) {
    val today: DayStats get() = days.last()
    val thisWeek: List<DayStats> get() = days.takeLast(7)
    val lastWeek: List<DayStats> get() = days.dropLast(7).takeLast(7)

    val thisWeekSeconds: Long get() = thisWeek.sumOf { it.seconds }
    val lastWeekSeconds: Long get() = lastWeek.sumOf { it.seconds }
    val thisWeekReels: Int get() = thisWeek.sumOf { it.reels }
    val lastWeekReels: Int get() = lastWeek.sumOf { it.reels }

    companion object {
        const val HISTORY_DAYS = 14
    }
}

/**
 * Single source of truth for settings and usage stats. Shared by the UI and the
 * accessibility service (same process), backed by SharedPreferences so the service
 * can read it synchronously.
 */
class FocusRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<FocusSettings> = _settings.asStateFlow()

    private val _stats = MutableStateFlow(readStats())
    val stats: StateFlow<FocusStats> = _stats.asStateFlow()

    @Synchronized
    fun updateSettings(transform: (FocusSettings) -> FocusSettings) {
        val updated = transform(_settings.value)
        prefs.edit {
            putBoolean(KEY_ONBOARDING_DONE, updated.onboardingDone)
            putBoolean(KEY_PROTECTION, updated.protectionEnabled)
            putBoolean(KEY_YOUTUBE, updated.youtubeBlocked)
            putBoolean(KEY_INSTAGRAM, updated.instagramBlocked)
            putBoolean(KEY_SNAPCHAT, updated.snapchatBlocked)
            putBoolean(KEY_TIMER_ENABLED, updated.dailyTimerEnabled)
            putInt(KEY_TIMER_MINUTES, updated.dailyTimerMinutes)
            putBoolean(KEY_REELS_ENABLED, updated.reelsLimitEnabled)
            putInt(KEY_REELS_LIMIT, updated.reelsLimit)
        }
        _settings.value = updated
    }

    @Synchronized
    fun recordReel() {
        val day = currentEpochDay()
        prefs.edit { putInt(dayKey(day, "reels"), prefs.getInt(dayKey(day, "reels"), 0) + 1) }
        refresh()
    }

    @Synchronized
    fun addWatchSeconds(seconds: Long) {
        if (seconds <= 0) return
        val day = currentEpochDay()
        prefs.edit { putLong(dayKey(day, "secs"), prefs.getLong(dayKey(day, "secs"), 0L) + seconds) }
        refresh()
    }

    @Synchronized
    fun recordBlocked() {
        val day = currentEpochDay()
        prefs.edit {
            putInt(dayKey(day, "blocked"), prefs.getInt(dayKey(day, "blocked"), 0) + 1)
            putInt(KEY_BLOCKED_TOTAL, prefs.getInt(KEY_BLOCKED_TOTAL, 0) + 1)
        }
        refresh()
    }

    /** Returns true the first time it is called on a given day. */
    @Synchronized
    fun claimLimitNotification(): Boolean {
        val day = currentEpochDay()
        if (prefs.getLong(KEY_LIMIT_NOTIFIED_DAY, -1L) == day) return false
        prefs.edit { putLong(KEY_LIMIT_NOTIFIED_DAY, day) }
        return true
    }

    /** Re-reads stats, e.g. after midnight or when the app comes to the foreground. */
    @Synchronized
    fun refresh() {
        _stats.value = readStats()
    }

    private fun readSettings(): FocusSettings {
        val defaults = FocusSettings()
        return FocusSettings(
            onboardingDone = prefs.getBoolean(KEY_ONBOARDING_DONE, defaults.onboardingDone),
            protectionEnabled = prefs.getBoolean(KEY_PROTECTION, defaults.protectionEnabled),
            youtubeBlocked = prefs.getBoolean(KEY_YOUTUBE, defaults.youtubeBlocked),
            instagramBlocked = prefs.getBoolean(KEY_INSTAGRAM, defaults.instagramBlocked),
            snapchatBlocked = prefs.getBoolean(KEY_SNAPCHAT, defaults.snapchatBlocked),
            dailyTimerEnabled = prefs.getBoolean(KEY_TIMER_ENABLED, defaults.dailyTimerEnabled),
            dailyTimerMinutes = prefs.getInt(KEY_TIMER_MINUTES, defaults.dailyTimerMinutes),
            reelsLimitEnabled = prefs.getBoolean(KEY_REELS_ENABLED, defaults.reelsLimitEnabled),
            reelsLimit = prefs.getInt(KEY_REELS_LIMIT, defaults.reelsLimit)
        )
    }

    private fun readStats(): FocusStats {
        val today = currentEpochDay()
        val days = ((today - FocusStats.HISTORY_DAYS + 1)..today).map { day ->
            DayStats(
                epochDay = day,
                reels = prefs.getInt(dayKey(day, "reels"), 0),
                seconds = prefs.getLong(dayKey(day, "secs"), 0L),
                blocked = prefs.getInt(dayKey(day, "blocked"), 0)
            )
        }
        pruneOldDays(today)
        return FocusStats(days = days, blockedTotal = prefs.getInt(KEY_BLOCKED_TOTAL, 0))
    }

    private fun pruneOldDays(today: Long) {
        val cutoff = today - RETENTION_DAYS
        val stale = prefs.all.keys.filter { key ->
            key.startsWith(DAY_PREFIX) &&
                (key.removePrefix(DAY_PREFIX).substringBefore('_').toLongOrNull() ?: today) < cutoff
        }
        if (stale.isNotEmpty()) prefs.edit { stale.forEach(::remove) }
    }

    private fun dayKey(day: Long, field: String) = "$DAY_PREFIX${day}_$field"

    companion object {
        private const val PREFS_NAME = "focusblock"
        private const val DAY_PREFIX = "day_"
        private const val RETENTION_DAYS = 30
        private const val DAY_MS = 24L * 60 * 60 * 1000

        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_PROTECTION = "protection_enabled"
        private const val KEY_YOUTUBE = "block_youtube"
        private const val KEY_INSTAGRAM = "block_instagram"
        private const val KEY_SNAPCHAT = "block_snapchat"
        private const val KEY_TIMER_ENABLED = "daily_timer_enabled"
        private const val KEY_TIMER_MINUTES = "daily_timer_minutes"
        private const val KEY_REELS_ENABLED = "reels_limit_enabled"
        private const val KEY_REELS_LIMIT = "reels_limit"
        private const val KEY_BLOCKED_TOTAL = "blocked_total"
        private const val KEY_LIMIT_NOTIFIED_DAY = "limit_notified_day"

        @Volatile
        private var instance: FocusRepository? = null

        fun get(context: Context): FocusRepository =
            instance ?: synchronized(this) {
                instance ?: FocusRepository(context).also { instance = it }
            }

        /** Days since epoch in the device's local time zone. */
        fun currentEpochDay(now: Long = System.currentTimeMillis()): Long =
            Math.floorDiv(now + TimeZone.getDefault().getOffset(now), DAY_MS)

        fun startOfTodayMillis(now: Long = System.currentTimeMillis()): Long =
            currentEpochDay(now) * DAY_MS - TimeZone.getDefault().getOffset(now)

        fun epochDayToUtcMillis(epochDay: Long): Long = epochDay * DAY_MS
    }
}
