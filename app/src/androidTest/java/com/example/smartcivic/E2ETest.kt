package com.example.smartcivic

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class E2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testAppLaunch_displaysSplashAndHomeScreen() {
        // 1. Verify app title is visible on launch (or in splash screen)
        composeTestRule.onAllNodesWithText("smart civic").onFirst().assertExists()
        
        // 2. Wait for splash screen to disappear (takes 2000ms in code)
        composeTestRule.mainClock.advanceTimeBy(2500)
        
        // 3. Verify main home tab content elements
        composeTestRule.onNodeWithText("City Analytics").assertExists()
        composeTestRule.onNodeWithText("Quick Opportunities").assertExists()
    }

    @Test
    fun testNavigation_openSettings() {
        composeTestRule.mainClock.advanceTimeBy(2500)
        
        // Find Settings quicklink in top bar or bottom navigation and click it
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        
        // Verify we navigated to Settings screen
        composeTestRule.onNodeWithText("Dark Mode").assertExists()
    }
}
