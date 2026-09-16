package com.napstop.ui

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.napstop.AlarmController
import com.napstop.AppRepository
import com.napstop.LocationAlarmService
import com.napstop.LocationSearchHelper
import com.napstop.MainViewModel
import com.napstop.SearchResult
import com.napstop.ui.components.AlarmList
import com.napstop.ui.components.CurrentLocationButton
import com.napstop.ui.components.MapContent
import com.napstop.ui.components.SearchLocationBar
import com.napstop.ui.model.toUiModel
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

/**
 * Main Location Alarm Screen decomposing the map foundation, search bar,
 * active alarm controls, and alarm bookmarks list.
 */
@Composable
fun LocationAlarmScreen(
    modifier: Modifier = Modifier,
    hasBackgroundLocation: Boolean = true,
    mainViewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val targetLoc by AppRepository.targetLocation.collectAsState()
    val isAlarmActive by AppRepository.isAlarmActive.collectAsState()
    val currentLocation by AppRepository.currentLocation.collectAsState()
    val dynamicRadius by AppRepository.dynamicRadius.collectAsState()
    val customRadius by AppRepository.customRadius.collectAsState()

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true
            }
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    powerManager.isIgnoringBatteryOptimizations(context.packageName)
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Database state
    val savedAlarmsEntities by mainViewModel.savedAlarms.collectAsStateWithLifecycle()
    val savedAlarmUiModels = remember(savedAlarmsEntities, targetLoc, isAlarmActive) {
        savedAlarmsEntities.map { it.toUiModel(targetLoc, isAlarmActive) }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    DisposableEffect(fusedLocationClient) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    AppRepository.currentLocation.value = location
                }
            }
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    AppRepository.currentLocation.value = location
                }
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("LocationAlarmScreen", "Missing location permissions for foreground updates", e)
        }

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    var speedText by remember { mutableStateOf("0 km/h") }
    var distanceText by remember { mutableStateOf("Unknown") }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var displayResults by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Dialog state for custom label naming
    var showSaveDialog by remember { mutableStateOf(false) }
    var customAlarmLabel by remember { mutableStateOf("") }
    var saveDialogRadius by remember { mutableStateOf(500f) }

    var forceCenterTrigger by remember { mutableStateOf(0L) }
    var explicitCenterPoint by remember { mutableStateOf<GeoPoint?>(null) }

    LaunchedEffect(currentLocation, targetLoc) {
        val curr = currentLocation
        val targ = targetLoc
        if (curr != null && targ != null) {
            val targLoc = Location("").apply {
                latitude = targ.latitude
                longitude = targ.longitude
            }
            val dist = curr.distanceTo(targLoc)
            distanceText = if (dist > 1000) String.format("%.1f km", dist / 1000) else String.format("%.0f m", dist)
            speedText = if (curr.hasSpeed()) String.format("%.0f km/h", curr.speed * 3.6f) else "0 km/h"
        }
    }

    val performSearch = {
        if (searchQuery.isNotBlank()) {
            coroutineScope.launch {
                isSearching = true
                displayResults = true
                searchResults = LocationSearchHelper.searchLocation(context, searchQuery)
                isSearching = false
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Destination") },
            text = {
                Column {
                    Text("Enter a unique or helpful name for this stop:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customAlarmLabel,
                        onValueChange = { customAlarmLabel = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Home, Work Office, Central Station etc.") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Trigger Radius: ${saveDialogRadius.toInt()}m", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = saveDialogRadius,
                        onValueChange = { saveDialogRadius = it },
                        valueRange = 100f..5000f,
                        steps = 49
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        targetLoc?.let {
                            mainViewModel.saveAlarm(customAlarmLabel, it.latitude, it.longitude, saveDialogRadius)
                            AppRepository.customRadius.value = saveDialogRadius
                        }
                        showSaveDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        if (!hasBackgroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Background Location Required",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Tap here, then select 'Permissions' -> 'Location' -> 'Allow all the time' for reliable tracking.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        if (!isIgnoringBatteryOptimizations && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        try {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Battery Optimization Active",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Tap here to grant background exemption so the alarm checks don't sleep.",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
        ) {
            // Map Content
            MapContent(
                targetLocation = targetLoc,
                currentLocation = currentLocation,
                dynamicRadius = dynamicRadius,
                customRadius = customRadius,
                forceCenterTrigger = forceCenterTrigger,
                explicitCenterPoint = explicitCenterPoint,
                onTargetSelected = { geoPoint ->
                    if (!isAlarmActive) {
                        AppRepository.targetLocation.value = geoPoint
                        displayResults = false
                        focusManager.clearFocus()
                    }
                }
            )

            // Centering Button floating on bottom right of map
            CurrentLocationButton(
                onClick = {
                    currentLocation?.let {
                        explicitCenterPoint = GeoPoint(it.latitude, it.longitude)
                        forceCenterTrigger = System.currentTimeMillis()
                    } ?: targetLoc?.let {
                        explicitCenterPoint = it
                        forceCenterTrigger = System.currentTimeMillis()
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            )

            // Dynamic Floating Search Bar Overlay
            SearchLocationBar(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    if (it.isEmpty()) {
                        searchResults = emptyList()
                        displayResults = false
                    } else {
                        displayResults = true
                    }
                },
                onSearch = {
                    performSearch()
                },
                onClear = {
                    searchQuery = ""
                    searchResults = emptyList()
                    displayResults = false
                },
                isSearching = isSearching,
                searchResults = searchResults,
                displayResults = displayResults,
                onResultSelected = { result ->
                    if (!isAlarmActive) {
                        AppRepository.targetLocation.value = result.geoPoint
                        searchQuery = result.name
                        displayResults = false
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Bottom control section combined with Saved Alarms management
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (targetLoc == null) {
                Text(
                    text = "Tap on map or search above to set stop",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = searchQuery.ifBlank { "Selected Destination" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format("Lat: %.4f, Lon: %.4f", targetLoc!!.latitude, targetLoc!!.longitude),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Save alarm bookmark button
                    val isAlreadySaved = savedAlarmsEntities.any {
                        val latDiff = Math.abs(it.latitude - targetLoc!!.latitude)
                        val lonDiff = Math.abs(it.longitude - targetLoc!!.longitude)
                        latDiff < 0.0001 && lonDiff < 0.0001
                    }

                    IconButton(
                        onClick = {
                            if (!isAlreadySaved) {
                                customAlarmLabel = searchQuery
                                saveDialogRadius = customRadius ?: dynamicRadius
                                showSaveDialog = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isAlreadySaved) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Save destination",
                            tint = if (isAlreadySaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBox("Distance", distanceText)
                    StatBox("Speed", speedText)
                    StatBox("Radius", "${(customRadius ?: dynamicRadius).toInt()}m")
                }

                if (!isAlarmActive) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Trigger Radius", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (customRadius != null) {
                                Text(
                                    "Reset to Dynamic",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.clickable { AppRepository.customRadius.value = null }
                                )
                            }
                        }
                        Slider(
                            value = customRadius ?: dynamicRadius,
                            onValueChange = { AppRepository.customRadius.value = it },
                            valueRange = 100f..10000f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        modifier = Modifier.weight(1f).height(50.dp),
                        enabled = !isAlarmActive,
                        onClick = {
                            AppRepository.isAlarmActive.value = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(Intent(context, LocationAlarmService::class.java))
                            } else {
                                context.startService(Intent(context, LocationAlarmService::class.java))
                            }
                        }
                    ) {
                        Text(if (isAlarmActive) "Alarm Active" else "Start Alarm")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        modifier = Modifier.weight(1f).height(50.dp),
                        enabled = isAlarmActive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
                        ),
                        onClick = {
                            AppRepository.isAlarmActive.value = false
                            AlarmController.stopAlarm()
                            context.stopService(Intent(context, LocationAlarmService::class.java))
                        }
                    ) {
                        Text("Stop")
                    }
                }
            }

            AlarmList(
                alarms = savedAlarmUiModels,
                onAlarmSelected = { alarm ->
                    if (!isAlarmActive) {
                        AppRepository.targetLocation.value = alarm.geoPoint
                        AppRepository.customRadius.value = alarm.radiusMeters.toFloat()
                        searchQuery = alarm.name
                    }
                },
                onAlarmDeleted = { alarmUi ->
                    savedAlarmsEntities.find { it.id == alarmUi.id }?.let { entity ->
                        mainViewModel.deleteAlarm(entity)
                    }
                }
            )
        }
    }
}

@Composable
fun StatBox(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
