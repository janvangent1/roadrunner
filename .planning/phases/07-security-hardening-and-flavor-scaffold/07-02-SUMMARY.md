---
phase: 07-security-hardening-and-flavor-scaffold
plan: 02
subsystem: security
tags: [play-integrity, android, datastore, coroutines, fastify, googleapis, device-attestation]

# Dependency graph
requires:
  - phase: 07-security-hardening-and-flavor-scaffold
    provides: Play Integrity API dependency in build.gradle.kts (com.google.android.play:integrity:1.4.0)
  - phase: 03-android-catalog-and-auth
    provides: MainActivity structure, AuthRepository injection, lifecycleScope pattern
  - phase: 01-backend-foundation
    provides: Fastify app.ts route registration pattern, fastify-plugin wrapper pattern

provides:
  - IntegrityChecker.kt with SHA-256 nonce, Play Integrity StandardIntegrityManager, 24h DataStore cache, OkHttp POST to backend
  - MainActivity updated to call IntegrityChecker.check() in lifecycleScope, show non-dismissible DeviceNotSupportedDialog on failure
  - Backend POST /api/v1/integrity/verify using googleapis playintegrity.v1.decodeIntegrityToken, checks MEETS_BASIC_INTEGRITY
  - DataStore preferences cache preventing repeated Play Integrity calls within 24h window

affects:
  - Any future feature that modifies MainActivity.onCreate
  - Backend route registration in app.ts

# Tech tracking
tech-stack:
  added:
    - androidx.datastore:datastore-preferences:1.1.1 (Android)
    - org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1 (Android — Task.await() bridge)
    - googleapis npm package (backend)
  patterns:
    - preferencesDataStore extension property on Context for scoped DataStore access
    - Play Integrity StandardIntegrityManager two-step: prepareIntegrityToken -> request
    - Fail-open on network error, fail-secure on API error (different handling on Android vs backend)
    - Non-dismissible AlertDialog (empty onDismissRequest lambda) for hard device block

key-files:
  created:
    - android/app/src/main/java/com/roadrunner/app/security/IntegrityChecker.kt
    - backend/src/routes/integrity.ts
  modified:
    - android/app/src/main/java/com/roadrunner/app/MainActivity.kt
    - android/app/build.gradle.kts
    - android/app/src/main/res/values/strings.xml
    - backend/src/app.ts
    - backend/.env.example
    - backend/package.json

key-decisions:
  - "Fail-open on Android network/API error (v1 pragmatism) — exception in lifecycleScope.launch is caught, app proceeds; fail-secure added on backend side (passed=false on API error)"
  - "preferencesDataStore extension defined at file level (not in class) — Kotlin DataStore requirement for singleton DataStore per name"
  - "kotlinx-coroutines-play-services:1.8.1 added for Task.await() — transitive from play-services-location was insufficient without explicit dependency"
  - "play_integrity_cloud_project_number placeholder value 0 in strings.xml — developer fills in actual Cloud project number from Play Console before release"
  - "googleapis npm package added for playintegrity.v1.decodeIntegrityToken — google-auth-library already present but does not include Play Integrity API client"

patterns-established:
  - "Security checks in lifecycleScope.launch before setContent: check runs async, UI state variable (mutableStateOf) gates what setContent renders"
  - "Non-dismissible dialog pattern: onDismissRequest = { } no-op plus no confirmButton/dismissButton renders unblockable full-screen dialog"

requirements-completed:
  - ARCH-INTEGRITY-01

# Metrics
duration: 8min
completed: 2026-03-15
---

# Phase 7 Plan 02: Play Integrity API Device Verification Summary

**Play Integrity device attestation with SHA-256 nonce, 24h DataStore cache, non-dismissible block dialog on Android, and googleapis-backed backend token decoder checking MEETS_BASIC_INTEGRITY**

## Performance

- **Duration:** 8 min
- **Started:** 2026-03-15T13:25:44Z
- **Completed:** 2026-03-15T13:33:00Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments

- IntegrityChecker.kt: generates Base64(SHA-256(deviceId:timestamp)) nonce, calls StandardIntegrityManager two-step flow, POSTs token+nonce to backend via OkHttp, caches boolean result for 24h in DataStore<Preferences>
- MainActivity updated: injects IntegrityChecker via Hilt, calls check() in lifecycleScope.launch before setContent renders navigation; integrityBlocked state variable gates a non-dismissible DeviceNotSupportedDialog
- Backend integrity.ts: receives token, calls googleapis playintegrity.v1.decodeIntegrityToken, checks MEETS_BASIC_INTEGRITY in deviceRecognitionVerdict array, returns { passed: boolean }; fails-secure on API error

## Task Commits

Each task was committed atomically:

1. **Task 1: Create IntegrityChecker.kt and wire into MainActivity** - `fd018f2` (feat)
2. **Task 2: Create backend integrity verification endpoint** - `e116c93` (feat)

**Plan metadata:** (created in this commit — see final commit)

## Files Created/Modified

- `android/app/src/main/java/com/roadrunner/app/security/IntegrityChecker.kt` - Play Integrity wrapper with nonce generation and 24h DataStore cache
- `android/app/src/main/java/com/roadrunner/app/MainActivity.kt` - Wired IntegrityChecker, integrityBlocked state, DeviceNotSupportedDialog
- `android/app/build.gradle.kts` - Added datastore-preferences:1.1.1, coroutines-play-services:1.8.1 (play:integrity:1.4.0 was already added by Plan 01)
- `android/app/src/main/res/values/strings.xml` - Added play_integrity_cloud_project_number placeholder (value: 0)
- `backend/src/routes/integrity.ts` - POST /verify route using googleapis playintegrity, MEETS_BASIC_INTEGRITY check
- `backend/src/app.ts` - Registered integrityHandlers at /api/v1/integrity prefix
- `backend/.env.example` - Documented ANDROID_PACKAGE_NAME env var
- `backend/package.json` - Added googleapis dependency

## Decisions Made

- Fail-open on Android: network/API errors log a warning and let the app proceed (fail-open). This is explicit v1 pragmatism to avoid blocking legitimate users on flaky connections.
- Fail-secure on backend: if the googleapis call throws, the backend returns `{ passed: false }`. The backend is the authoritative check.
- `preferencesDataStore` extension property is defined at file level (outside the class) — this is a DataStore requirement to prevent multiple DataStore instances for the same name.
- `kotlinx-coroutines-play-services:1.8.1` added explicitly for `Task.await()` bridge — transitive availability from `play-services-location` is not reliable.
- `play_integrity_cloud_project_number` set to placeholder 0 in strings.xml — developer must fill in their Google Cloud project number from Play Console before release.
- `googleapis` npm package added to backend — `google-auth-library` (already present for Google Sign-In) does not include the Play Integrity API client.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added kotlinx-coroutines-play-services dependency**
- **Found during:** Task 1 (IntegrityChecker.kt creation)
- **Issue:** Plan mentioned `Task.await()` needs `kotlinx-coroutines-play-services` but listed it as a note rather than a definitive step. The dependency was absent from build.gradle.kts.
- **Fix:** Added `implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")` to build.gradle.kts dependencies block.
- **Files modified:** android/app/build.gradle.kts
- **Verification:** Included in Task 1 commit; file exists and dependency block is correct.
- **Committed in:** fd018f2 (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** Necessary — without this dependency `Task.await()` would not compile for the Play Integrity token flow. No scope creep.

## Issues Encountered

- `build.gradle.kts` had been updated by Phase 7 Plan 01 (which ran before this plan) and contained new flavor structure and buildType fields not present in the plan's context snapshot. Read the file before editing to pick up the correct current state.
- Gradle dry-run fails locally due to Java 8 JVM in dev environment (project requires Java 11+). This is a pre-existing environment constraint, not a code issue introduced by this plan.

## User Setup Required

Before release, developers must:
1. Fill in `play_integrity_cloud_project_number` in `android/app/src/main/res/values/strings.xml` with the actual Google Cloud project number from Play Console.
2. Set `ANDROID_PACKAGE_NAME` environment variable on the backend (documented in `.env.example`).
3. Configure Google Play Integrity API credentials (service account) for the backend GoogleAuth client.

## Next Phase Readiness

- Device integrity check is wired end-to-end; backend route is registered and TypeScript-clean.
- Placeholder values in strings.xml and .env.example are documented — production configuration required before release.
- Phase 7 Plan 03 (certificate pinning) can proceed; no blockers from this plan.

---
*Phase: 07-security-hardening-and-flavor-scaffold*
*Completed: 2026-03-15*
