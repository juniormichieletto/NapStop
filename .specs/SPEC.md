# NapStop Application Specification

## 1. Overview
NapStop is an Android application designed for commuters who want to sleep on public transport without missing their stop. By setting a destination and a geofence radius, the app monitors the device's location in the background and triggers an alarm when the user enters the predefined radius of their destination.

## 2. Core Features
- **Location Geofencing**: Uses the Fused Location Provider client (`PRIORITY_HIGH_ACCURACY`) to track the user's location and calculate the distance to the target destination. 
- **Dynamic Radius**: Users can adjust the alarm trigger radius (e.g., from 100 meters up to multiple kilometers) via a dynamic slider, adapting to different commuting speeds (train vs. walking).
- **Background Tracking**: Leverages an Android Foreground Service (`LocationAlarmService`) with persistent wake locks and background location permissions to ensure alarms trigger even when the screen is off or the app is minimized.
- **Smart Alarm Routing**: Detects if headphones (wired or Bluetooth) are connected before playing the alarm. If headphones are detected, the alarm routes through the media stream at a safe volume to avoid disturbing others. Otherwise, it plays loudly over the device speaker.
- **Notification Controls**: The triggered alarm notification includes a direct "Stop" action button (using a BroadcastReceiver), allowing users to immediately silence the alarm without needing to open the app.
- **Saved Alarms (Room Database)**: Persists user-defined target locations (`SavedAlarm` entities) for quick reuse, avoiding the need to search the map repeatedly for regular commutes.
- **Interactive Map**: Integrates Osmdroid for an interactive map experience, allowing users to select destinations by tapping or searching, visualizing the geofence boundary dynamically on the map.

## 3. Architecture & Patterns
- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose adhering to Material 3 guidelines.
- **Pattern**: Unidirectional Data Flow / MVVM.
  - State management handles variables (`targetLocation`, `currentLocation`, `isAlarmActive`, `dynamicRadius`) within `AppRepository` flows, observed inside Composables (`collectAsStateWithLifecycle`).
- **Database**: Room SQLite for offline data persistence.
- **Location**: `com.google.android.gms.location` for high accuracy tracking.

## 4. UI Layout (MainScreen)
- **Top Bar**: Search bar for address resolution (Geocoding) and a recent/saved accesses menu.
- **Map View**: Central Osmdroid instance rendering the user's current location, target marker, and the trigger radius polygon.
- **Control Panel (Bottom)**: 
  - Dynamic Slider for radius adjustments.
  - "Start Alarm" and "Stop" buttons reflecting the active state of the alarm. 

## 5. Testing & Verification
- **Unit and Integration**: Robolectric tests (`testDebugUnitTest`) verifying repository logic, database interactions, and state updates.
- **UI Snapshot Testing**: Roborazzi screenshots (`recordRoborazziDebug` and `verifyRoborazziDebug`) for asserting visual correctness of Jetpack Compose nodes under varying device states.

## 6. Permissions Model
- **Foreground Location** (`ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`)
- **Background Location** (`ACCESS_BACKGROUND_LOCATION`)
- **Notifications** (`POST_NOTIFICATIONS`) for the foreground service. 

## 7. Build Constraints
- Minimum Android SDK: 29 (Android 10)
- Target Android SDK: 34 (Android 14) 
