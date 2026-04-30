package com.dredio.textraocr.harness

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.dredio.textraocr.R
import com.dredio.textraocr.MainActivity
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SettingsActivityUiTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingsScreenShowsCoreSections() {
        composeRule.waitUntilAtLeastOneExists(
            matcher = hasText(context.getString(R.string.settings_button_label)),
            timeoutMillis = 15_000
        )

        composeRule.onNodeWithText(context.getString(R.string.settings_button_label))
            .performClick()

        composeRule.waitUntilAtLeastOneExists(
            matcher = hasText(context.getString(R.string.settings_screen_title)),
            timeoutMillis = 15_000
        )

        composeRule.onAllNodesWithText(context.getString(R.string.settings_screen_title))
            .assertCountEquals(1)
        composeRule.onAllNodesWithText(context.getString(R.string.settings_general_section_title))
            .assertCountEquals(1)
        composeRule.onAllNodesWithText(context.getString(R.string.ocr_tuning_section_title))
            .assertCountEquals(1)
        composeRule.onAllNodesWithText(context.getString(R.string.subscription_section_title))
            .assertCountEquals(1)
        composeRule.onAllNodesWithText(context.getString(R.string.close_label))
            .assertCountEquals(1)
    }
}
