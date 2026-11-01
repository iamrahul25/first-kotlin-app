package com.you.reelblocker.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.you.reelblocker.data.FocusRepository
import com.you.reelblocker.service.AccessibilityUtils

data class PermissionStatus(
    val accessibility: Boolean = false,
    val notifications: Boolean = false,
    val usageAccess: Boolean = false,
    val battery: Boolean = false
) {
    val grantedCount: Int get() = listOf(accessibility, notifications, usageAccess, battery).count { it }
    val totalCount: Int get() = 4
}

object PermissionUtils {

    fun check(context: Context) = PermissionStatus(
        accessibility = AccessibilityUtils.isAccessibilityServiceEnabled(context),
        notifications = hasNotificationPermission(context),
        usageAccess = hasUsageAccess(context),
        battery = isIgnoringBatteryOptimizations(context)
    )

    fun hasNotificationPermission(context: Context): Boolean {
        val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openAccessibilitySettings(context: Context) {
        context.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun openUsageAccessSettings(context: Context) {
        val perApp = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, packageUri(context))
        if (!context.launch(perApp)) context.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri(context))
        if (!context.launch(request)) {
            context.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))
        }
        if (!context.launch(intent)) openAppDetails(context)
    }

    fun openAppDetails(context: Context) {
        context.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context)))
    }

    /** Foreground time today per package, computed from usage events. Requires Usage Access. */
    fun foregroundTimeToday(context: Context, packageNames: Set<String>): Map<String, Long> {
        if (!hasUsageAccess(context)) return emptyMap()
        val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usage.queryEvents(FocusRepository.startOfTodayMillis(now), now)
        val event = UsageEvents.Event()
        val openedAt = mutableMapOf<String, Long>()
        val totals = packageNames.associateWith { 0L }.toMutableMap()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            if (pkg !in packageNames) continue
            @Suppress("DEPRECATION")
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> openedAt[pkg] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> openedAt.remove(pkg)?.let { start ->
                    totals[pkg] = (totals[pkg] ?: 0L) + (event.timeStamp - start)
                }
            }
        }
        openedAt.forEach { (pkg, start) -> totals[pkg] = (totals[pkg] ?: 0L) + (now - start) }
        return totals
    }

    private fun packageUri(context: Context): Uri = Uri.fromParts("package", context.packageName, null)

    private fun Context.launch(intent: Intent): Boolean = try {
        if (this !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
