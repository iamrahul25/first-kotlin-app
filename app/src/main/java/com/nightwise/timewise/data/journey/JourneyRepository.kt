package com.nightwise.timewise.data.journey

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.usage.UsageSnapshot
import com.nightwise.timewise.permissions.PermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class JourneyState(
    val installedAt: Long,
    /** First time TimeWise saw the permission on; Android doesn't say when it was actually granted. */
    val usageAccessSince: Long?,
    val notificationsSince: Long?,
    val baseline: Baseline?
) {
    val installDay: Long get() = FocusRepository.currentEpochDay(installedAt)
}

/** When the user started with TimeWise and how they used their phone before, to measure the difference. */
class JourneyRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read(context))
    val state: StateFlow<JourneyState> = _state.asStateFlow()

    @Synchronized
    fun notePermissions(status: PermissionStatus, now: Long = System.currentTimeMillis()) {
        val current = _state.value
        val updated = current.copy(
            usageAccessSince = current.usageAccessSince ?: now.takeIf { status.usageAccess },
            notificationsSince = current.notificationsSince ?: now.takeIf { status.notifications }
        )
        if (updated == current) return
        prefs.edit {
            updated.usageAccessSince?.let { putLong(KEY_USAGE_SINCE, it) }
            updated.notificationsSince?.let { putLong(KEY_NOTIFICATIONS_SINCE, it) }
        }
        _state.value = updated
    }

    /** Measures the baseline once enough history exists; later calls keep the first result. */
    @Synchronized
    fun captureBaseline(usage: UsageSnapshot) {
        val current = _state.value
        if (current.baseline != null) return
        val baseline = JourneyLogic.baseline(usage.history, current.installDay, usage.today.epochDay) ?: return
        prefs.edit {
            putLong(KEY_BASELINE_AVERAGE, baseline.dailyAverageMillis)
            putInt(KEY_BASELINE_DAYS, baseline.days)
            putInt(KEY_BASELINE_UNLOCKS, baseline.unlocksPerDay ?: -1)
            putString(KEY_BASELINE_SOURCE, baseline.source.name)
        }
        _state.value = current.copy(baseline = baseline)
    }

    private fun read(context: Context): JourneyState {
        val installedAt = prefs.getLong(KEY_INSTALLED_AT, -1L).takeIf { it > 0 } ?: run {
            val first = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
            }.getOrNull()?.takeIf { it > 0 } ?: System.currentTimeMillis()
            prefs.edit { putLong(KEY_INSTALLED_AT, first) }
            first
        }
        val source = prefs.getString(KEY_BASELINE_SOURCE, null)?.let { name ->
            BaselineSource.entries.firstOrNull { it.name == name }
        }
        val baseline = source?.let {
            Baseline(
                dailyAverageMillis = prefs.getLong(KEY_BASELINE_AVERAGE, 0L),
                days = prefs.getInt(KEY_BASELINE_DAYS, 0),
                unlocksPerDay = prefs.getInt(KEY_BASELINE_UNLOCKS, -1).takeIf { it >= 0 },
                source = it
            )
        }
        return JourneyState(
            installedAt = installedAt,
            usageAccessSince = prefs.getLong(KEY_USAGE_SINCE, -1L).takeIf { it > 0 },
            notificationsSince = prefs.getLong(KEY_NOTIFICATIONS_SINCE, -1L).takeIf { it > 0 },
            baseline = baseline
        )
    }

    companion object {
        private const val PREFS_NAME = "timewise_journey"
        private const val KEY_INSTALLED_AT = "installed_at"
        private const val KEY_USAGE_SINCE = "usage_access_since"
        private const val KEY_NOTIFICATIONS_SINCE = "notifications_since"
        private const val KEY_BASELINE_AVERAGE = "baseline_average"
        private const val KEY_BASELINE_DAYS = "baseline_days"
        private const val KEY_BASELINE_UNLOCKS = "baseline_unlocks"
        private const val KEY_BASELINE_SOURCE = "baseline_source"

        @Volatile
        private var instance: JourneyRepository? = null

        fun get(context: Context): JourneyRepository =
            instance ?: synchronized(this) {
                instance ?: JourneyRepository(context.applicationContext).also { instance = it }
            }
    }
}
