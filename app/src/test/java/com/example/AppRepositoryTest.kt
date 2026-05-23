package com.example

import android.location.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.osmdroid.util.GeoPoint

class AppRepositoryTest {

    @Test
    fun testAppRepositoryInitialAndUpdatedStates() {
        // Test initial values
        assertNull(AppRepository.targetLocation.value)
        assertEquals(false, AppRepository.isAlarmActive.value)
        assertNull(AppRepository.currentLocation.value)
        assertEquals(500f, AppRepository.dynamicRadius.value)

        // Test state transitions
        val target = GeoPoint(45.0, 9.0)
        AppRepository.targetLocation.value = target
        assertEquals(target, AppRepository.targetLocation.value)

        AppRepository.isAlarmActive.value = true
        assertEquals(true, AppRepository.isAlarmActive.value)

        AppRepository.dynamicRadius.value = 850f
        assertEquals(850f, AppRepository.dynamicRadius.value)

        // Reset to avoid side effects on other tests
        AppRepository.targetLocation.value = null
        AppRepository.isAlarmActive.value = false
        AppRepository.currentLocation.value = null
        AppRepository.dynamicRadius.value = 500f
    }
}
