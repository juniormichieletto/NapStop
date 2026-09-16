package com.napstop

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.napstop.ui.LocationAlarmScreen
import com.napstop.ui.components.MapContent
import com.napstop.ui.theme.MyApplicationTheme
import org.osmdroid.util.GeoPoint

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MyApp()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MyApp() {
    val permissionsList = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissionsList.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    val permissionsState = rememberMultiplePermissionsState(permissionsList)
    val context = LocalContext.current

    val hasBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        if (permissionsState.allPermissionsGranted) {
            MainScreen(
                modifier = Modifier.padding(innerPadding),
                hasBackgroundLocation = hasBackgroundLocation
            )
        } else {
            PermissionDeniedScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    hasBackgroundLocation: Boolean = true,
    mainViewModel: MainViewModel = viewModel()
) {
    LocationAlarmScreen(
        modifier = modifier,
        hasBackgroundLocation = hasBackgroundLocation,
        mainViewModel = mainViewModel
    )
}

@Composable
fun StatBox(title: String, value: String) {
    com.napstop.ui.StatBox(title = title, value = value)
}

@Composable
fun MapKitDisplay(
    targetLocation: GeoPoint?,
    currentLocation: Location?,
    dynamicRadius: Float,
    customRadius: Float?,
    forceCenterTrigger: Long = 0L,
    explicitCenterPoint: GeoPoint? = null,
    onTargetSelected: (GeoPoint) -> Unit
) {
    MapContent(
        targetLocation = targetLocation,
        currentLocation = currentLocation,
        dynamicRadius = dynamicRadius,
        customRadius = customRadius,
        forceCenterTrigger = forceCenterTrigger,
        explicitCenterPoint = explicitCenterPoint,
        onTargetSelected = onTargetSelected
    )
}

@Composable
fun PermissionDeniedScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Location and Notification permissions are required.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(32.dp)
        )
    }
}
