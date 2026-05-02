package com.example.weblinkscanner.utils

import android.content.Context
import android.content.SharedPreferences

object ScanLimitNotificationManager {

    private const val PREF_NAME = "scan_limit_notif_prefs"
    private const val KEY_ENABLED = "scan_limit_notification_enabled"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, enabled: Boolean, userId: String = "default") {
        prefs(context).edit().putBoolean("${KEY_ENABLED}_$userId", enabled).apply()
    }

    fun isEnabled(context: Context, userId: String = "default"): Boolean =
        prefs(context).getBoolean("${KEY_ENABLED}_$userId", true) // default ON
}