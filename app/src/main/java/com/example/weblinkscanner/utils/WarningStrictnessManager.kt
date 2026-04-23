package com.example.weblinkscanner.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists the user's chosen warning strictness level.
 *
 * LOW    - Only DANGEROUS verdicts shown as a warning; SUSPICIOUS treated as SAFE.
 * MEDIUM - Default. SUSPICIOUS and DANGEROUS both shown as warnings.
 * HIGH   - SUSPICIOUS and DANGEROUS shown; long URL alone triggers a notice.
 *
 * The strictness is sent to the backend as a query param on scan requests,
 * or applied client-side by adjusting the displayed verdict.
 */
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

    /**
     * Adjusts the displayed verdict based on strictness.
     * The backend always returns the raw verdict; the client adjusts display.
     *
     *   LOW:    SUSPICIOUS → treated as SAFE on-screen (user wants fewer alerts)
     *   MEDIUM: no change (default)
     *   HIGH:   no change to verdict, but caller can show extra warnings
     */
    fun adjustVerdict(rawVerdict: String, strictness: String): String {
        return if (strictness == LOW && rawVerdict.uppercase() == "SUSPICIOUS") "SAFE"
        else rawVerdict
    }
}