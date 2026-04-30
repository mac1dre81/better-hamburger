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
class MainActivityUiTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenShowsOnlyOneSettingsButton() {
        composeRule.waitUntilAtLeastOneExists(
            matcher = hasText(context.getString(R.string.settings_button_label)),
            timeoutMillis = 15_000
        )

        composeRule.onAllNodesWithText(context.getString(R.string.settings_button_label))
            .assertCountEquals(1)
    }

    @Test
    fun subscribeButtonOpensPurchasePopup() {
        val subscribeLabel = context.getString(R.string.subscribe_premium_button)
        val plansTitle = context.getString(R.string.subscription_plans_title)
        val unavailableText = context.getString(R.string.subscription_unavailable)

        composeRule.waitUntilAtLeastOneExists(
            matcher = hasText(subscribeLabel),
            timeoutMillis = 15_000
        )

        composeRule.onNodeWithText(subscribeLabel).performClick()
        composeRule.waitUntilAtLeastOneExists(
            matcher = hasText(plansTitle),
            timeoutMillis = 15_000
        )
        composeRule.onAllNodesWithText(plansTitle).assertCountEquals(1)
        composeRule.onAllNodesWithText(unavailableText).assertCountEquals(1)
    }
}
