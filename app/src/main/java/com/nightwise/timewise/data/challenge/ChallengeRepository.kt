package com.nightwise.timewise.data.challenge

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.usage.AppUsage
import com.nightwise.timewise.data.usage.ScreenTimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChallengeState(
    val active: ActiveChallenge?,
    /** Most recent first. */
    val history: List<ChallengeResult>
)

/** Progress of the running challenge: time spent in other apps since it started. */
data class ChallengeProgress(val distractionMillis: Long, val topApp: AppUsage?)

/** What happened when a challenge ended, including badges unlocked by it. */
data class ChallengeOutcome(val result: ChallengeResult, val progress: ChallengeProgress, val newBadges: Set<Badge>)

class ChallengeRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val screenTime = ScreenTimeRepository(context)
    private val ownPackage = context.packageName

    private val _state = MutableStateFlow(read())
    val state: StateFlow<ChallengeState> = _state.asStateFlow()

    @Synchronized
    fun start(type: ChallengeType, now: Long = System.currentTimeMillis()): ActiveChallenge {
        val active = ActiveChallenge(type, now)
        prefs.edit {
            putString(KEY_ACTIVE_TYPE, type.name)
            putLong(KEY_ACTIVE_START, now)
        }
        _state.value = _state.value.copy(active = active)
        return active
    }

    /** Time in other apps since the challenge started. Reads usage events; call off the main thread. */
    fun progress(active: ActiveChallenge, now: Long = System.currentTimeMillis()): ChallengeProgress {
        val apps = screenTime.appTimeBetween(active.startAt, minOf(now, active.endAt), exclude = setOf(ownPackage))
        return ChallengeProgress(apps.sumOf { it.millis }, apps.firstOrNull())
    }

    /**
     * Ends the running challenge if it is over or already failed. Returns null if it is
     * still going or another caller already finished it.
     */
    fun evaluate(now: Long = System.currentTimeMillis()): ChallengeOutcome? {
        val active = _state.value.active ?: return null
        val progress = progress(active, now)
        val failedEarly = !ChallengeLogic.isSuccess(active.type, progress.distractionMillis)
        if (now < active.endAt && !failedEarly) return null
        val success = !failedEarly
        return finish(active, success, if (success) active.endAt else now, progress)
    }

    fun giveUp(now: Long = System.currentTimeMillis()): ChallengeOutcome? {
        val active = _state.value.active ?: return null
        return finish(active, success = false, endAt = now, progress = progress(active, now))
    }

    @Synchronized
    private fun finish(active: ActiveChallenge, success: Boolean, endAt: Long, progress: ChallengeProgress): ChallengeOutcome? {
        if (_state.value.active != active) return null
        val before = ChallengeLogic.earnedBadges(_state.value.history)
        val result = ChallengeResult(active.type, active.startAt, endAt, success, FocusRepository.currentEpochDay(endAt))
        val history = (listOf(result) + _state.value.history).take(MAX_HISTORY)
        prefs.edit {
            remove(KEY_ACTIVE_TYPE)
            remove(KEY_ACTIVE_START)
            putString(KEY_HISTORY, history.joinToString(RECORD_SEPARATOR, transform = ::encode))
        }
        _state.value = ChallengeState(active = null, history = history)
        return ChallengeOutcome(result, progress, ChallengeLogic.earnedBadges(history) - before)
    }

    private fun read(): ChallengeState {
        val type = ChallengeType.fromName(prefs.getString(KEY_ACTIVE_TYPE, null))
        val start = prefs.getLong(KEY_ACTIVE_START, -1L)
        val active = if (type != null && start > 0) ActiveChallenge(type, start) else null
        val history = prefs.getString(KEY_HISTORY, null).orEmpty()
            .split(RECORD_SEPARATOR)
            .mapNotNull(::decode)
        return ChallengeState(active, history)
    }

    private fun encode(result: ChallengeResult): String = listOf(
        result.type.name, result.startAt, result.endAt, if (result.success) 1 else 0, result.epochDay
    ).joinToString(FIELD_SEPARATOR)

    private fun decode(record: String): ChallengeResult? {
        val fields = record.split(FIELD_SEPARATOR)
        if (fields.size != 5) return null
        return ChallengeResult(
            type = ChallengeType.fromName(fields[0]) ?: return null,
            startAt = fields[1].toLongOrNull() ?: return null,
            endAt = fields[2].toLongOrNull() ?: return null,
            success = fields[3] == "1",
            epochDay = fields[4].toLongOrNull() ?: return null
        )
    }

    companion object {
        private const val PREFS_NAME = "timewise_challenges"
        private const val KEY_ACTIVE_TYPE = "active_type"
        private const val KEY_ACTIVE_START = "active_start"
        private const val KEY_HISTORY = "history"
        private const val RECORD_SEPARATOR = ";"
        private const val FIELD_SEPARATOR = ","
        private const val MAX_HISTORY = 500

        @Volatile
        private var instance: ChallengeRepository? = null

        fun get(context: Context): ChallengeRepository =
            instance ?: synchronized(this) {
                instance ?: ChallengeRepository(context.applicationContext).also { instance = it }
            }
    }
}
