package com.napstop

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.os.Build

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class MainActivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainScreen_rendersAndShowsStartButton() {
        composeTestRule.setContent {
            // We pass hasBackgroundLocation = true to bypass the permission warning screen for the test
            MainScreen(hasBackgroundLocation = true)
        }

        // Initially we should see "Tap on map or search above to set stop" because no target location is set
        composeTestRule.onNodeWithText("Tap on map or search above to set stop").assertExists()
    }

    @Test
    fun mainScreen_savesTargetLocation() {
        composeTestRule.setContent {
            MainScreen(hasBackgroundLocation = true)
        }

        // Before setting target location, check for prompt
        composeTestRule.onNodeWithText("Tap on map or search above to set stop").assertExists()

        // We can simulate target location change manually via Repository to see how UI updates
        AppRepository.targetLocation.value = org.osmdroid.util.GeoPoint(48.8566, 2.3522)

        composeTestRule.waitForIdle()

        // It should display "Selected Destination" or the Lat/Lon
        composeTestRule.onNodeWithText("Selected Destination").assertExists()
        
        // Ensure "Start Alarm" or "Alarm Active" text button is visible now
        composeTestRule.onNodeWithText("Start Alarm").assertExists()
        composeTestRule.onNodeWithText("Stop").assertExists()
    }
}
