---
phase: 06-navigation-experience
plan: 02
subsystem: ui
tags: [osmdroid, compose, gps, hud, polyline, waypoints, android, hilt, stateflow]

requires:
  - phase: 06-navigation-experience/06-01
    provides: NavigationViewModel, NavigationUiState, LocationRepository, OffRouteDetector

provides:
  - NavigationScreen: full-screen OSMDroid map with route polyline, GPS overlay, HUD, off-route banner, waypoint markers
  - ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION declared in AndroidManifest.xml

affects:
  - 06-03 (tile caching displayed on this same MapView)

tech-stack:
  added: []
  patterns:
    - Full-screen Compose Box with AndroidView MapView (no Scaffold/TopAppBar)
    - buildMapView pure function creates and configures MapView from routePoints and waypoints lists
    - MyLocationNewOverlay + enableFollowLocation() for auto-centering on GPS position
    - HUD composable: semi-transparent Card with 2x2 metric grid (speed/covered/remaining/elapsed)
    - OffRouteBanner: red Card shown conditionally from isOffRoute state
    - Waypoint Marker.subDescription carries emoji icon label (FUEL/WATER/CAUTION/INFO)

key-files:
  created: []
  modified:
    - android/app/src/main/AndroidManifest.xml
    - android/app/src/main/java/com/roadrunner/app/ui/navigation/NavigationScreen.kt

key-decisions:
  - "No Scaffold or TopAppBar — navigation screen is full-screen; back button floated in Box overlay at TopStart"
  - "buildMapView is a private pure function (not a composable) because MapView is a View, not a Composable"
  - "updateMapLocation is a no-op: MyLocationNewOverlay.enableFollowLocation() handles auto-centering"
  - "Waypoint emoji labels stored as unicode escapes to avoid source encoding issues with multi-codepoint emoji"
  - "Permission request via rememberLauncherForActivityResult; startTracking() called in result callback if granted"

patterns-established:
  - "Full-screen map pattern: Box fills screen, AndroidView MapView fills Box, composable overlays use Alignment modifiers"
  - "HUD pattern: CardDefaults.cardColors containerColor = Color.Black.copy(alpha = 0.65f) for semi-transparent overlay"

requirements-completed: [NAV-01, NAV-02, NAV-03, NAV-04, NAV-05, WAYPT-01, WAYPT-02]

duration: 4min
completed: 2026-03-15
---

# Phase 06 Plan 02: NavigationScreen Map UI Summary

**Full-screen OSMDroid map with route polyline, real-time GPS dot (MyLocationNewOverlay), HUD overlay (speed/distance/elapsed), off-route banner, and labeled waypoint pins — replacing the Phase 5 placeholder**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-03-15T13:03:22Z
- **Completed:** 2026-03-15T13:05:58Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- AndroidManifest.xml declares ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION
- NavigationScreen replaced: full-screen Box with AndroidView MapView, HUD Card, OffRouteBanner, floating back button
- Route polyline drawn from NavigationUiState.routePoints with blue 8px stroke
- MyLocationNewOverlay + CompassOverlay wired to map; GPS follows location automatically
- HUD shows speed (km/h), distance covered, distance remaining, elapsed time — all from ViewModel StateFlow
- Waypoint Marker overlays with label, type, and emoji subDescription per waypoint type
- Session expiry polling (30s LaunchedEffect) and ExpiryDialog preserved identically from Phase 5
- Permission request gate: startTracking() called only after ACCESS_FINE_LOCATION granted

## Task Commits

Each task was committed atomically:

1. **Task 1: Declare ACCESS_FINE_LOCATION in AndroidManifest** - `337f552` (feat)
2. **Task 2: Implement full NavigationScreen** - `59dc32a` (feat)

**Plan metadata:** (docs commit follows)

## Files Created/Modified

- `android/app/src/main/AndroidManifest.xml` - Added ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permissions
- `android/app/src/main/java/com/roadrunner/app/ui/navigation/NavigationScreen.kt` - Full implementation replacing Phase 5 stub

## Decisions Made

- No Scaffold or TopAppBar used — navigation is full-screen with floating composable overlays inside a Box
- `buildMapView` is a private pure function returning `MapView` (not a Composable) because OSMDroid MapView is a View not a Composable; it takes routePoints and waypoints directly
- `updateMapLocation` is intentionally a no-op: OSMDroid's `enableFollowLocation()` on `MyLocationNewOverlay` handles auto-centering without intervention
- Emoji characters stored as unicode escape sequences (`\u26fd`, `\ud83d\udca7`, etc.) to avoid source-file encoding issues with multi-codepoint emoji
- `startTracking()` called conditionally after permission check in `LaunchedEffect(Unit)` — if not granted, `permissionLauncher.launch()` triggers and `startTracking()` is called in the result callback

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- Java 8 JVM on PATH caused Gradle to fail: "Dependency requires at least JVM runtime version 11". Resolved by using the pre-installed OpenJDK 24 at `~/.jdks/openjdk-24.0.1` via `JAVA_HOME` override. This is a dev-environment issue, not a code issue.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- NavigationScreen fully implemented; all Phase 6 v1 navigation features are present except offline tile caching
- Plan 03 (tile caching via WorkManager) can now be executed — work-runtime-ktx was added in Plan 01
- No blockers

---
*Phase: 06-navigation-experience*
*Completed: 2026-03-15*
