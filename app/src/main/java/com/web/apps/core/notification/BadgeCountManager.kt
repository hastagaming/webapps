package com.web.apps.core.notification

import android.content.Context
import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BadgeCountManager @Inject constructor() {

    private val prefsName = "badge_count_prefs"
    private val countKey = "unread_count"

    fun incrementBadge(context: Context) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val newCount = prefs.getInt(countKey, 0) + 1
        prefs.edit().putInt(countKey, newCount).apply()
        applyBadge(context, newCount)
    }

    fun clearBadge(context: Context) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().putInt(countKey, 0).apply()
        applyBadge(context, 0)
    }

    fun getCurrentCount(context: Context): Int {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return prefs.getInt(countKey, 0)
    }

    private fun applyBadge(context: Context, count: Int) {
        try {
            val launcherClassName = getLauncherClassName(context) ?: return

            val intent = Intent("android.intent.action.BADGE_COUNT_UPDATE").apply {
                putExtra("badge_count", count)
                putExtra("badge_count_package_name", context.packageName)
                putExtra("badge_count_class_name", launcherClassName)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            // Beberapa launcher tidak mendukung; abaikan tanpa crash
        }
    }

    private fun getLauncherClassName(context: Context): String? {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return intent.component?.className
    }
}