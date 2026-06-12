package com.thefadghost.neverforget

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.thefadghost.neverforget.database.NeverForgetDatabase
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

class OnboardingCountryPersistenceTest {
    private val clearSettingsRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                File(context.filesDir.parentFile, "datastore/never_forget_settings.preferences_pb").delete()
                NeverForgetDatabase.get(context).clearAllTables()
                base.evaluate()
            }
        }
    }
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(clearSettingsRule).around(composeRule)

    @Test
    fun countryChoicesFromOnboardingArePersisted() {
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithContentDescription("Bulgaria selected").performClick()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Start planning").performClick()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Stay ahead").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Settings").onFirst().performClick()
        composeRule.onNodeWithContentDescription("Bulgaria enabled").assertIsOff()

        composeRule.waitUntil(5_000) {
            runBlocking {
                NeverForgetDatabase.get(composeRule.activity).eventsDao().snapshot()
                    .any { it.type == "COUNTRY_OBSERVANCE" }
            }
        }
        val countryEvents = runBlocking {
            NeverForgetDatabase.get(composeRule.activity).eventsDao().snapshot()
                .filter { it.type == "COUNTRY_OBSERVANCE" }
        }
        assertTrue(countryEvents.isNotEmpty())
        assertTrue(countryEvents.all { it.countryCode == "GB" })
    }
}
