# NapStop Developer & AI Agent Guidelines

This document outlines command references, architectural patterns, and project conventions for NapStop. Follow these guidelines during iterative feature development, bug investigation, or test runs.

---

## 🛠️ Essential Commands

### 📱 Build & Runtime
- **Build debug APK**:
  ```bash
  ./gradlew :app:assembleDebug
  ```
- **Run verification checks (Lint)**:
  ```bash
  ./gradlew :app:lint
  ```

### 🧪 Tests & Quality Assurance
- **Run local Robolectric unit and integration tests**:
  ```bash
  ./gradlew :app:testDebugUnitTest
  ```
- **Record/Update Roborazzi physical UI snapshot references**:
  ```bash
  ./gradlew :app:recordRoborazziDebug
  ```
- **Compare and verify against Roborazzi reference snapshots**:
  ```bash
  ./gradlew :app:verifyRoborazziDebug
  ```

---

## 🏗️ Architecture & Style Guidelines

### 1. State Flow & MVVM Pattern
- **Unidirectional Data Flow**: Maintain core states like `currentLocation` and `targetLocation` inside `AppRepository` or `MainViewModel`. Update screen composition via `collectAsStateWithLifecycle`.
- **Background Actions**: Leverage `LocationAlarmService` for persistent location checks inside Android foreground service constraints. Ensure graceful wake locks and background permission safety.

### 2. Modern Jetpack Compose & M3 Styling
- **Theme Consistency**: Utilize color palettes provided by `MaterialTheme.colorScheme` inside `/ui/theme/Theme.kt`. Avoid raw hex code strings.
- **Responsive Sizing**: Adopt container-based design concepts (`Modifier.fillMaxWidth()` with standard `Spacer()` padding) over hardcoded sizes to accommodate tablet layouts.
- **Interactive UI Testing**: Add `Modifier.testTag("tag_name")` to key buttons, fields, and custom panels to maintain Roborazzi screenshot and functional stability.

### 3. Maps & Sensors Integration
- **Osmdroid Map Configuration**: Coordinate markers and boundary polygons dynamically. Handle centering changes cleanly through custom animation sequences without creating visual lockups.
- **Location Updates**: Active foreground components use Fused Location Provider client with `PRIORITY_HIGH_ACCURACY` state configuration, falling back cleanly to last-known cached coordinates on startup.
