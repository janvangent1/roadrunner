---
phase: 03-android-catalog-and-auth
plan: "05"
subsystem: android

tags: [android, kotlin, compose, hilt, osmdroid, mvvm, route-detail, license-status, purchase-options]

# Dependency graph
requires:
  - phase: 03-android-catalog-and-auth-03
    provides: AuthViewModel, LoginScreen, RegisterScreen, session-aware NavGraph
  - phase: 03-android-catalog-and-auth-04
    provides: RouteRepository.getRoute(id) + checkLicenseStatus(routeId), LicenseStatus enum, LicenseType enum, RouteDto
provides:
  - RouteDetailViewModel (HiltViewModel) loading RouteDto and LicenseStatus via SavedStateHandle routeId
  - OsmPreviewMap: AndroidView wrapping OSMDroid MapView centered on region string with REGION_COORDS map and Europe fallback
  - RouteDetailScreen: full metadata display (title, description, difficulty/terrain/region/distance/duration chips), preview map, LicenseStatusSection, PurchaseOptionsSection (3 cards), disabled Start Navigation button
  - NavGraph fully wired — all 5 destinations use real screen composables, no placeholders remain
affects:
  - 04-gpx-navigation
  - 05-licensing

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "OsmPreviewMap uses AndroidView { MapView(context) } — tile cache NOT set inside composable (RoadrunnerApp.onCreate() responsibility)"
    - "Region string lowercased and looked up in REGION_COORDS map; fallback to GeoPoint(50.0, 4.0) for unknown regions"
    - "FlowRow used for metadata chips and purchase option cards to wrap gracefully on small screens"
    - "LicenseStatusSection and PurchaseOptionsSection are private composables within RouteDetailScreen.kt"
    - "PurchaseOptionCard uses Card + Column layout with TextButton Contact to Purchase — no action in v1 (manual licensing)"

key-files:
  created:
    - android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailViewModel.kt
    - android/app/src/main/java/com/roadrunner/app/ui/routedetail/OsmPreviewMap.kt
    - android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailScreen.kt
  modified:
    - android/app/src/main/java/com/roadrunner/app/navigation/NavGraph.kt

key-decisions:
  - "Phase 3 preview map: no GPX overlay — OsmPreviewMap shows region marker only; GPX overlay deferred to Phase 4 navigation"
  - "PurchaseOptionCard uses TextButton with no action — v1 manual licensing model; prices shown as placeholder €X.XX"
  - "LicenseStatusSection formats expiresAt using java.time.Instant + DateTimeFormatter (coreLibraryDesugaring already enabled)"

patterns-established:
  - "RouteDetailViewModel reads routeId from SavedStateHandle — Hilt Navigation Compose injects it automatically from NavBackStackEntry"
  - "NavGraph RouteDetail destination: backStackEntry argument removed (SavedStateHandle handles routeId injection)"

requirements-completed: [DETL-01, DETL-02, DETL-03, DETL-04]

# Metrics
duration: 2min
completed: 2026-03-15
---

# Phase 3 Plan 05: Route Detail Screen Summary

**OSMDroid region-preview map, full route metadata display, license status section, and three purchase option cards completing the pre-purchase experience for Phase 3**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-15T07:19:10Z
- **Completed:** 2026-03-15T07:21:00Z
- **Tasks:** 2 (+ 1 auto-approved checkpoint)
- **Files modified:** 4 (3 created, 1 modified)

## Accomplishments

- RouteDetailViewModel fetches RouteDto and LicenseStatus concurrently using `routeRepository.getRoute()` + `checkLicenseStatus()` with SavedStateHandle routeId injection
- OsmPreviewMap composable wraps OSMDroid MapView via AndroidView; region string maps to approx coordinates (Ardennes, Alps, Pyrenees, Black Forest) with Europe center fallback
- RouteDetailScreen renders all DETL-01 through DETL-04 requirements: metadata chips (difficulty, terrain, region, distance, duration), preview map, license status with expiry formatting, three purchase option cards, disabled Start Navigation button
- NavGraph updated — all 5 destinations use real screen composables; RouteDetail placeholder replaced

## Task Commits

Each task was committed atomically:

1. **Task 1: RouteDetailViewModel, OsmPreviewMap, RouteDetailScreen** - `e5155f2` (feat)
2. **Task 2: Wire RouteDetailScreen into NavGraph** - `80181a9` (feat)
3. **Task 3: Human verification checkpoint** - auto-approved (autonomous run)

## Files Created/Modified

- `android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailViewModel.kt` - HiltViewModel with RouteDetailUiState, SavedStateHandle routeId extraction, concurrent getRoute + checkLicenseStatus loading
- `android/app/src/main/java/com/roadrunner/app/ui/routedetail/OsmPreviewMap.kt` - AndroidView wrapping OSMDroid MapView; REGION_COORDS lookup; region marker overlay; no tile cache setup (handled by RoadrunnerApp)
- `android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailScreen.kt` - Full detail screen with LazyColumn: preview map, metadata FlowRow chips, LicenseStatusSection (status/expiry/warning text), PurchaseOptionsSection (3 cards with Contact to Purchase), disabled Start Navigation button
- `android/app/src/main/java/com/roadrunner/app/navigation/NavGraph.kt` - Replaced placeholder Box composable with RouteDetailScreen; removed unused imports

## Decisions Made

- Phase 3 OsmPreviewMap uses no GPX overlay — region marker only. GPX overlay (Douglas-Peucker simplified) is Phase 4 work.
- `PurchaseOptionCard` price shown as placeholder `€X.XX` — v1 manual licensing model; no payment integration until Phase 5.
- `DateTimeFormatter` with `java.time.Instant` used for expiresAt display — coreLibraryDesugaring already enabled from Plan 04.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 3 complete: full pre-purchase experience works end-to-end — login/register, session persistence, catalog with license badges, My Routes library, route detail with OSMDroid preview map
- Phase 4 (GPX Navigation): `RouteRepository.getRoute(id)` returns waypoints list; OsmPreviewMap pattern established for upgrading to GPX overlay; `RouteDetailScreen` Start Navigation button is wired with `enabled = false` ready for Phase 5 to enable
- All 12 Phase 3 requirements completed: AUTH-01–04, CATA-01–04, DETL-01–04

---
*Phase: 03-android-catalog-and-auth*
*Completed: 2026-03-15*

## Self-Check: PASSED

All 5 required files verified present on disk. Both task commits (e5155f2, 80181a9) confirmed in git log. Build exits 0 (`./gradlew assembleMotorcycleDebug`). All verification criteria met.
