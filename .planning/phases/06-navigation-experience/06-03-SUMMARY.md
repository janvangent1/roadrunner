---
phase: 06-navigation-experience
plan: 03
subsystem: ui
tags: [android, kotlin, osmdroid, workmanager, offline, tile-cache, okhttp]

# Dependency graph
requires:
  - phase: 06-01
    provides: WorkManager and OkHttp declared in build.gradle; NavigationSessionManager; RouteRepository.getDecryptedGpx
  - phase: 06-02
    provides: NavigationScreen and RouteDetailViewModel context

provides:
  - TileCacheManager singleton for bounding box tile enqueue
  - TileCacheWorker CoroutineWorker downloading OSM Mapnik tiles zoom 10-15
  - RouteDetailScreen "Download for offline use" button for licensed routes
  - downloadTiles() in RouteDetailViewModel that parses GPX bounding box

affects:
  - 07-branding-and-release

# Tech tracking
tech-stack:
  added: []
  patterns:
    - WorkManager UniqueWork with KEEP policy for idempotent tile download
    - Standard slippy map Web Mercator XYZ tile coordinate formula (lon2tile, lat2tile)
    - GPX bounding box extraction with 0.01 degree padding before tile enqueue

key-files:
  created:
    - android/app/src/main/java/com/roadrunner/app/data/tilecache/TileCacheManager.kt
    - android/app/src/main/java/com/roadrunner/app/data/tilecache/TileCacheWorker.kt
  modified:
    - android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailViewModel.kt
    - android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailScreen.kt

key-decisions:
  - "TileCacheWorker is a plain CoroutineWorker (not Hilt-injected) — WorkManager creates workers, DI not available at construction time"
  - "isTilesCached uses loose heuristic (zoom 10 dir non-empty) — per-route tracking is v2 concern"
  - "Tile storage uses cacheDir (not filesDir) — plan specifies cacheDir to match OSMDroid OfflineTileProvider path"
  - "downloadTiles() reads GPX bytes, parses with GPXParser, extracts min/max lat/lon with 0.01 degree padding before enqueueing"

patterns-established:
  - "Tile coordinate formula: lon2tile = (lon+180)/360 * 2^z; lat2tile uses Mercator projection with ln(tan(rad) + 1/cos(rad))"
  - "Y axis is inverted in tile coords: maxLat gives minY tile, minLat gives maxY tile"

requirements-completed:
  - NAV-06

# Metrics
duration: 4min
completed: 2026-03-15
---

# Phase 06 Plan 03: Offline Tile Pre-Caching Summary

**WorkManager job (TileCacheWorker) downloads OSM Mapnik tiles zoom 10-15 to cacheDir/osmdroid/tiles/Mapnik via OkHttp, triggered from a new "Download for offline use" button on the route detail screen for licensed routes**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-15T13:08:01Z
- **Completed:** 2026-03-15T13:12:04Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- TileCacheManager singleton with `isTilesCached()` and `enqueueTileDownload()` using WorkManager UniqueWork KEEP policy
- TileCacheWorker CoroutineWorker iterating zoom 10-15 with standard Web Mercator tile coordinate math, downloading each tile via OkHttp
- RouteDetailViewModel updated with TileCacheManager injection, `downloadTiles()` function parsing GPX bounding box, and three new UI state fields
- RouteDetailScreen gains "Download for offline use" OutlinedButton (shown only for licensed routes) and "Map tiles cached for offline use" label

## Task Commits

Each task was committed atomically:

1. **Task 1: TileCacheManager and TileCacheWorker** - `fcec6b3` (feat)
2. **Task 2: RouteDetailViewModel download trigger and RouteDetailScreen button** - `e2e026d` (feat)

## Files Created/Modified
- `android/app/src/main/java/com/roadrunner/app/data/tilecache/TileCacheManager.kt` - Singleton managing tile cache check and WorkManager enqueue
- `android/app/src/main/java/com/roadrunner/app/data/tilecache/TileCacheWorker.kt` - CoroutineWorker downloading OSM tiles for bounding box at zoom 10-15
- `android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailViewModel.kt` - Added TileCacheManager injection, downloadTiles(), and tile state fields
- `android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailScreen.kt` - Added OutlinedButton "Download for offline use" and cached tiles label

## Decisions Made
- TileCacheWorker is a plain CoroutineWorker (not Hilt-injected) because WorkManager creates workers directly; Hilt HiltWorker pattern would require additional setup not in plan scope
- isTilesCached uses a loose heuristic checking if zoom 10 directory has any files; per-route granularity is v2
- Tile storage path matches cacheDir/osmdroid/tiles/Mapnik as specified (OSMDroid OfflineTileProvider default)
- downloadTiles() is non-fatal on GPX parse failure — logs warning and skips tile enqueue rather than crashing

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Shell environment had Java 8 on PATH; Gradle requires Java 11+. Resolved by setting JAVA_HOME to the openjdk-24.0.1 installation found at ~/.jdks/. Not a code issue — environmental configuration only.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Offline tile pre-caching complete for NAV-06; navigation works without cell signal after download
- Phase 07 (branding and release) can proceed — all navigation experience features shipped

---
*Phase: 06-navigation-experience*
*Completed: 2026-03-15*

## Self-Check: PASSED

- FOUND: android/app/src/main/java/com/roadrunner/app/data/tilecache/TileCacheManager.kt
- FOUND: android/app/src/main/java/com/roadrunner/app/data/tilecache/TileCacheWorker.kt
- FOUND: android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailViewModel.kt
- FOUND: android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailScreen.kt
- FOUND: .planning/phases/06-navigation-experience/06-03-SUMMARY.md
- COMMIT fcec6b3: feat(06-03): add TileCacheManager and TileCacheWorker — FOUND
- COMMIT e2e026d: feat(06-03): add offline tile download trigger to route detail — FOUND
