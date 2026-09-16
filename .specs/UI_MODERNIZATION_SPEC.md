# Location Alarm — UI Modernization Specification

## 1. Objective

Modernize the application's main screen so that the product feels like a polished **location-alarm application**, rather than a map utility that stores coordinates.

The redesign should improve:

* visual hierarchy;
* clarity of the core use case;
* discoverability of location alarms;
* map readability;
* alarm creation flow;
* Material 3 consistency;
* dark-mode quality;
* maintainability of the UI architecture.

The implementation should be incremental. Each phase should leave the application fully usable and shippable.

Android's current design stack already provides Material 3 components for search, bottom sheets, cards, buttons and related surfaces, so the redesign should prefer standard components before custom implementations. ([Android Developers][1])

---

# 2. Design Principles

### 2.1 The alarm is the product; the map is context

The current screen visually emphasizes the map and coordinates.

The new hierarchy should be:

**Location Alarm → Place → Trigger → Radius → Map**

Not:

**Map → Coordinates → Saved Point**

This principle should guide every UI decision.

### 2.2 Progressive disclosure

Do not expose all controls simultaneously.

The home screen should answer three questions immediately:

* Where are my alarms?
* Which ones are active?
* How do I create another one?

Editing radius, trigger type, exact coordinates and destructive actions can appear only when required.

### 2.3 One primary action

There should be one clearly dominant action:

**Add alarm**

Secondary operations such as locating the user, editing, pausing and deleting should have lower visual priority. Android's Material guidance similarly recommends reserving the highest prominence/FAB level for a single primary action and moving less frequent actions into menus. ([Android Developers][2])

---

# 3. Target Home Screen Structure

The eventual screen hierarchy should be:

```text
LocationAlarmScreen
│
├── FullScreenMap
│   ├── SearchBar
│   ├── SelectedLocationMarker
│   ├── GeofenceRadiusOverlay
│   └── CurrentLocationButton
│
└── AlarmBottomSheet
    ├── DragHandle
    ├── Header
    │   ├── "Location alarms"
    │   └── Alarm count
    │
    ├── AlarmList
    │   └── AlarmListItem[]
    │
    └── AddAlarmButton
```

The map should continue underneath the system bars and bottom sheet where appropriate.

For Android 15 / API 35 targets, edge-to-edge is already the default behavior, so system insets should be handled deliberately rather than treating the status/navigation bars as separate opaque regions. ([Android Developers][3])

---

# 4. Information Architecture Changes

## Existing concept

```text
Saved Stops
 └── Place
      └── latitude / longitude
```

## New concept

```text
Location Alarms
 └── Alarm
      ├── Place
      ├── Radius
      ├── Trigger
      ├── Enabled state
      └── Coordinates
```

Coordinates remain part of the domain model but should not normally appear on the home screen.

### Proposed model

```kotlin
data class LocationAlarm(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val trigger: GeofenceTrigger,
    val enabled: Boolean
)

enum class GeofenceTrigger {
    ENTER,
    EXIT,
    ENTER_OR_EXIT
}
```

If the existing domain model cannot be changed immediately, introduce a UI model:

```kotlin
data class LocationAlarmUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val enabled: Boolean
)
```

That prevents the UI redesign from becoming coupled to the persistence migration.

---

# 5. Home Screen Specification

## 5.1 Map

The map becomes the full-screen visual foundation.

### Requirements

* Fill available screen bounds.
* Draw behind status bar.
* Use appropriate `WindowInsets`.
* Keep map interaction enabled.
* Reduce visual clutter where the map provider allows styling.
* Maintain readable contrast between map and alarm overlays.

Desired visual hierarchy:

```text
Alarm marker       HIGH
Alarm radius       HIGH/MEDIUM
Primary roads      MEDIUM
Map labels         MEDIUM/LOW
Minor roads        LOW
POIs                LOW
```

The map must not compete with the alarms.

---

# 6. Search

Replace the oversized existing search control with a Material 3-style floating search surface.

Target:

```text
╭─────────────────────────────╮
│ 🔍  Search location      ⚙ │
╰─────────────────────────────╯
```

### Requirements

* Horizontal margin: approximately `16dp`.
* Height around `56dp`.
* Rounded shape.
* Surface visually separated from the map.
* Placeholder:

**Search location**

* Leading search icon.
* Optional trailing control only if there is a real function for it.

Do not add decorative controls just because the mockup has one.

Material 3 provides dedicated `SearchBar` APIs and expanded search states for suggestions/results. The current API also supports full-screen and docked search patterns. ([Android Developers][4])

### Search states

```text
Idle
Focused
Typing
Loading
Results
No results
Error
```

The redesign must explicitly support each state.

---

# 7. Bottom Sheet

Replace the static content beneath the map with a persistent bottom sheet.

Use:

```kotlin
BottomSheetScaffold
```

rather than constructing a custom draggable surface unless there is a hard technical requirement.

Material 3 provides `BottomSheetScaffold` and `ModalBottomSheet`, including system inset integration. ([Android Developers][1])

## Sheet states

Minimum:

```text
Collapsed
Expanded
```

Potential later state:

```text
Partially Expanded
```

### Collapsed

Show enough information to understand that alarms exist.

Example:

```text
━━━━━━━━

Location alarms          2

Willesden Green
300 m · Arriving

[ + Add alarm ]
```

### Expanded

Show the full alarm list.

---

# 8. Bottom Sheet Header

Replace:

**Saved Stops**

with:

**Location alarms**

Optional secondary description:

**Get notified when you reach your places**

Do not permanently display tutorial text such as:

> Tap on map or search above to set stop.

That is onboarding, not primary interface content.

For first use, use either:

* transient coach mark;
* tooltip;
* empty-state copy.

---

# 9. Alarm List Item

Current:

```text
pin
Willesden Green Station
51.5490, -0.2212
trash
```

Target:

```text
╭──────────────────────────────────────╮
│ 📍  Willesden Green Station          │
│     300 m · Arriving     Active   ⋮ │
╰──────────────────────────────────────╯
```

## Information hierarchy

### Primary

```text
Willesden Green Station
```

### Secondary

```text
300 m · Arriving
```

or:

```text
500 m · Leaving
```

### Tertiary

State:

```text
Active
Paused
```

### Overflow menu

```text
Edit
Pause / Resume
Duplicate
Delete
```

Delete should no longer have a permanently visible trash icon.

This removes unnecessary visual noise and moves infrequent operations into an overflow action, which aligns with Android's recommended action hierarchy. ([Android Developers][2])

---

# 10. Alarm Status

Alarm state needs to be visually obvious without relying exclusively on color.

## Active

```text
🔔 Active
```

## Paused

```text
🔕 Paused
```

Do not communicate status using green/gray alone.

Use:

```text
icon + text + color
```

for accessibility.

Material 3's tonal color system is designed around accessible color roles and contrast rather than arbitrary raw colors. ([Android Developers][5])

---

# 11. Geofence Visualization

This is one of the highest-value changes in the redesign.

When a location is selected or an alarm is focused:

```text
        ...............
     ...               ...
   ..                     ..
  .           📍            .
   ..                     ..
     ...               ...
        ...............
```

Display:

* location marker;
* semi-transparent radius fill;
* radius border.

Example:

```text
radius = 300m
```

should be directly represented geographically.

This makes the app's central concept visually self-explanatory.

---

# 12. Current Location Control

The existing location control should remain secondary.

Target size:

```text
48–56dp
```

Placement:

```text
map bottom-right
```

but positioned above the bottom sheet.

Possible icon:

```text
◎
```

Behavior:

```text
Tap
↓
request permission if necessary
↓
animate map to current position
```

It should never visually compete with `Add alarm`.

---

# 13. Primary Action

Use:

```text
+ Add alarm
```

as the dominant interaction.

Recommended final location:

inside the bottom sheet.

Example:

```text
╭─────────────────────────────╮
│           + Add alarm       │
╰─────────────────────────────╯
```

Avoid simultaneously displaying:

* giant FAB;
* giant sheet button;
* map CTA;

for the same action.

One action, one dominant representation.

---

# 14. Add Alarm Flow

Long-term flow:

```text
User taps Add alarm
        ↓
Search or select location
        ↓
Marker appears
        ↓
Geofence radius appears
        ↓
Alarm configuration sheet
```

Configuration sheet:

```text
Willesden Green Station

Notify me

[ Arriving ] [ Leaving ]

Distance

100m ───────●──────── 2km

300 m

[ Create alarm ]
```

Recommended defaults:

```text
Trigger: Arriving
Radius: 300m
Enabled: true
```

Exact defaults should eventually be informed by product usage rather than pure design preference.

---

# 15. Selecting Directly on the Map

Map tapping should remain supported.

Flow:

```text
Tap map
↓
temporary marker
↓
reverse geocode
↓
show place
↓
show configuration sheet
```

While geocoding:

```text
Selected location
Finding place…
```

Do not block the interaction unnecessarily.

---

# 16. Dark Theme

Move away from pure black + generic Android blue.

Use semantic Material color roles:

```text
background
surface
surfaceContainer
surfaceContainerHigh
primary
primaryContainer
onPrimary
onSurface
onSurfaceVariant
error
```

Approximate design direction:

```text
Background          #090B0F
Surface             #11151B
Elevated surface    #181D25

Primary             modern blue
Active              semantic green
Error               semantic red
```

These are design references, not hardcoded requirements.

Prefer theme tokens:

```kotlin
MaterialTheme.colorScheme.surface
MaterialTheme.colorScheme.primary
```

rather than:

```kotlin
Color(0xFF...)
```

throughout individual composables.

---

# 17. Shape System

Introduce consistent shape tokens.

For example:

```text
Small       8dp
Medium      12dp
Large       20dp
XLarge      28dp
```

Suggested usage:

```text
Search bar        28dp
Bottom sheet      28dp top corners
Alarm cards       16–20dp
Primary button    24–28dp
Status chips      pill
```

The exact dimensions matter less than consistency.

---

# 18. Spacing System

Use a 4dp-based scale:

```text
4
8
12
16
20
24
32
```

Avoid arbitrary values such as:

```text
13dp
19dp
27dp
```

unless a component genuinely requires them.

Recommended baseline:

```text
Screen horizontal padding    16dp
Card internal padding        16dp
Card spacing                 8–12dp
Section spacing              24dp
```

---

# 19. Typography

Use Material typography tokens.

Suggested hierarchy:

```text
Location alarms
→ titleLarge

Willesden Green Station
→ titleMedium

300 m · Arriving
→ bodyMedium

Active
→ labelMedium

Add alarm
→ labelLarge
```

Do not create a forest of custom font sizes.

---

# 20. Animation

Animation should communicate state, not decorate the app.

Recommended transitions:

### Bottom sheet

Spring-based movement.

### New marker

```text
scale: 0.8 → 1
alpha: 0 → 1
```

### Geofence

```text
radius expands
alpha fades in
```

### Alarm enabled/paused

Animate status chip and marker appearance.

### New alarm creation

Map:

```text
camera → location
```

then sheet:

```text
collapsed → configuration
```

Avoid gratuitous animations on every list interaction.

---

# 21. Accessibility

At minimum:

* meaningful content descriptions;
* touch targets around `48dp`;
* status not represented exclusively by color;
* scalable text;
* contrast-aware theme;
* system font-scale testing;
* TalkBack testing.

Material components provide a useful accessibility foundation, but custom map controls and markers still need explicit semantics. ([Android Developers][5])

---

# 22. Edge-to-Edge

Implement proper edge-to-edge early rather than near the end.

Target:

```text
Status bar
      ↓
MAP CONTINUES

Navigation bar
      ↑
BOTTOM SHEET / CONTENT CONTINUES
```

Interactive controls must respect safe insets.

On Android 15+, applications targeting SDK 35 are edge-to-edge by default; Android recommends consuming system insets so important controls remain visible and tappable. ([Android Developers][3])

---

# Incremental Roadmap

The important part: **do not start with the bottom sheet rewrite.**

First clean the existing design with low-risk changes. Then change the interaction model.

---

## Phase 0 — Refactor Before Redesign

### Goal

Separate existing UI logic enough that visual changes don't break alarm behavior.

### Work

Create / isolate:

```text
LocationAlarmScreen
MapContent
SearchLocationBar
AlarmList
AlarmListItem
CurrentLocationButton
```

Move state into:

```text
ViewModel
↓
UiState
```

Example:

```kotlin
data class LocationAlarmScreenState(
    val alarms: List<LocationAlarmUiModel>,
    val selectedLocation: LatLng?,
    val searchQuery: String,
    val isSearching: Boolean
)
```

### Do not change

* business logic;
* persistence;
* geofence registration;
* navigation;
* map behavior.

### Outcome

Same UI, cleaner architecture.

### Risk

Low.

---

# Phase 1 — Alarm Cards

### Goal

Get the biggest visible improvement with almost no structural change.

### Changes

Replace:

```text
Place name
coordinates
trash
```

with:

```text
Place name
radius · trigger
status
overflow
```

Example:

```text
Willesden Green Station
300 m · Arriving            Active ⋮
```

### Add

Overflow menu:

```text
Edit
Pause
Delete
```

### Remove

Coordinates from home.

### Keep

Existing screen layout.

### Outcome

The app starts presenting **alarms instead of coordinates**.

### Risk

Very low.

### Release

`v1 UI refresh`

---

# Phase 2 — Visual Design System

### Goal

Fix inconsistency before restructuring the screen.

Implement:

```text
ColorScheme
Typography
Shapes
Spacing
Reusable AlarmCard
Reusable status chip
```

Also:

* modernize dark theme;
* reduce oversized elements;
* modernize search appearance;
* improve icon consistency.

### Outcome

The current layout becomes materially more polished.

### Risk

Low.

---

# Phase 3 — Edge-to-Edge

### Goal

Modernize the overall visual canvas.

Implement:

```kotlin
enableEdgeToEdge()
```

and proper inset handling.

Map should extend behind the status bar.

Controls must remain inside safe interaction regions.

Material 3 components and `Scaffold` can handle significant parts of the inset behavior, although the application still needs to consume the provided inset padding correctly. ([Android Developers][6])

### Outcome

Immediate modern Android appearance.

### Risk

Low–medium because keyboard and device-specific inset issues need testing.

---

# Phase 4 — Geofence Visualization

### Goal

Visually explain what a location alarm does.

Add:

```text
marker
+
geofence radius circle
```

When selecting an alarm:

```text
tap alarm
↓
map camera moves
↓
marker highlighted
↓
radius displayed
```

### Outcome

Huge UX improvement without changing the overall layout.

### Risk

Medium.

---

# Phase 5 — Bottom Sheet Migration

### Goal

Replace the current map/list split with the target layout.

Before:

```text
MAP

instruction

Saved Stops
```

After:

```text
MAP
██████████████
Bottom Sheet
```

Use:

```kotlin
BottomSheetScaffold
```

### Sheet content

```text
drag handle

Location alarms                 2

Alarm 1
Alarm 2

+ Add alarm
```

### Outcome

This is the point at which the application will visually resemble the generated concept.

### Risk

Medium.

---

# Phase 6 — New Add Alarm Flow

### Goal

Move alarm creation into a unified map workflow.

Implement:

```text
Add alarm
↓
select/search place
↓
configure radius
↓
configure trigger
↓
save
```

Use the bottom sheet for configuration.

Avoid introducing another full-screen page unless necessary.

### Outcome

Much more fluid UX.

### Risk

Medium-high because this touches state and navigation.

---

# Phase 7 — Search Redesign

### Goal

Turn search into a first-class location selection experience.

Implement:

```text
Idle search
↓
focused
↓
autocomplete
↓
results
↓
location selected
```

Potential screen:

```text
╭─────────────────────────────╮
│ ← Willesden               × │
├─────────────────────────────┤
│ 📍 Willesden Green Station  │
│ 📍 Willesden Junction       │
│ 📍 Willesden Library        │
╰─────────────────────────────╯
```

Material 3's newer search API separates the input/search-bar state from expanded full-screen or docked results, which maps well to this interaction. ([Android Developers][4])

---

# Phase 8 — Editing Radius on Map

### Goal

Make geofence configuration visual.

Add slider:

```text
100m ───────●──────── 2km

          300m
```

Simultaneously update the circle on the map.

Interaction:

```text
slider →
radius changes →
map circle changes immediately
```

Optional later enhancement:

drag the radius directly on the map.

Do **not** implement direct radius dragging initially. It adds complexity with questionable value compared with a slider.

---

# Phase 9 — Interaction Polish

Add:

* sheet transitions;
* marker transitions;
* map camera animation;
* loading skeletons if necessary;
* button states;
* empty state;
* error state;
* haptic feedback where appropriate.

This should happen **after** the UX works.

Polishing broken UX is just expensive lipstick.

---

# Phase 10 — Adaptive Layout

Only after the phone experience is solid.

Support:

```text
phone portrait
phone landscape
foldable
tablet
```

For larger screens, the alarm list can become a persistent side panel instead of a bottom sheet.

Android recommends adapting navigation and layout patterns to the available window size instead of simply stretching compact-phone UI across larger screens. ([Android Developers][2])

---

# Recommended Delivery Order

I'd structure development into these milestones:

| Milestone | Scope                        | User-visible impact | Technical risk |
| --------- | ---------------------------- | ------------------: | -------------: |
| **M0**    | UI architecture cleanup      |                None |            Low |
| **M1**    | Better alarm cards           |                High |       Very low |
| **M2**    | Theme + typography + spacing |                High |            Low |
| **M3**    | Edge-to-edge                 |              Medium |     Low/Medium |
| **M4**    | Geofence circle              |           Very high |         Medium |
| **M5**    | Bottom sheet                 |           Very high |         Medium |
| **M6**    | New add-alarm flow           |           Very high |    Medium/High |
| **M7**    | Search redesign              |                High |         Medium |
| **M8**    | Interactive radius           |                High |         Medium |
| **M9**    | Animation/polish             |              Medium |            Low |
| **M10**   | Tablet/foldable              |              Medium |         Medium |

---

# What I Would Build First

For the **first development iteration**, I would stop at Phase 4:

```text
Phase 0
Architecture cleanup

Phase 1
Alarm card redesign

Phase 2
Theme system

Phase 3
Edge-to-edge

Phase 4
Geofence radius visualization
```

That gets you roughly **60–70% of the perceived redesign** without touching the riskiest part of the application.

Then:

```text
Phase 5 → Phase 8
```

becomes the actual UX transformation.

That split matters. Trying to implement the generated mockup in one shot would mix **styling, navigation, state management, map behavior and geofence UX** in the same change. That's exactly how a cosmetic redesign turns into a regression factory.

[1]: https://developer.android.com/jetpack/androidx/releases/compose-material3?utm_source=chatgpt.com "Compose Material 3  |  Jetpack  |  Android Developers"
[2]: https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns?utm_source=chatgpt.com "Layouts and navigation patterns  |  Mobile  |  Android Developers"
[3]: https://developer.android.com/about/versions/15/behavior-changes-15?utm_source=chatgpt.com "Behavior changes: Apps targeting Android 15 or higher  |  Android Developers"
[4]: https://developer.android.com/reference/kotlin/androidx/compose/material3/SearchBar.composable?utm_source=chatgpt.com "SearchBar  |  API reference  |  Android Developers"
[5]: https://developer.android.com/develop/ui/compose/designsystems/material3?utm_source=chatgpt.com "Material Design 3 in Compose  |  Jetpack Compose  |  Android Developers"
[6]: https://developer.android.com/develop/ui/compose/system/insets?utm_source=chatgpt.com "About window insets  |  Jetpack Compose  |  Android Developers"
