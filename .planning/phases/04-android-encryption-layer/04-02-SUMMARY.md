---
phase: 04-android-encryption-layer
plan: 02
subsystem: ui
tags: [kotlin, tink, osmdroid, gpx, aes-256-gcm, retrofit, streaming, composable]

# Dependency graph
requires:
  - phase: 04-01
    provides: GpxCryptoManager with decryptToByteArray() and encryptedFileExists()
  - phase: 03-android-catalog-and-auth
    provides: RouteRepository, ApiService, RouteDetailScreen, OsmPreviewMap skeleton
provides:
  - Full download-store-render pipeline for encrypted GPX routes
  - ApiService.getRouteGpx() streaming endpoint
  - RouteRepository.downloadAndStoreGpx() writing raw encrypted bytes to filesDir/gpx/{routeId}.enc
  - RouteRepository.getDecryptedGpx() returning in-memory plaintext ByteArray only
  - OsmPreviewMap renders real GPX polyline from decrypted bytes; falls back to region marker
affects: [05-navigation, 07-branding-release]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Streaming Retrofit endpoint for large binary responses (@Streaming + ResponseBody)
    - Pipe encrypted server bytes directly to FileOutputStream — no plaintext intermediary
    - LaunchedEffect(routeId) pattern for async data loading in Composable with Dispatchers.IO
    - AndroidView update lambda for live map overlay changes driven by Compose state
    - GPXParser.parse(ByteArrayInputStream(bytes)) for in-memory GPX parsing without temp files

key-files:
  created: []
  modified:
    - android/app/src/main/java/com/roadrunner/app/data/remote/ApiService.kt
    - android/app/src/main/java/com/roadrunner/app/data/repository/RouteRepository.kt
    - android/app/src/main/java/com/roadrunner/app/ui/routedetail/OsmPreviewMap.kt
    - android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailViewModel.kt
    - android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailScreen.kt

key-decisions:
  - "RouteDetailViewModel exposes routeRepository and routeId as public vals — composable passes them to OsmPreviewMap directly instead of adding a new ViewModel method"
  - "getDecryptedGpx() auto-triggers downloadAndStoreGpx() if .enc absent — single entry point simplifies call sites"
  - "OsmPreviewMap falls back silently (logs warning) on GPX failure — region marker always visible; no crash path"

patterns-established:
  - "Security boundary: ciphertext on disk, plaintext only ever in RAM at render time"
  - "Streaming Retrofit: @Streaming + ResponseBody for large binary downloads to avoid OOM"
  - "In-memory GPX rendering: ByteArrayInputStream wraps decrypted bytes — no temp file needed"

requirements-completed: [PROT-01, PROT-02, PROT-03]

# Metrics
duration: 2min
completed: 2026-03-15
---

# Phase 4 Plan 02: Android Encryption Layer — Download, Store, Render Summary

**Streaming encrypted GPX download pipeline with in-memory AES-256-GCM decryption and OSMDroid polyline rendering — ciphertext on disk, plaintext only in RAM**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-15T07:38:26Z
- **Completed:** 2026-03-15T07:40:25Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- `ApiService` gains `@Streaming @GET api/v1/routes/{id}/gpx` endpoint — raw encrypted blob streamed directly from server
- `RouteRepository.downloadAndStoreGpx()` pipes response bytes to `filesDir/gpx/{routeId}.enc` with no plaintext intermediary at any point
- `RouteRepository.getDecryptedGpx()` auto-downloads if .enc absent, decrypts in memory via `GpxCryptoManager.decryptToByteArray()`, returns `Result<ByteArray>`
- `OsmPreviewMap` upgraded: `LaunchedEffect(routeId)` calls `getDecryptedGpx` on `Dispatchers.IO`, parses GPX from `ByteArrayInputStream`, renders `Polyline` overlay; falls back to region marker on failure
- Zero `ACTION_SEND`, `FileProvider`, share intent, or export mechanism in the entire Android module — PROT-03 confirmed

## Task Commits

Each task was committed atomically:

1. **Task 1: Add getRouteGpx endpoint and downloadAndStoreGpx/getDecryptedGpx** - `9c7c1de` (feat)
2. **Task 2: Wire GPX polyline rendering in OsmPreviewMap** - `51c27fe` (feat)

## Files Created/Modified
- `android/app/src/main/java/com/roadrunner/app/data/remote/ApiService.kt` - Added `@Streaming getRouteGpx()` endpoint with `ResponseBody`
- `android/app/src/main/java/com/roadrunner/app/data/repository/RouteRepository.kt` - Injected `GpxCryptoManager` + `@ApplicationContext context`; added `downloadAndStoreGpx()` and `getDecryptedGpx()`
- `android/app/src/main/java/com/roadrunner/app/ui/routedetail/OsmPreviewMap.kt` - New signature with `routeId`/`routeRepository`; `LaunchedEffect` + `Polyline` overlay rendering; `update` lambda in `AndroidView`
- `android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailViewModel.kt` - `routeRepository` and `routeId` changed from `private` to public `val`
- `android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailScreen.kt` - Updated `OsmPreviewMap` call site to pass `viewModel.routeId` and `viewModel.routeRepository`

## Decisions Made
- `RouteDetailViewModel` exposes `routeRepository` and `routeId` as public vals so the composable can pass them directly to `OsmPreviewMap` without adding a ViewModel method for every GPX operation.
- `getDecryptedGpx()` auto-triggers download when `.enc` file is absent — single entry point simplifies all call sites.
- `OsmPreviewMap` falls back silently on GPX failure (logs warning, shows region marker) — the screen never crashes on a missing or corrupt GPX file.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Security boundary (PROT-01, PROT-02, PROT-03) fully implemented: encrypted ciphertext on disk, plaintext only in memory at render time
- Phase 4 encryption layer is complete
- Phase 5 navigation can build on `getDecryptedGpx()` for turn-by-turn routing from the same decrypted in-memory bytes

---
*Phase: 04-android-encryption-layer*
*Completed: 2026-03-15*

## Self-Check: PASSED

All modified files exist on disk. Both task commits (9c7c1de, 51c27fe) confirmed in git log.
