package com.example.batterypercentagenotifier

import android.content.Context
import android.content.SharedPreferences

object ThresholdPrefs {
    private const val PREFS_NAME = "battery_prefs"
    private const val KEY_THRESHOLD = "threshold"
    private const val KEY_LAST_TRIGGERED_PERCENT = "last_triggered_percent"
    private const val DEFAULT_THRESHOLD = 85

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThreshold(context: Context): Int =
        getPrefs(context).getInt(KEY_THRESHOLD, DEFAULT_THRESHOLD)

    fun setThreshold(context: Context, value: Int) {
        getPrefs(context).edit().putInt(KEY_THRESHOLD, value).apply()
    }

    fun getLastTriggeredPercent(context: Context): Int =
        getPrefs(context).getInt(KEY_LAST_TRIGGERED_PERCENT, -1)

    fun setLastTriggeredPercent(context: Context, value: Int) {
        getPrefs(context).edit().putInt(KEY_LAST_TRIGGERED_PERCENT, value).apply()
    }

    fun clearLastTriggeredPercent(context: Context) {
        getPrefs(context).edit().remove(KEY_LAST_TRIGGERED_PERCENT).apply()
    }
}
