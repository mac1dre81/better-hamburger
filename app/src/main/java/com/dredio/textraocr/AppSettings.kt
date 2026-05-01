package com.dredio.textraocr

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppSettings {
    private const val PREFS_NAME = "textraocr_settings"
    private const val KEY_BLACK_WHITE_PREVIEW = "black_white_preview"
    private const val KEY_DARK_THEME = "dark_theme"
    private const val KEY_AUTO_OCR_IMAGE_OPEN = "auto_ocr_image_open"
    private const val KEY_AUTO_SAVE_OCR = "auto_save_ocr"
    private const val KEY_DEFAULT_OUTPUT_FORMAT = "default_output_format"
    private const val KEY_SAVE_DESTINATION = "save_destination"
    private const val KEY_DEFAULT_PREPROCESSING_PROFILE = "default_preprocessing_profile"
    private const val KEY_PREMIUM_CACHED = "premium_cached"
    private const val KEY_SCAN_DATE = "scan_date"
    private const val KEY_SCAN_COUNT = "scan_count"

    const val FREE_DAILY_SCAN_LIMIT = 10
    const val DEFAULT_OCR_OUTPUT_FORMAT = ".txt"
    const val SAVE_DESTINATION_LOCAL = "local"
    const val SAVE_DESTINATION_SHARE = "share"
    const val DEFAULT_SAVE_DESTINATION = SAVE_DESTINATION_LOCAL
    const val PROFILE_BALANCED = "balanced"
    const val PROFILE_CLEAN = "clean"
    const val PROFILE_FAST = "fast"
    const val DEFAULT_PREPROCESSING_PROFILE = PROFILE_BALANCED

    fun isBlackWhitePreviewEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_BLACK_WHITE_PREVIEW, false)
    }

    fun setBlackWhitePreviewEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BLACK_WHITE_PREVIEW, enabled).apply()
    }

    fun isDarkThemeEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_DARK_THEME, false)
    }

    fun setDarkThemeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK_THEME, enabled).apply()
    }

    fun isAutoOcrImageOpenEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_AUTO_OCR_IMAGE_OPEN, true)
    }

    fun setAutoOcrImageOpenEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_OCR_IMAGE_OPEN, enabled).apply()
    }

    fun isAutoSaveOcrEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_AUTO_SAVE_OCR, false)
    }

    fun setAutoSaveOcrEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_SAVE_OCR, enabled).apply()
    }

    fun getDefaultOutputFormat(context: Context): String {
        val storedFormat = prefs(context).getString(KEY_DEFAULT_OUTPUT_FORMAT, DEFAULT_OCR_OUTPUT_FORMAT)
            ?: DEFAULT_OCR_OUTPUT_FORMAT
        return normalizeOutputFormat(storedFormat)
    }

    fun setDefaultOutputFormat(context: Context, format: String) {
        prefs(context).edit().putString(KEY_DEFAULT_OUTPUT_FORMAT, normalizeOutputFormat(format)).apply()
    }

    fun getSaveDestination(context: Context): String {
        return prefs(context).getString(KEY_SAVE_DESTINATION, DEFAULT_SAVE_DESTINATION)
            ?: DEFAULT_SAVE_DESTINATION
    }

    fun setSaveDestination(context: Context, destination: String) {
        prefs(context).edit().putString(KEY_SAVE_DESTINATION, destination).apply()
    }

    fun getDefaultPreprocessingProfile(context: Context): String {
        return prefs(context).getString(KEY_DEFAULT_PREPROCESSING_PROFILE, DEFAULT_PREPROCESSING_PROFILE)
            ?: DEFAULT_PREPROCESSING_PROFILE
    }

    fun setDefaultPreprocessingProfile(context: Context, profile: String) {
        prefs(context).edit().putString(KEY_DEFAULT_PREPROCESSING_PROFILE, profile).apply()
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

    private fun normalizeOutputFormat(format: String): String {
        return when (format.lowercase(Locale.US)) {
            ".md" -> ".md"
            else -> DEFAULT_OCR_OUTPUT_FORMAT
        }
    }

    private val DAY_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
}
