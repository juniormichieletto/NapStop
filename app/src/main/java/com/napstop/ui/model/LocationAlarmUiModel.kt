package com.napstop.ui.model

import com.napstop.SavedAlarm
import org.osmdroid.util.GeoPoint

/**
 * UI representation of a location alarm for presentation in the UI layer.
 * Decouples the presentation layer from the underlying Room database entity.
 */
data class LocationAlarmUiModel(
    val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val isCurrentTarget: Boolean = false,
    val isAlarmActive: Boolean = false
) {
    val subtitle: String
        get() = "${radiusMeters}m · Lat: ${String.format("%.4f", latitude)}, Lon: ${String.format("%.4f", longitude)}"

    val geoPoint: GeoPoint
        get() = GeoPoint(latitude, longitude)
}

/**
 * Convert Room database [SavedAlarm] entity to UI presentation model.
 */
fun SavedAlarm.toUiModel(
    currentTarget: GeoPoint? = null,
    isAlarmActive: Boolean = false
): LocationAlarmUiModel {
    val isTarget = currentTarget?.let {
        Math.abs(it.latitude - this.latitude) < 0.0001 &&
        Math.abs(it.longitude - this.longitude) < 0.0001
    } ?: false

    return LocationAlarmUiModel(
        id = this.id,
        name = this.name,
        latitude = this.latitude,
        longitude = this.longitude,
        radiusMeters = this.radius.toInt(),
        isCurrentTarget = isTarget,
        isAlarmActive = isTarget && isAlarmActive
    )
}
