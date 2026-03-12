package com.example.linkscanner.utils

import android.content.Context
import android.content.SharedPreferences

object TokenManager {

    private const val PREF_NAME        = "linkscanner_prefs"
    private const val KEY_TOKEN        = "access_token"
    private const val KEY_EXPIRY       = "token_expiry"
    private const val SESSION_DURATION = 7 * 24 * 60 * 60 * 1000L // 7 days in ms

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ── Save token with 7-day expiry ──────────────────────────────────────────
    fun saveToken(context: Context, token: String) {
        val expiry = System.currentTimeMillis() + SESSION_DURATION
        prefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_EXPIRY, expiry)
            .apply()
        android.util.Log.d("TOKEN", "Token saved, expires in 7 days")
    }

    // ── Get token only if not expired ─────────────────────────────────────────
    fun getToken(context: Context): String? {
        val prefs  = prefs(context)
        val token  = prefs.getString(KEY_TOKEN, null)
        val expiry = prefs.getLong(KEY_EXPIRY, 0L)

        if (token == null) return null

        return if (System.currentTimeMillis() < expiry) {
            android.util.Log.d("TOKEN", "Token valid, days remaining: ${daysRemaining(expiry)}")
            token
        } else {
            android.util.Log.d("TOKEN", "Token expired — clearing")
            clearToken(context)
            null
        }
    }

    // ── Check if a valid session exists ───────────────────────────────────────
    fun hasValidSession(context: Context): Boolean {
        return getToken(context) != null
    }

    // ── Clear token on logout ─────────────────────────────────────────────────
    fun clearToken(context: Context) {
        prefs(context).edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EXPIRY)
            .apply()
        android.util.Log.d("TOKEN", "Token cleared")
    }

    // ── Helper: days remaining ────────────────────────────────────────────────
    private fun daysRemaining(expiry: Long): Long {
        val ms = expiry - System.currentTimeMillis()
        return ms / (24 * 60 * 60 * 1000L)
    }
}