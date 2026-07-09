package com.napstop

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CommuteWakeDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: SavedAlarmDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.savedAlarmDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetAllAlarms() = runBlocking {
        val alarm = SavedAlarm(
            id = 1,
            name = "Work Office",
            latitude = 37.7749,
            longitude = -122.4194,
            radius = 600f,
            timestamp = 1000L
        )

        dao.insertAlarm(alarm)

        val allAlarms = dao.getAllAlarms().first()
        assertEquals(1, allAlarms.size)
        assertEquals("Work Office", allAlarms[0].name)
        assertEquals(37.7749, allAlarms[0].latitude, 0.0001)
        assertEquals(-122.4194, allAlarms[0].longitude, 0.0001)
        assertEquals(600f, allAlarms[0].radius, 0.0001f)
    }

    @Test
    fun deleteAlarm() = runBlocking {
        val alarm = SavedAlarm(
            id = 2,
            name = "Gym",
            latitude = 34.0522,
            longitude = -118.2437,
            radius = 500f,
            timestamp = 2000L
        )

        dao.insertAlarm(alarm)
        var allAlarms = dao.getAllAlarms().first()
        assertEquals(1, allAlarms.size)

        dao.deleteAlarm(allAlarms[0])
        allAlarms = dao.getAllAlarms().first()
        assertTrue(allAlarms.isEmpty())
    }

    @Test
    fun deleteAlarmById() = runBlocking {
        val alarm = SavedAlarm(
            id = 42,
            name = "Central Station",
            latitude = 40.7128,
            longitude = -74.0060,
            radius = 500f,
            timestamp = 3000L
        )

        dao.insertAlarm(alarm)
        var allAlarms = dao.getAllAlarms().first()
        assertEquals(1, allAlarms.size)

        dao.deleteAlarmById(allAlarms[0].id)
        allAlarms = dao.getAllAlarms().first()
        assertTrue(allAlarms.isEmpty())
    }
}
