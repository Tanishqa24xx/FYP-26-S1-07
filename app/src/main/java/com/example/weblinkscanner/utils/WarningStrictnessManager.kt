/*
 This manager stores how strictly we handle suspicious links for each user.

 It basically lets users decide if they want to see every potential threat
 or if they only want to be interrupted for things that are definitely
 dangerous. We save this per-user so their preference sticks even after logging out.
 */

package com.example.weblinkscanner.utils

import android.content.Context
import android.content.SharedPreferences

object WarningStrictnessManager {

    private const val PREF_NAME  = "strictness_prefs"
    private const val KEY_LEVEL  = "warning_strictness"
    const val LOW    = "LOW"
    const val MEDIUM = "MEDIUM"
    const val HIGH   = "HIGH"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, level: String, userId: String = "default") {
        prefs(context).edit().putString("${KEY_LEVEL}_$userId", level).apply()
    }

    fun get(context: Context, userId: String = "default"): String =
        prefs(context).getString("${KEY_LEVEL}_$userId", MEDIUM) ?: MEDIUM

    // Adjusts the displayed verdict based on strictness.
    // The backend always returns the raw verdict; the client adjusts display.
    // LOW: SUSPICIOUS - treated as SAFE on-screen (user wants fewer alerts)
    // MEDIUM: no change (default)
    // HIGH: no change to verdict, but caller can show extra warnings
    fun adjustVerdict(rawVerdict: String, strictness: String): String {
        return if (strictness == LOW && rawVerdict.uppercase() == "SUSPICIOUS") "SAFE"
        else rawVerdict
    }
}