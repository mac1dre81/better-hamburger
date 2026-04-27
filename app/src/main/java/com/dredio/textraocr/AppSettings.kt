package com.dredio.textraocr

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppSettings {
    private const val PREFS_NAME = "textraocr_settings"
    private const val KEY_BLACK_WHITE_PREVIEW = "black_white_preview"
    private const val KEY_PREMIUM_CACHED = "premium_cached"
    private const val KEY_SCAN_DATE = "scan_date"
    private const val KEY_SCAN_COUNT = "scan_count"

    const val FREE_DAILY_SCAN_LIMIT = 8

    fun isBlackWhitePreviewEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_BLACK_WHITE_PREVIEW, false)
    }

    fun setBlackWhitePreviewEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BLACK_WHITE_PREVIEW, enabled).apply()
    }

    fun isPremiumCached(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_PREMIUM_CACHED, false)
    }

    fun setPremiumCached(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PREMIUM_CACHED, enabled).apply()
    }

    fun remainingFreeScans(context: Context): Int {
        refreshDailyCounterIfNeeded(context)
        val used = prefs(context).getInt(KEY_SCAN_COUNT, 0)
        return (FREE_DAILY_SCAN_LIMIT - used).coerceAtLeast(0)
    }

    fun canStartScan(context: Context, isPremium: Boolean): Boolean {
        return isPremium || remainingFreeScans(context) > 0
    }

    fun recordCompletedScan(context: Context) {
        refreshDailyCounterIfNeeded(context)
        val preferences = prefs(context)
        val currentCount = preferences.getInt(KEY_SCAN_COUNT, 0)
        preferences.edit().putInt(KEY_SCAN_COUNT, currentCount + 1).apply()
    }

    private fun refreshDailyCounterIfNeeded(context: Context) {
        val preferences = prefs(context)
        val today = currentDayKey()
        if (preferences.getString(KEY_SCAN_DATE, null) == today) {
            return
        }

        preferences.edit()
            .putString(KEY_SCAN_DATE, today)
            .putInt(KEY_SCAN_COUNT, 0)
            .apply()
    }

    private fun currentDayKey(): String {
        return DAY_FORMAT.format(Date())
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val DAY_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
}
