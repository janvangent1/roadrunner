---
phase: 05-license-enforcement
plan: "01"
subsystem: api
tags: [kotlin, typescript, prisma, retrofit, hilt, license, repository-pattern]

# Dependency graph
requires:
  - phase: 04-android-encryption-layer
    provides: RouteRepository, GpxCryptoManager, ApiService with checkLicense
  - phase: 01-backend-foundation
    provides: POST /api/v1/licenses/check, prisma.license model, requireAuth middleware
provides:
  - GET /api/v1/licenses/my backend endpoint (authenticated, all non-revoked licenses for user)
  - LicenseDtos.kt (LicenseDto, NavigationSession)
  - ApiService.getMyLicenses() Retrofit declaration
  - LicenseRepository @Singleton with getMyLicenses(), computeLicenseStatus(), checkLicense()
  - In-memory license cache (_cachedLicenses)
affects:
  - 05-02 (navigation gating imports LicenseRepository.checkLicense)
  - 05-03 (license display imports LicenseRepository.computeLicenseStatus and getMyLicenses)

# Tech tracking
tech-stack:
  added: [com.github.ticofab:android-gpx-parser:2.3.0 (via JitPack, fixed group ID)]
  patterns: [in-memory license cache with stale-on-failure strategy, server-clock-only expiry for security gate]

key-files:
  created:
    - backend/src/routes/licenses.ts (modified — GET /my added)
    - android/app/src/main/java/com/roadrunner/app/data/remote/dto/LicenseDtos.kt
    - android/app/src/main/java/com/roadrunner/app/data/repository/LicenseRepository.kt
  modified:
    - android/app/src/main/java/com/roadrunner/app/data/remote/ApiService.kt
    - android/app/src/main/java/com/roadrunner/app/data/remote/dto/RouteDtos.kt
    - android/settings.gradle.kts
    - android/app/build.gradle.kts
    - android/app/src/main/java/com/roadrunner/app/ui/routedetail/OsmPreviewMap.kt

key-decisions:
  - "GET /my returns expired licenses too — client needs them for display; no expiry filter server-side"
  - "In-memory cache keeps stale data on fetch failure — never clears on error to preserve offline display"
  - "System.currentTimeMillis() used only for EXPIRING_SOON display hint, not for the active/expired security gate"
  - "android-gpx-parser lives on JitPack as com.github.ticofab (not io.ticofab) — fixed group ID and added JitPack repo"
  - "sessionExpiresAt added to LicenseCheckResponse with default null for backward compatibility"

patterns-established:
  - "Repository pattern: @Singleton + @Inject constructor for Hilt DI, same as RouteRepository"
  - "checkLicense error mapping: parse JSON code field inline without extra JSON library, map to user-readable strings"
  - "computeLicenseStatus returns Triple<LicenseStatus, String?, LicenseType?> — same shape as RouteRepository.checkLicenseStatus"

requirements-completed: [LIC-01, LIC-02, LIC-03, LIC-04]

# Metrics
duration: 7min
completed: 2026-03-15
---

# Phase 5 Plan 01: License Enforcement Foundation Summary

**GET /api/v1/licenses/my backend endpoint + Android LicenseRepository with in-memory cache and NavigationSession-returning checkLicense**

## Performance

- **Duration:** 7 min
- **Started:** 2026-03-15T07:55:15Z
- **Completed:** 2026-03-15T08:02:00Z
- **Tasks:** 3
- **Files modified:** 8

## Accomplishments

- Added GET /api/v1/licenses/my Fastify route (authenticated, prisma.license.findMany with revokedAt=null, returns all including expired for display purposes)
- Created LicenseDtos.kt with LicenseDto and NavigationSession; updated ApiService.kt with getMyLicenses() Retrofit declaration
- Created LicenseRepository @Singleton with getMyLicenses() (in-memory cache), computeLicenseStatus() (server-issued expiry), and checkLicense() (NavigationSession or user-readable failure)

## Task Commits

Each task was committed atomically:

1. **Task 1: Add GET /api/v1/licenses/my backend endpoint** - `21df827` (feat)
2. **Task 2: LicenseDtos.kt and updated ApiService with getMyLicenses** - `4defd6b` (feat)
3. **Task 3: LicenseRepository with in-memory cache and checkLicense** - `868c953` (feat)

## Files Created/Modified

- `backend/src/routes/licenses.ts` - Added GET /my route alongside existing POST /check; requireAuth; prisma.license.findMany
- `android/.../dto/LicenseDtos.kt` - LicenseDto (mirrors backend GET /my response), NavigationSession
- `android/.../remote/ApiService.kt` - Added getMyLicenses() returning Response<List<LicenseDto>>
- `android/.../dto/RouteDtos.kt` - Added sessionExpiresAt: String? = null to LicenseCheckResponse
- `android/.../repository/LicenseRepository.kt` - Full @Singleton with cache, status computation, license check
- `android/settings.gradle.kts` - Added JitPack repository (required for android-gpx-parser)
- `android/app/build.gradle.kts` - Fixed android-gpx-parser group ID: io.ticofab -> com.github.ticofab
- `android/.../ui/routedetail/OsmPreviewMap.kt` - Fixed GPX parser API: trackSegments/trackPoints + nullable lat/lon

## Decisions Made

- GET /my returns all non-revoked licenses (including expired) — client needs expired licenses for display; server does not filter by expiry on this endpoint
- In-memory cache never clears on failure — stale data preserved for offline display
- System.currentTimeMillis() is acceptable for EXPIRING_SOON threshold (display hint only); the expired/active boundary uses server-issued expiresAt parsed via Instant.parse
- sessionExpiresAt added to LicenseCheckResponse with default null so existing call sites are unaffected

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed android-gpx-parser wrong group ID in build.gradle.kts**
- **Found during:** Task 2 (Kotlin compilation attempt)
- **Issue:** Dependency declared as `io.ticofab.android-gpx-parser:android-gpx-parser:2.3.0` — this group ID does not exist in Google Maven or Maven Central; library is published on JitPack as `com.github.ticofab:android-gpx-parser:2.3.0`
- **Fix:** Changed group ID in build.gradle.kts to `com.github.ticofab:android-gpx-parser:2.3.0`
- **Files modified:** `android/app/build.gradle.kts`
- **Verification:** Build successful after fix
- **Committed in:** `4defd6b` (Task 2 commit)

**2. [Rule 3 - Blocking] Added JitPack repository to settings.gradle.kts**
- **Found during:** Task 2 (Kotlin compilation attempt)
- **Issue:** JitPack not listed in dependencyResolutionManagement.repositories, so android-gpx-parser could not be resolved
- **Fix:** Added `maven { url = uri("https://jitpack.io") }` to settings.gradle.kts
- **Files modified:** `android/settings.gradle.kts`
- **Verification:** Dependency resolved successfully after fix
- **Committed in:** `4defd6b` (Task 2 commit)

**3. [Rule 1 - Bug] Fixed OsmPreviewMap.kt GPX parser API mismatch**
- **Found during:** Task 2 (Kotlin compilation)
- **Issue:** OsmPreviewMap.kt used `it.segments`/`it.points`/`it.latitude`/`it.longitude` — the android-gpx-parser 2.3.0 API uses `trackSegments`, `trackPoints`, and nullable `latitude`/`longitude`; compiler reported "Unresolved reference 'it'"
- **Fix:** Updated to use explicit lambda parameters `track.trackSegments`, `segment.trackPoints`, and `mapNotNull` with nullable checks
- **Files modified:** `android/.../ui/routedetail/OsmPreviewMap.kt`
- **Verification:** Kotlin compilation passes clean after fix
- **Committed in:** `4defd6b` (Task 2 commit)

---

**Total deviations:** 3 auto-fixed (2 bugs, 1 blocking)
**Impact on plan:** All three fixes were necessary for the Android build to succeed. Issues originated in Phase 4 (OsmPreviewMap and build.gradle.kts). No scope creep.

## Issues Encountered

- Gradle build required Java 11+ but the system default was Java 8. Resolved by using Android Studio's bundled JBR at `/d/AndroidStudio/jbr` (OpenJDK 21.0.6).
- Build variant is `motorcycleDebug` (not plain `debug`) — the correct Gradle task is `:app:compileMotorcycleDebugKotlin`.

## Next Phase Readiness

- Plan 05-02 (navigation gating) can now import `LicenseRepository.checkLicense()` which returns `Result<NavigationSession>`
- Plan 05-03 (license display) can import `LicenseRepository.getMyLicenses()` and `computeLicenseStatus()` for badge rendering
- Both plans have clear Kotlin types to implement against: `LicenseDto`, `NavigationSession`, `LicenseStatus`, `LicenseType`

---
*Phase: 05-license-enforcement*
*Completed: 2026-03-15*
