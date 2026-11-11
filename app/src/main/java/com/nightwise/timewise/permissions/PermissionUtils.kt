package com.nightwise.timewise.permissions

import android.Manifest
import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nightwise.timewise.limits.AppLimitService

data class PermissionStatus(
    val usageAccess: Boolean = false,
    val notifications: Boolean = false,
    /** The app limit service, which closes apps once their time is up. */
    val accessibility: Boolean = false
) {
    val grantedCount: Int get() = listOf(usageAccess, notifications, accessibility).count { it }
    val totalCount: Int get() = 3
}

object PermissionUtils {

    fun check(context: Context) = PermissionStatus(
        usageAccess = hasUsageAccess(context),
        notifications = hasNotificationPermission(context),
        accessibility = hasAccessibilityService(context)
    )

    fun hasAccessibilityService(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val expected = ComponentName(context, AppLimitService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    /** Opens Accessibility settings, scrolled to TimeWise where the device supports it. */
    fun openAccessibilitySettings(context: Context) {
        val component = ComponentName(context, AppLimitService::class.java).flattenToString()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .putExtra(EXTRA_FRAGMENT_ARG_KEY, component)
            .putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, Bundle().apply { putString(EXTRA_FRAGMENT_ARG_KEY, component) })
        context.launch(intent)
    }

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

    fun openUsageAccessSettings(context: Context) {
        val perApp = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, packageUri(context))
        if (!context.launch(perApp)) context.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
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

    private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
    private const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"
}
