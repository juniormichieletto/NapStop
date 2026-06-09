package com.napstop

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainViewModelTest {

    private lateinit var application: Application
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        // Initialize DatabaseProvider with context
        DatabaseProvider.init(application)
    }

    @Test
    fun saveAlarmAndObserve() = runBlocking {
        val viewModel = MainViewModel(application)

        // Count existing alarms
        val initialSize = viewModel.savedAlarms.value.size

        // Save a test alarm
        viewModel.saveAlarm("Target Location X", 48.8566, 2.3522)

        // Yield/Wait for state flow
        // The repository saves in viewModelScope, let's wait a little bit or query DB
        val dao = DatabaseProvider.savedAlarmDao
        var attempts = 0
        var alarms = dao.getAllAlarms().first()
        while (alarms.size <= initialSize && attempts < 10) {
            Thread.sleep(100)
            alarms = dao.getAllAlarms().first()
            attempts++
        }

        // Verify it exists in database flow
        assertNotEquals(0, alarms.size)
        val addedAlarm = alarms.firstOrNull { it.name == "Target Location X" }
        assertNotEquals(null, addedAlarm)
        assertEquals(48.8566, addedAlarm!!.latitude, 0.0001)
        assertEquals(2.3522, addedAlarm!!.longitude, 0.0001)
    }
}
