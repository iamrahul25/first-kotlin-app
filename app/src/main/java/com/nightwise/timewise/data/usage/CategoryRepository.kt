package com.nightwise.timewise.data.usage

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/** Which category each app counts under, with the user's own choices saved on top. */
class CategoryRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val declared = ConcurrentHashMap<String, Pair<Int?, Boolean>>()

    private val _overrides = MutableStateFlow(readOverrides())
    val overrides: StateFlow<Map<String, AppCategory>> = _overrides.asStateFlow()

    /** Reads the app's declared category on first use; call off the main thread. */
    fun categoryOf(packageName: String, overrides: Map<String, AppCategory> = _overrides.value): AppCategory {
        val (systemCategory, isGame) = declared.getOrPut(packageName) { readDeclared(packageName) }
        return CategoryLogic.categorize(packageName, overrides[packageName], systemCategory, isGame)
    }

    /** [category] null goes back to the automatic one. */
    @Synchronized
    fun setOverride(packageName: String, category: AppCategory?) {
        val updated = _overrides.value.toMutableMap()
        if (category == null) updated.remove(packageName) else updated[packageName] = category
        prefs.edit { putString(KEY_OVERRIDES, updated.entries.joinToString(",") { "${it.key}=${it.value.name}" }) }
        _overrides.value = updated
    }

    private fun readOverrides(): Map<String, AppCategory> = prefs.getString(KEY_OVERRIDES, null).orEmpty()
        .split(',')
        .mapNotNull { entry ->
            val pkg = entry.substringBefore('=', "")
            val category = AppCategory.fromName(entry.substringAfter('=', ""))
            if (pkg.isEmpty() || category == null) null else pkg to category
        }
        .toMap()

    @Suppress("DEPRECATION")
    private fun readDeclared(packageName: String): Pair<Int?, Boolean> = try {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        val category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            info.category.takeIf { it != ApplicationInfo.CATEGORY_UNDEFINED }
        } else {
            null
        }
        category to (info.flags and ApplicationInfo.FLAG_IS_GAME != 0)
    } catch (_: PackageManager.NameNotFoundException) {
        null to false
    }

    companion object {
        private const val PREFS_NAME = "timewise_categories"
        private const val KEY_OVERRIDES = "overrides"

        @Volatile
        private var instance: CategoryRepository? = null

        fun get(context: Context): CategoryRepository =
            instance ?: synchronized(this) {
                instance ?: CategoryRepository(context.applicationContext).also { instance = it }
            }
    }
}
