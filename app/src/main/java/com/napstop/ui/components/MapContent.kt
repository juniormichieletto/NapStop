package com.napstop.ui.components

import android.graphics.Color as AndroidColor
import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.events.MapEventsReceiver
import com.napstop.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

/**
 * Isolated Map component wrapping the Osmdroid MapView with lifecycle handling,
 * touch gestures, current location marker, destination marker, and trigger radius overlay.
 */
@Composable
fun MapContent(
    targetLocation: GeoPoint?,
    currentLocation: Location?,
    dynamicRadius: Float,
    customRadius: Float?,
    modifier: Modifier = Modifier,
    forceCenterTrigger: Long = 0L,
    explicitCenterPoint: GeoPoint? = null,
    onTargetSelected: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            val london = GeoPoint(51.5074, -0.1278) // Default coordinate if current not available
            controller.setZoom(14.0)
            controller.setCenter(london)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentLocation) {
        if (currentLocation != null && targetLocation == null) {
            val currGeo = GeoPoint(currentLocation.latitude, currentLocation.longitude)
            mapView.controller.animateTo(currGeo)
        }
    }

    LaunchedEffect(targetLocation) {
        if (targetLocation != null) {
            mapView.controller.animateTo(targetLocation)
        }
    }

    LaunchedEffect(forceCenterTrigger) {
        if (forceCenterTrigger > 0L && explicitCenterPoint != null) {
            mapView.controller.animateTo(explicitCenterPoint)
        }
    }

    LaunchedEffect(targetLocation, currentLocation, dynamicRadius, customRadius) {
        mapView.overlays.clear()

        // Map tap receiver
        val mReceive: MapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                onTargetSelected(p)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        }
        mapView.overlays.add(MapEventsOverlay(mReceive))

        // Target Marker & Radius Circle
        if (targetLocation != null) {
            val targetMarker = Marker(mapView)
            targetMarker.position = targetLocation
            targetMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            targetMarker.icon = ContextCompat.getDrawable(context, R.drawable.ic_destination_pin)
            targetMarker.title = "Destination"
            mapView.overlays.add(targetMarker)

            // Radius Polygon
            val polygon = Polygon(mapView)
            val currentRad = customRadius ?: dynamicRadius
            polygon.points = Polygon.pointsAsCircle(targetLocation, currentRad.toDouble())
            polygon.fillColor = AndroidColor.argb(50, 255, 0, 0)
            polygon.strokeColor = AndroidColor.argb(150, 255, 0, 0)
            polygon.strokeWidth = 2f
            mapView.overlays.add(polygon)
        }

        // Current Location Marker
        if (currentLocation != null) {
            val currentMarker = Marker(mapView)
            currentMarker.position = GeoPoint(currentLocation.latitude, currentLocation.longitude)
            currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            currentMarker.icon = ContextCompat.getDrawable(context, R.drawable.ic_my_location_dot)
            currentMarker.title = "You"
            mapView.overlays.add(currentMarker)
        }

        mapView.invalidate()
    }

    AndroidView(factory = { mapView }, modifier = modifier.fillMaxSize())
}
