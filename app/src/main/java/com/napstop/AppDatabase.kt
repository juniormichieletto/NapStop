package com.napstop

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SavedAlarm::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedAlarmDao(): SavedAlarmDao
}
