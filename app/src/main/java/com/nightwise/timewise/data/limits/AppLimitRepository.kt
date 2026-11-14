package com.nightwise.timewise.data.limits

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Per-app daily limits and the extra time earned today. Read by the blocking service too. */
class AppLimitRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<LimitsState> = _state.asStateFlow()

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ENABLED, enabled) }
        _state.value = _state.value.copy(enabled = enabled)
    }

    @Synchronized
    fun setLimit(packageName: String, minutes: Int) {
        val clamped = minutes.coerceIn(LimitLogic.MIN_LIMIT_MINUTES, LimitLogic.MAX_LIMIT_MINUTES)
        val limits = _state.value.limits.filter { it.packageName != packageName } + AppLimit(packageName, clamped)
        saveLimits(limits)
    }

    @Synchronized
    fun removeLimit(packageName: String) {
        saveLimits(_state.value.limits.filter { it.packageName != packageName })
    }

    @Synchronized
    fun grantExtra(packageName: String, epochDay: Long, minutes: Int = LimitLogic.EXTRA_MINUTES) {
        val current = _state.value
        val extras = (if (current.extrasDay == epochDay) current.extras else emptyMap()).toMutableMap()
        extras[packageName] = (extras[packageName] ?: 0) + minutes
        prefs.edit {
            putLong(KEY_EXTRAS_DAY, epochDay)
            putString(KEY_EXTRAS, encode(extras))
        }
        _state.value = current.copy(extrasDay = epochDay, extras = extras)
    }

    private fun saveLimits(limits: List<AppLimit>) {
        val sorted = limits.sortedBy { it.packageName }
        prefs.edit { putString(KEY_LIMITS, encode(sorted.associate { it.packageName to it.minutes })) }
        _state.value = _state.value.copy(limits = sorted)
    }

    private fun read(): LimitsState = LimitsState(
        enabled = prefs.getBoolean(KEY_ENABLED, true),
        limits = decode(prefs.getString(KEY_LIMITS, null)).map { (pkg, minutes) -> AppLimit(pkg, minutes) },
        extrasDay = prefs.getLong(KEY_EXTRAS_DAY, -1L),
        extras = decode(prefs.getString(KEY_EXTRAS, null)).toMap()
    )

    private fun encode(values: Map<String, Int>): String = values.entries.joinToString(",") { "${it.key}=${it.value}" }

    private fun decode(raw: String?): List<Pair<String, Int>> = raw.orEmpty()
        .split(',')
        .mapNotNull { entry ->
            val pkg = entry.substringBefore('=', "")
            val minutes = entry.substringAfter('=', "").toIntOrNull()
            if (pkg.isEmpty() || minutes == null) null else pkg to minutes
        }

    companion object {
        private const val PREFS_NAME = "timewise_limits"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_LIMITS = "limits"
        private const val KEY_EXTRAS_DAY = "extras_day"
        private const val KEY_EXTRAS = "extras"

        @Volatile
        private var instance: AppLimitRepository? = null

        fun get(context: Context): AppLimitRepository =
            instance ?: synchronized(this) {
                instance ?: AppLimitRepository(context.applicationContext).also { instance = it }
            }
    }
}
