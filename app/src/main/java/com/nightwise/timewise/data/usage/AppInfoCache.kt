package com.nightwise.timewise.data.usage

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap

data class AppInfo(val label: String, val icon: ImageBitmap?)

/** Labels and icons for installed apps, cached for the life of the process. */
object AppInfoCache {

    private const val ICON_PX = 96
    private val cache = ConcurrentHashMap<String, AppInfo>()

    fun get(context: Context, packageName: String): AppInfo = cache.getOrPut(packageName) {
        val pm = context.packageManager
        try {
            val info = pm.getApplicationInfo(packageName, 0)
            val icon = runCatching {
                pm.getApplicationIcon(info).toBitmap(ICON_PX, ICON_PX).asImageBitmap()
            }.getOrNull()
            AppInfo(pm.getApplicationLabel(info).toString(), icon)
        } catch (_: PackageManager.NameNotFoundException) {
            AppInfo(packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }, null)
        }
    }
}
