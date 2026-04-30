package com.example.weblinkscanner.utils

import android.content.Context
import android.content.SharedPreferences

object AutoLogoutManager {

    private const val PREF_NAME = "auto_logout_prefs"
    private const val KEY_TIMEOUT_MIN = "timeout_minutes"
    private const val DEFAULT_TIMEOUT = 10  // minutes

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Save timeout per user
    fun saveTimeout(context: Context, minutes: Int, userId: String = "default") {
        prefs(context).edit().putInt("${KEY_TIMEOUT_MIN}_$userId", minutes).apply()
    }

    fun getTimeout(context: Context, userId: String = "default"): Int =
        prefs(context).getInt("${KEY_TIMEOUT_MIN}_$userId", DEFAULT_TIMEOUT)

    fun getTimeoutMs(context: Context, userId: String = "default"): Long =
        getTimeout(context, userId) * 60 * 1000L
}
