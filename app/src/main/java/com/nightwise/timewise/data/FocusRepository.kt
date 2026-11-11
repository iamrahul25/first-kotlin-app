package com.nightwise.timewise.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.nightwise.timewise.data.usage.AppUsage
import com.nightwise.timewise.data.usage.DayUsage
import com.nightwise.timewise.data.usage.SleepGap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.TimeZone

data class FocusSettings(
    val onboardingDone: Boolean = false,
    val bedtimeEnabled: Boolean = false,
    /** Minutes after local midnight. */
    val bedtimeMinutes: Int = 23 * 60,
    val wakeMinutes: Int = 7 * 60,
    val reminderLeadMinutes: Int = 15
) {
    /** Planned sleep length, handling schedules that cross midnight. */
    val sleepGoalMinutes: Int get() = Math.floorMod(wakeMinutes - bedtimeMinutes, 24 * 60)
}

/** Day totals saved so history survives after Android prunes its usage events. */
data class StoredDay(val totalMillis: Long, val unlocks: Int?, val topApps: List<AppUsage>) {
    fun toDayUsage(epochDay: Long) = DayUsage(epochDay, totalMillis, topApps, unlocks, List(24) { 0 })
}

/** One night of sleep, keyed by the day the user woke up. */
data class SleepEntry(val wakeEpochDay: Long, val phoneDownAt: Long, val pickedUpAt: Long) {
    val durationMillis: Long get() = pickedUpAt - phoneDownAt
}

/**
 * Settings and saved history, backed by SharedPreferences so broadcast receivers can
 * read it synchronously.
 */
class FocusRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<FocusSettings> = _settings.asStateFlow()

    @Synchronized
    fun updateSettings(transform: (FocusSettings) -> FocusSettings) {
        val updated = transform(_settings.value)
        prefs.edit {
            putBoolean(KEY_ONBOARDING_DONE, updated.onboardingDone)
            putBoolean(KEY_BEDTIME_ENABLED, updated.bedtimeEnabled)
            putInt(KEY_BEDTIME_MINUTES, updated.bedtimeMinutes)
            putInt(KEY_WAKE_MINUTES, updated.wakeMinutes)
            putInt(KEY_REMINDER_LEAD, updated.reminderLeadMinutes)
        }
        _settings.value = updated
    }

    fun storedDay(epochDay: Long): StoredDay? {
        val total = prefs.getLong(dayKey(epochDay, "total"), -1L)
        if (total < 0) return null
        val unlocks = prefs.getInt(dayKey(epochDay, "unlocks"), -1)
        val apps = prefs.getString(dayKey(epochDay, "apps"), null).orEmpty()
            .split(',')
            .mapNotNull { entry ->
                val pkg = entry.substringBefore('=', "")
                val millis = entry.substringAfter('=', "").toLongOrNull()
                if (pkg.isEmpty() || millis == null) null else AppUsage(pkg, millis)
            }
        return StoredDay(total, unlocks.takeIf { it >= 0 }, apps)
    }

    @Synchronized
    fun saveDay(epochDay: Long, day: StoredDay) {
        prefs.edit {
            putLong(dayKey(epochDay, "total"), day.totalMillis)
            putInt(dayKey(epochDay, "unlocks"), day.unlocks ?: -1)
            putString(
                dayKey(epochDay, "apps"),
                day.topApps.take(STORED_APPS_PER_DAY).joinToString(",") { "${it.packageName}=${it.millis}" }
            )
        }
    }

    fun storedSleep(wakeEpochDay: Long): SleepEntry? {
        val down = prefs.getLong(dayKey(wakeEpochDay, "sleep_down"), -1L)
        val up = prefs.getLong(dayKey(wakeEpochDay, "sleep_up"), -1L)
        if (down < 0 || up < 0) return null
        return SleepEntry(wakeEpochDay, down, up)
    }

    @Synchronized
    fun saveSleep(wakeEpochDay: Long, gap: SleepGap) {
        prefs.edit {
            putLong(dayKey(wakeEpochDay, "sleep_down"), gap.phoneDownAt)
            putLong(dayKey(wakeEpochDay, "sleep_up"), gap.pickedUpAt)
        }
    }

    @Synchronized
    fun pruneOldDays(today: Long = currentEpochDay()) {
        val cutoff = today - RETENTION_DAYS
        val stale = prefs.all.keys.filter { key ->
            key.startsWith(DAY_PREFIX) &&
                (key.removePrefix(DAY_PREFIX).substringBefore('_').toLongOrNull() ?: today) < cutoff
        }
        if (stale.isNotEmpty()) prefs.edit { stale.forEach(::remove) }
    }

    private fun readSettings(): FocusSettings {
        val defaults = FocusSettings()
        return FocusSettings(
            onboardingDone = prefs.getBoolean(KEY_ONBOARDING_DONE, defaults.onboardingDone),
            bedtimeEnabled = prefs.getBoolean(KEY_BEDTIME_ENABLED, defaults.bedtimeEnabled),
            bedtimeMinutes = prefs.getInt(KEY_BEDTIME_MINUTES, defaults.bedtimeMinutes),
            wakeMinutes = prefs.getInt(KEY_WAKE_MINUTES, defaults.wakeMinutes),
            reminderLeadMinutes = prefs.getInt(KEY_REMINDER_LEAD, defaults.reminderLeadMinutes)
        )
    }

    private fun dayKey(day: Long, field: String) = "$DAY_PREFIX${day}_$field"

    companion object {
        private const val PREFS_NAME = "timewise"
        private const val DAY_PREFIX = "day_"
        /** Long enough for the monthly graph planned for v2. */
        private const val RETENTION_DAYS = 400
        private const val STORED_APPS_PER_DAY = 10
        private const val DAY_MS = 24L * 60 * 60 * 1000

        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_BEDTIME_ENABLED = "bedtime_enabled"
        private const val KEY_BEDTIME_MINUTES = "bedtime_minutes"
        private const val KEY_WAKE_MINUTES = "wake_minutes"
        private const val KEY_REMINDER_LEAD = "reminder_lead_minutes"

        @Volatile
        private var instance: FocusRepository? = null

        fun get(context: Context): FocusRepository =
            instance ?: synchronized(this) {
                instance ?: FocusRepository(context).also { instance = it }
            }

        /** Days since epoch in the device's local time zone. */
        fun currentEpochDay(now: Long = System.currentTimeMillis()): Long =
            Math.floorDiv(now + TimeZone.getDefault().getOffset(now), DAY_MS)

        /** Local midnight that starts [epochDay], correct across DST changes. */
        fun startOfDayMillis(epochDay: Long): Long {
            val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = epochDay * DAY_MS }
            return Calendar.getInstance().apply {
                clear()
                set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            }.timeInMillis
        }

        fun epochDayToUtcMillis(epochDay: Long): Long = epochDay * DAY_MS
    }
}
