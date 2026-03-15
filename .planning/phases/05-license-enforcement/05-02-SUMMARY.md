---
phase: 05-license-enforcement
plan: "02"
subsystem: android-navigation
tags: [kotlin, compose, hilt, navigation, license-enforcement, session-management]

# Dependency graph
requires:
  - phase: 05-license-enforcement
    plan: "01"
    provides: LicenseRepository.checkLicense(routeId) -> Result<NavigationSession>
  - phase: 04-android-encryption-layer
    provides: RouteDetailViewModel, RouteRepository, NavGraph
provides:
  - NavigationSessionManager singleton (storeSession, clearSession, isSessionExpired, hasActiveSession)
  - NavigationScreen placeholder composable with 30-second expiry polling
  - ExpiryDialog AlertDialog composable
  - Screen.Navigation destination with createRoute(routeId)
  - RouteDetailViewModel.startNavigation() calling LicenseRepository.checkLicense
  - RouteDetailScreen "Start Navigation" button gated on OWNED/ACTIVE/EXPIRING_SOON
  - NavGraph Navigation destination wired via EntryPointAccessors
affects:
  - 05-03 (license display imports RouteDetailViewModel with LicenseRepository)
  - Phase 6 (NavigationScreen placeholder to be replaced)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - EntryPointAccessors.fromApplication pattern for injecting @Singleton into NavGraph composables
    - LaunchedEffect polling loop with break-on-condition for session expiry check

key-files:
  created:
    - android/app/src/main/java/com/roadrunner/app/data/local/NavigationSessionManager.kt
    - android/app/src/main/java/com/roadrunner/app/ui/navigation/NavigationScreen.kt
    - android/app/src/main/java/com/roadrunner/app/ui/navigation/ExpiryDialog.kt
  modified:
    - android/app/src/main/java/com/roadrunner/app/navigation/Screen.kt
    - android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailViewModel.kt
    - android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailScreen.kt
    - android/app/src/main/java/com/roadrunner/app/navigation/NavGraph.kt

key-decisions:
  - "NavigationScreen receives sessionManager directly (not via ViewModel) — stateless coordinator pattern, session lives in singleton"
  - "EntryPointAccessors.fromApplication used to access NavigationSessionManager singleton in NavGraph composable without wrapper ViewModel"
  - "onSessionExpired navigates to Catalog with popUpTo(0) inclusive — clears entire back stack so user cannot navigate back to expired session"
  - "isStartingNavigation + CircularProgressIndicator on button prevents double-taps during license check network call"

requirements-completed: [LIC-01, LIC-02, LIC-03]

# Metrics
duration: 7min
completed: 2026-03-15
---

# Phase 5 Plan 02: Navigation Gating Summary

**Start Navigation button wired to server license check with NavigationSessionManager singleton, ExpiryDialog, and fully gated NavGraph NavigationScreen destination**

## Performance

- **Duration:** 7 min
- **Started:** 2026-03-15T08:05:00Z
- **Completed:** 2026-03-15T08:12:00Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments

- Created NavigationSessionManager @Singleton with in-memory session token and server-issued expiry timestamp; isSessionExpired() polls System.currentTimeMillis() against server-issued sessionExpiresAt
- Created ExpiryDialog AlertDialog composable with single OK button calling onDismiss
- Created NavigationScreen placeholder composable with Back button, Scaffold, and 30-second LaunchedEffect polling loop that shows ExpiryDialog on expiry then clears session
- Added Screen.Navigation sealed object with createRoute(routeId) helper
- Updated RouteDetailViewModel to inject LicenseRepository + NavigationSessionManager; added startNavigation(onNavigate) calling checkLicense; added clearNavigationError()
- Updated RouteDetailScreen with onStartNavigation param, conditional enabled button (OWNED/ACTIVE/EXPIRING_SOON only), CircularProgressIndicator during check, Snackbar for server error reasons
- Updated NavGraph with NavGraphEntryPoint @EntryPoint interface for session manager DI, Navigation composable destination, and RouteDetail wired with onStartNavigation

## Task Commits

Each task was committed atomically:

1. **Task 1: NavigationSessionManager, NavigationScreen, ExpiryDialog, Screen.Navigation** - `7d1b457` (feat)
2. **Task 2: Wire RouteDetailViewModel + RouteDetailScreen, update NavGraph** - `ed9bf5b` (feat)

## Files Created/Modified

- `android/.../data/local/NavigationSessionManager.kt` - @Singleton with storeSession, clearSession, isSessionExpired, hasActiveSession
- `android/.../ui/navigation/ExpiryDialog.kt` - AlertDialog with "Session Expired" title, OK dismiss button
- `android/.../ui/navigation/NavigationScreen.kt` - Placeholder with Back button and 30-second polling LaunchedEffect
- `android/.../navigation/Screen.kt` - Added Navigation object with createRoute(routeId)
- `android/.../ui/routedetail/RouteDetailViewModel.kt` - LicenseRepository + NavigationSessionManager injected, startNavigation(), clearNavigationError(), isStartingNavigation/navigationError in UiState
- `android/.../ui/routedetail/RouteDetailScreen.kt` - onStartNavigation param, conditional enabled button, Snackbar for navigationError
- `android/.../navigation/NavGraph.kt` - NavGraphEntryPoint @EntryPoint, Navigation destination wired, RouteDetail onStartNavigation callback

## Decisions Made

- NavigationScreen receives sessionManager directly (not via ViewModel) — it is a stateless coordinator; the session singleton lives outside the screen's lifecycle
- EntryPointAccessors.fromApplication provides the @Singleton NavigationSessionManager to NavGraph composables without creating a wrapper ViewModel
- onSessionExpired navigates to Catalog with `popUpTo(0) { inclusive = true }` to clear the entire back stack — user cannot go back to an expired navigation session
- Button shows CircularProgressIndicator and `enabled = false` during isStartingNavigation to prevent double-tap race conditions on the license check network call

## Deviations from Plan

None — plan executed exactly as written.

## Self-Check

- [x] NavigationSessionManager.kt exists at data/local/
- [x] NavigationScreen.kt exists at ui/navigation/
- [x] ExpiryDialog.kt exists at ui/navigation/
- [x] Screen.Navigation declared in Screen.kt
- [x] startNavigation() in RouteDetailViewModel.kt
- [x] licenseRepository.checkLicense called in RouteDetailViewModel.kt
- [x] sessionManager.storeSession in RouteDetailViewModel.kt
- [x] isSessionExpired in NavigationScreen.kt LaunchedEffect
- [x] Screen.Navigation destination in NavGraph.kt
- [x] enabled = canNavigate in RouteDetailScreen.kt
- [x] Kotlin compiles clean (BUILD SUCCESSFUL)
- [x] Commit 7d1b457 exists (Task 1)
- [x] Commit ed9bf5b exists (Task 2)

## Self-Check: PASSED

---
*Phase: 05-license-enforcement*
*Completed: 2026-03-15*
