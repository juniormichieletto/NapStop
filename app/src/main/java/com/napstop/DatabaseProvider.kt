package com.napstop

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    lateinit var database: AppDatabase
        private set

    val savedAlarmDao: SavedAlarmDao
        get() = database.savedAlarmDao()

    fun init(context: Context) {
        if (!::database.isInitialized) {
            database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "commute_wake_db"
            ).fallbackToDestructiveMigration().build()
        }
    }
}
