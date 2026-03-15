---
phase: 06-navigation-experience
plan: 01
subsystem: navigation
tags: [gps, fused-location, osmdroid, haversine, hilt, stateflow, viewmodel, tdd]

requires:
  - phase: 05-license-enforcement
    provides: NavigationSessionManager and NavigationScreen stub consumed here

provides:
  - LocationRepository: FusedLocationProviderClient wrapper emitting StateFlow<Location?>
  - OffRouteDetector: pure haversine point-to-polyline utility with hysteresis support
  - NavigationViewModel: HiltViewModel owning all navigation state via NavigationUiState StateFlow
  - play-services-location:21.3.0 and work-runtime-ktx:2.9.1 in build.gradle.kts

affects:
  - 06-02 (NavigationScreen map overlay consumes NavigationViewModel and LocationRepository)
  - 06-03 (tile caching uses work-runtime-ktx added here)

tech-stack:
  added:
    - com.google.android.gms:play-services-location:21.3.0
    - androidx.work:work-runtime-ktx:2.9.1
  patterns:
    - LocationRepository @Singleton with FusedLocationProviderClient, class-level LocationCallback
    - OffRouteDetector as Kotlin object (pure math, no DI)
    - Haversine + segment projection with dot-product parametric t clamped to [0,1]
    - Off-route hysteresis: 50m activate / 40m clear thresholds
    - NavigationViewModel exposes single StateFlow<NavigationUiState> updated via .update{}
    - TDD: failing test commit, then implementation commit

key-files:
  created:
    - android/app/src/main/java/com/roadrunner/app/data/location/LocationRepository.kt
    - android/app/src/main/java/com/roadrunner/app/navigation/OffRouteDetector.kt
    - android/app/src/main/java/com/roadrunner/app/ui/navigation/NavigationViewModel.kt
    - android/app/src/test/java/com/roadrunner/app/navigation/OffRouteDetectorTest.kt
  modified:
    - android/app/build.gradle.kts

key-decisions:
  - "LocationCallback is class-level val in LocationRepository so stopLocationUpdates() can unregister the same instance"
  - "OffRouteDetector is an object (not a class) since it is pure math with no DI requirements"
  - "haversineMetres() is private and duplicated in NavigationViewModel (for Location-to-Location) and OffRouteDetector (for GeoPoint-to-GeoPoint) to avoid cross-layer coupling"
  - "NavigationViewModel accumulates distanceCoveredKm in memory using haversine between consecutive locations"
  - "GPX parse failure in NavigationViewModel is non-fatal: routePoints = emptyList() and navigation continues without off-route detection"

patterns-established:
  - "LocationRepository pattern: @Singleton + FusedLocationProviderClient + class-level LocationCallback"
  - "Geometry utility pattern: Kotlin object with haversineMetres + distanceToSegment + minDistanceToPolyline"
  - "NavigationUiState pattern: single data class with all screen state, updated via StateFlow.update{}"

requirements-completed: [NAV-03, NAV-04, NAV-05]

duration: 4min
completed: 2026-03-15
---

# Phase 06 Plan 01: Navigation Data Layer Summary

**GPS streaming via FusedLocationProviderClient, haversine off-route detection (50m/40m hysteresis), and NavigationViewModel with single StateFlow<NavigationUiState> owning all navigation state**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-15T12:55:13Z
- **Completed:** 2026-03-15T12:59:37Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- LocationRepository wraps FusedLocationProviderClient, emits StateFlow<Location?>, starts/stops with a stable callback reference
- OffRouteDetector provides haversine point-to-polyline math with segment projection clamped to [0,1]; all 5 unit tests pass
- NavigationViewModel owns NavigationUiState (location, speed, distance, elapsed time, off-route flag, route points, waypoints) with GPX parse via android-gpx-parser

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Gradle dependencies and LocationRepository** - `911fdf3` (feat)
2. **Task 2 RED: Failing OffRouteDetector tests** - `eba46ea` (test)
3. **Task 2 GREEN: OffRouteDetector and NavigationViewModel** - `a8f8aba` (feat)

**Plan metadata:** (docs commit follows)

_Note: TDD tasks may have multiple commits (test -> feat -> refactor)_

## Files Created/Modified

- `android/app/build.gradle.kts` - Added play-services-location:21.3.0 and work-runtime-ktx:2.9.1
- `android/app/src/main/java/com/roadrunner/app/data/location/LocationRepository.kt` - FusedLocationProviderClient wrapper with StateFlow<Location?>
- `android/app/src/main/java/com/roadrunner/app/navigation/OffRouteDetector.kt` - Haversine point-to-polyline distance utility (object)
- `android/app/src/main/java/com/roadrunner/app/ui/navigation/NavigationViewModel.kt` - HiltViewModel with NavigationUiState StateFlow, loadRoute, startTracking, stopTracking
- `android/app/src/test/java/com/roadrunner/app/navigation/OffRouteDetectorTest.kt` - 5 unit tests for OffRouteDetector

## Decisions Made

- LocationCallback stored as class-level val to ensure stopLocationUpdates() unregisters the exact same callback instance registered in startLocationUpdates()
- OffRouteDetector is a Kotlin object (no DI) since it is a pure math utility with no state
- haversineMetres() duplicated in both OffRouteDetector and NavigationViewModel to avoid cross-layer coupling (different argument types: GeoPoint vs Location)
- GPX parsing failures are non-fatal in NavigationViewModel: routePoints falls back to emptyList(), navigation continues without off-route detection

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- LocationRepository, OffRouteDetector, and NavigationViewModel are fully implemented and ready for Plan 02 (NavigationScreen map overlay)
- work-runtime-ktx is in place for Plan 03 (tile caching)
- No blockers

---
*Phase: 06-navigation-experience*
*Completed: 2026-03-15*
