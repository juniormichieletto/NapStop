package com.example

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedAlarmDao {
    @Query("SELECT * FROM saved_alarms ORDER BY timestamp DESC")
    fun getAllAlarms(): Flow<List<SavedAlarm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: SavedAlarm)

    @Delete
    suspend fun deleteAlarm(alarm: SavedAlarm)

    @Query("DELETE FROM saved_alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Int)
}
