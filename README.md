# 💤 NapStop — Location-Based Commute Alarm

**NapStop** is a lightweight, location-based alarm application designed specifically for commuters, travelers, and public transit nappers. By leveraging GPS tracking and customizable geofences, NapStop ensures you never miss your bus stop, train station, or destination again—allowing you to snooze peacefully with full peace of mind.

---

## 🚀 Key Features

*   **Precise Geofenced Alarms**: Set up a destination and wake up precisely when you cross into the designated arrival radius.
*   **Saved Locations (Room Database)**: Save recurring commutes, workspaces, or home locations for one-tap tracking.
*   **Dynamic Radius Adjustment**: Customize the warning boundary radius (e.g., from 100 meters up to 10 kilometers) to fit high-speed trains or short suburban walking routes.
*   **Smart Audio & Headphone Checks**: Intelligently verifies headphone connectivity to route warnings appropriately, preventing loud disturbances to nearby passengers if headphones are detected.
*   **Interactive Map Controls**: Drag-and-drop map markers (powered by OpenStreetMap via Osmdroid) with a helper button to instantly snap centering back to your live location or active target destination.
*   **Background Activity Restrictions Mitigation**: Handles wakelocks and requests "Allow all the time" location access to keep tracking active even when the screen is off or the device is sleeping.

---

## 🛠️ Built With

*   **Language**: [Kotlin](https://kotlinlang.org/) — 100% type-safe, expressive, and modern.
*   **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) — Custom designed Material 3 components, fluid animations, and responsive window density.
*   **Architecture**: Unidirectional Data Flow pattern with a centralized Repository.
*   **Database**: [Room SQLite](https://developer.android.com/training/data-storage/room) — Offline local persistence for saved coordinate targets.
*   **Map Integration**: [Osmdroid (OpenStreetMap)](https://github.com/osmdroid/osmdroid) — Free, customizable open-source mapping.
*   **Testing Infrastructure**: 
    *   **Robolectric**: Fast, headless JVM integration tests for UI compose nodes and ViewModels.
    *   **Roborazzi**: High-fidelity visual snapshot and regression testing.

---

## 🧪 Testing and Verification

NapStop has robust local JUnit, Robolectric, and screenshot test coverage. You can run tests easily with the following Gradle commands:

### Run unit and integration tests
```bash
gradle :app:testDebugUnitTest
```

### Record / Update UI Screenshot Ref (Roborazzi)
```bash
gradle :app:recordRoborazziDebug
```

---

## 📦 Getting Started

1. **Clone the project** or export it directly from Google AI Studio.
2. **Open in Android Studio** (Ladybug or newer recommended).
3. **Build and Run** on a device/emulator running Android 10 (API 29) or higher.
4. Make sure to grant **Location Permissions** (Background Location recommended) and **Notification Permissions** to enable real-time transit tracking and foreground services.
