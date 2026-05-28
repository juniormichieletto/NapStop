package com.napstop

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_alarms")
data class SavedAlarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)
