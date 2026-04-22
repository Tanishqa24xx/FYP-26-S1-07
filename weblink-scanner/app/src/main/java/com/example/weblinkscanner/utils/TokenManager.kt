package com.example.weblinkscanner.utils

import android.content.Context
import android.content.SharedPreferences

object TokenManager {

    private const val PREF_NAME = "linkscanner_prefs"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_EXPIRY = "token_expiry"
    private const val KEY_NAME = "user_name"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_PLAN = "user_plan"
    private const val KEY_USER_ID = "user_id"
    private const val SESSION_DURATION = 7 * 24 * 60 * 60 * 1000L // 7 days

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // --- Save full session ---
    fun saveSession(
        context: Context,
        token: String,
        name: String,
        email: String,
        plan: String,
        userId: String
    ) {
        val expiry = System.currentTimeMillis() + SESSION_DURATION
        prefs(context).edit()
            .putString(KEY_TOKEN,   token)
            .putLong(KEY_EXPIRY,    expiry)
            .putString(KEY_NAME,    name)
            .putString(KEY_EMAIL,   email)
            .putString(KEY_PLAN,    plan)
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    // Keep backward compat
    fun saveToken(context: Context, token: String) {
        val expiry = System.currentTimeMillis() + SESSION_DURATION
        prefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_EXPIRY,  expiry)
            .apply()
    }

    // --- Get token if valid ---
    fun getToken(context: Context): String? {
        val prefs  = prefs(context)
        val token  = prefs.getString(KEY_TOKEN, null)  ?: return null
        val expiry = prefs.getLong(KEY_EXPIRY, 0L)
        return if (System.currentTimeMillis() < expiry) token
        else { clearSession(context); null }
    }

    fun hasValidSession(context: Context) = getToken(context) != null

    // --- Restore saved user info ---
    fun getSavedName(context: Context): String = prefs(context).getString(KEY_NAME,    "") ?: ""
    fun getSavedEmail(context: Context): String = prefs(context).getString(KEY_EMAIL,   "") ?: ""
    fun getSavedPlan(context: Context): String = prefs(context).getString(KEY_PLAN,    "FREE") ?: "FREE"
    fun getSavedUserId(context: Context): String = prefs(context).getString(KEY_USER_ID, "00000000-0000-0000-0000-000000000000") ?: "00000000-0000-0000-0000-000000000000"

    // --- Clear on logout ---
    fun clearSession(context: Context) {
        prefs(context).edit().clear().apply()
    }

    // Keep backward compat
    fun clearToken(context: Context) = clearSession(context)
}
