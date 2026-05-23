package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SavedAlarmRepository(DatabaseProvider.savedAlarmDao)

    val savedAlarms: StateFlow<List<SavedAlarm>> = repository.allAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveAlarm(name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            repository.insertAlarm(
                SavedAlarm(
                    name = name.ifBlank { "Location (${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)})" },
                    latitude = latitude,
                    longitude = longitude
                )
            )
        }
    }

    fun deleteAlarm(alarm: SavedAlarm) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
        }
    }
}
