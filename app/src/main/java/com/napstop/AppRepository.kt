package com.napstop

import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import org.osmdroid.util.GeoPoint

object AppRepository {
    // The target destination
    val targetLocation = MutableStateFlow<GeoPoint?>(null)
    
    // Custom radius if the user specified one, null if dynamic
    val customRadius = MutableStateFlow<Float?>(null)
    
    // Whether the alarm is currently active
    val isAlarmActive = MutableStateFlow(false)
    
    // The user's current location (updated by service)
    val currentLocation = MutableStateFlow<Location?>(null)
    
    // the dynamic radius in meters that we trigger the alarm on
    val dynamicRadius = MutableStateFlow(500f) // default to 500m
}
