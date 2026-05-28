package com.napstop

import kotlinx.coroutines.flow.Flow

class SavedAlarmRepository(private val dao: SavedAlarmDao) {
    val allAlarms: Flow<List<SavedAlarm>> = dao.getAllAlarms()

    suspend fun insertAlarm(alarm: SavedAlarm) {
        dao.insertAlarm(alarm)
    }

    suspend fun deleteAlarm(alarm: SavedAlarm) {
        dao.deleteAlarm(alarm)
    }

    suspend fun deleteAlarmById(id: Int) {
        dao.deleteAlarmById(id)
    }
}
