package com.nightwise.timewise.data.badges

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.nightwise.timewise.data.usage.UsageSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class BadgeRecord(
    /** Epoch days the earned periods start on. */
    val periods: Set<Long> = emptySet(),
    /** How many of the wins the user has already seen on the badges screen. */
    val seenCount: Int = 0
) {
    val count: Int get() = periods.size
    val earned: Boolean get() = periods.isNotEmpty()
    val lastEarnedDay: Long? get() = periods.maxOrNull()
    val unseen: Int get() = (count - seenCount).coerceAtLeast(0)
}

/**
 * Lifetime ledger of screen time badges. Saved history only reaches back about a year,
 * so wins are kept here once found and counts never go down.
 */
class BadgeRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _records = MutableStateFlow(read())
    val records: StateFlow<Map<UsageBadge, BadgeRecord>> = _records.asStateFlow()

    /** Adds every badge earned in [usage] to the ledger. */
    @Synchronized
    fun record(usage: UsageSnapshot) {
        val today = usage.today.epochDay
        val firstDayOfWeek = Calendar.getInstance().firstDayOfWeek
        val current = _records.value
        val updated = UsageBadge.entries.associateWith { badge ->
            val record = current[badge] ?: BadgeRecord()
            val found = BadgeEngine.earnedPeriods(badge, usage.history, today, firstDayOfWeek)
            record.copy(periods = BadgeEngine.merge(badge, record.periods, found))
        }
        if (updated == current) return
        prefs.edit {
            updated.forEach { (badge, record) ->
                if (record.periods != current[badge]?.periods) {
                    putStringSet(periodsKey(badge), record.periods.mapTo(HashSet()) { it.toString() })
                }
            }
        }
        _records.value = updated
    }

    @Synchronized
    fun markAllSeen() {
        val current = _records.value
        if (current.values.none { it.unseen > 0 }) return
        prefs.edit { current.forEach { (badge, record) -> putInt(seenKey(badge), record.count) } }
        _records.value = current.mapValues { (_, record) -> record.copy(seenCount = record.count) }
    }

    private fun read(): Map<UsageBadge, BadgeRecord> = UsageBadge.entries.associateWith { badge ->
        BadgeRecord(
            periods = prefs.getStringSet(periodsKey(badge), null).orEmpty().mapNotNullTo(HashSet()) { it.toLongOrNull() },
            seenCount = prefs.getInt(seenKey(badge), 0)
        )
    }

    private fun periodsKey(badge: UsageBadge) = "periods_${badge.name}"
    private fun seenKey(badge: UsageBadge) = "seen_${badge.name}"

    companion object {
        private const val PREFS_NAME = "timewise_badges"

        @Volatile
        private var instance: BadgeRepository? = null

        fun get(context: Context): BadgeRepository =
            instance ?: synchronized(this) {
                instance ?: BadgeRepository(context.applicationContext).also { instance = it }
            }
    }
}
