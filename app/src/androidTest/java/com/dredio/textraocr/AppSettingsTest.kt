package com.dredio.textraocr

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSettingsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun defaultsAreStable() {
        assertFalse(AppSettings.isBlackWhitePreviewEnabled(context))
        assertFalse(AppSettings.isDarkThemeEnabled(context))
        assertTrue(AppSettings.isAutoOcrImageOpenEnabled(context))
        assertFalse(AppSettings.isAutoSaveOcrEnabled(context))
        assertEquals(AppSettings.DEFAULT_OCR_OUTPUT_FORMAT, AppSettings.getDefaultOutputFormat(context))
        assertEquals(AppSettings.DEFAULT_SAVE_DESTINATION, AppSettings.getSaveDestination(context))
        assertEquals(AppSettings.DEFAULT_PREPROCESSING_PROFILE, AppSettings.getDefaultPreprocessingProfile(context))
        assertFalse(AppSettings.isPremiumCached(context))
        assertEquals(AppSettings.FREE_DAILY_SCAN_LIMIT, AppSettings.remainingFreeScans(context))
    }

    @Test
    fun settersPersistValues() {
        AppSettings.setBlackWhitePreviewEnabled(context, true)
        AppSettings.setDarkThemeEnabled(context, true)
        AppSettings.setAutoOcrImageOpenEnabled(context, false)
        AppSettings.setAutoSaveOcrEnabled(context, true)
        AppSettings.setDefaultOutputFormat(context, ".md")
        AppSettings.setSaveDestination(context, AppSettings.SAVE_DESTINATION_SHARE)
        AppSettings.setDefaultPreprocessingProfile(context, AppSettings.PROFILE_FAST)
        AppSettings.setPremiumCached(context, true)

        assertTrue(AppSettings.isBlackWhitePreviewEnabled(context))
        assertTrue(AppSettings.isDarkThemeEnabled(context))
        assertFalse(AppSettings.isAutoOcrImageOpenEnabled(context))
        assertTrue(AppSettings.isAutoSaveOcrEnabled(context))
        assertEquals(".md", AppSettings.getDefaultOutputFormat(context))
        assertEquals(AppSettings.SAVE_DESTINATION_SHARE, AppSettings.getSaveDestination(context))
        assertEquals(AppSettings.PROFILE_FAST, AppSettings.getDefaultPreprocessingProfile(context))
        assertTrue(AppSettings.isPremiumCached(context))
    }

    @Test
    fun scanCounterResetsWhenDateChanges() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("scan_date", "2000-01-01")
            .putInt("scan_count", 9)
            .commit()

        assertEquals(AppSettings.FREE_DAILY_SCAN_LIMIT, AppSettings.remainingFreeScans(context))
        assertEquals(0, prefs.getInt("scan_count", -1))
    }

    @Test
    fun canStartScanRespectsPremiumAndDailyLimit() {
        assertTrue(AppSettings.canStartScan(context, isPremium = true))

        repeat(AppSettings.FREE_DAILY_SCAN_LIMIT) {
            AppSettings.recordCompletedScan(context)
        }

        assertEquals(0, AppSettings.remainingFreeScans(context))
        assertFalse(AppSettings.canStartScan(context, isPremium = false))
    }

    private companion object {
        private const val PREFS_NAME = "textraocr_settings"
    }
}