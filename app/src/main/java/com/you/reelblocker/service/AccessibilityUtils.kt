package com.you.reelblocker.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object AccessibilityUtils {
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val expectedService = ComponentName(
            context,
            ShortsAccessibilityService::class.java
        ).flattenToString()

        return enabledServices.split(":").any { it == expectedService }
    }
}
