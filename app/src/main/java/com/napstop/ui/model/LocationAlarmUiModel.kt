package com.napstop.ui.model

import com.napstop.SavedAlarm
import org.osmdroid.util.GeoPoint

/**
 * Trigger type for a geofence location alarm.
 */
enum class GeofenceTrigger(val label: String) {
    ARRIVING("Arriving"),
    LEAVING("Leaving")
}

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
    val trigger: GeofenceTrigger = GeofenceTrigger.ARRIVING,
    val isEnabled: Boolean = true,
    val isCurrentTarget: Boolean = false,
    val isAlarmActive: Boolean = false
) {
    /**
     * Formatted subtitle according to Section 9:
     * e.g. "300 m · Arriving"
     */
    val subtitle: String
        get() = "$radiusMeters m · ${trigger.label}"

    val geoPoint: GeoPoint
        get() = GeoPoint(latitude, longitude)

    /**
     * Determines whether the alarm is actively monitoring/alerting.
     */
    val isTriggerActive: Boolean
        get() = isCurrentTarget && isAlarmActive && isEnabled

    /**
     * Determines whether the alarm is paused/muted.
     */
    val isPaused: Boolean
        get() = !isEnabled
}

/**
 * Convert Room database [SavedAlarm] entity to UI presentation model.
 */
fun SavedAlarm.toUiModel(
    currentTarget: GeoPoint? = null,
    isAlarmActive: Boolean = false,
    pausedAlarmIds: Set<Int> = emptySet()
): LocationAlarmUiModel {
    val isTarget = currentTarget?.let {
        Math.abs(it.latitude - this.latitude) < 0.0001 &&
        Math.abs(it.longitude - this.longitude) < 0.0001
    } ?: false

    val isPaused = pausedAlarmIds.contains(this.id)

    return LocationAlarmUiModel(
        id = this.id,
        name = this.name,
        latitude = this.latitude,
        longitude = this.longitude,
        radiusMeters = this.radius.toInt(),
        trigger = GeofenceTrigger.ARRIVING,
        isEnabled = !isPaused,
        isCurrentTarget = isTarget,
        isAlarmActive = isTarget && isAlarmActive
    )
}
