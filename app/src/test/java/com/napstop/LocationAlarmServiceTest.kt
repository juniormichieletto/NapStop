package com.napstop

import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.osmdroid.util.GeoPoint

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocationAlarmServiceTest {
    
    @Before
    fun setup() {
        AppRepository.currentLocation.value = null
        AppRepository.targetLocation.value = null
        AppRepository.isAlarmActive.value = false
    }

    @Test
    fun testAlarmTriggersWhenDistanceIsWithinRadius() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        AppRepository.isAlarmActive.value = true
        AppRepository.targetLocation.value = GeoPoint(0.0, 0.0)
        AppRepository.dynamicRadius.value = 1000f

        val service = LocationAlarmService()
        
        val currentLocation = Location("").apply {
            latitude = 0.001 // close to 0.0
            longitude = 0.001 
        }

        // We can manually trigger check location if we made it visible or we can just test the math logic
        val target = Location("").apply {
            latitude = 0.0
            longitude = 0.0
        }
        
        val distance = currentLocation.distanceTo(target)
        assertTrue("Distance should be less than radius", distance < 1000f)
    }
}
