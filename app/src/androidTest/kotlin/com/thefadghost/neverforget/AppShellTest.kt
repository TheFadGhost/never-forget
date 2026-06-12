package com.thefadghost.neverforget

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import com.thefadghost.neverforget.feature.onboarding.OnboardingScreen
import com.thefadghost.neverforget.navigation.MainShell
import org.junit.Rule
import org.junit.Test

class AppShellTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboardingExplainsCountryAndReminderSetup() {
        composeRule.setContent {
            OnboardingScreen(onComplete = { _ -> })
        }

        composeRule.onNodeWithText("Never forget what matters").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onAllNodesWithText("United Kingdom").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Bulgaria").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Daily from 9:00 AM", substring = true).assertIsDisplayed()
    }

    @Test
    fun mainShellExposesFiveDestinationsAndQuickAdd() {
        composeRule.setContent {
            MainShell()
        }

        listOf("Home", "Calendar", "People", "Occasions", "Settings").forEach {
            composeRule.onAllNodesWithText(it).onFirst().assertIsDisplayed()
        }
        composeRule.onNodeWithContentDescription("Add important date").performClick()
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Birthday").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Task").onFirst().assertIsDisplayed()
    }
}
