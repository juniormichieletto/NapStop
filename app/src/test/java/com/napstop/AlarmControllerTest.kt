package com.napstop

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAudioManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlarmControllerTest {

    @Test
    fun testHeadphoneCheckReturnsValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val headphoneStatus = AlarmController.areHeadphonesConnected(context)
        // By default on a fresh headless JVM context, headphones are likely not connected
        // Verification asserts it evaluates clean without crashing or throwing NPEs
        assertFalse(headphoneStatus)
    }

    @Test
    fun testStartAndStopDoesNotThrow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Ensure starting and stopping the alarm is solid and handle any system exception gracefully
        try {
            AlarmController.startAlarm(context)
            AlarmController.stopAlarm()
        } catch (e: Exception) {
            // No unhandled crashing is acceptable
            throw e
        }
    }
}
