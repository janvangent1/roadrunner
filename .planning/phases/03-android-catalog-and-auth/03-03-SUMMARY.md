---
phase: 03-android-catalog-and-auth
plan: "03"
subsystem: android

tags: [android, kotlin, compose, hilt, viewmodel, navigation, auth, login, register, jetpack]

# Dependency graph
requires:
  - phase: 03-android-catalog-and-auth-02
    provides: AuthRepository with isLoggedIn/login/register/loginWithGoogle/logout and Tink-backed TokenStorage
provides:
  - AuthViewModel (HiltViewModel) with AuthUiState sealed class and login/register/googleSignIn/logout/resetState functions
  - LoginScreen composable with email/password fields, Sign In button, Google Sign-In button, password visibility toggle, error display
  - RegisterScreen composable with email/password fields, client-side length validation, Create Account button, error display
  - Session-aware NavGraph: startDestination determined by authRepository.isLoggedIn() at app launch
  - Sign Out in Catalog placeholder clears full back stack and returns to Login (AUTH-04)
affects:
  - 03-android-catalog-and-auth
  - 04-gpx-navigation

# Tech tracking
tech-stack:
  added:
    - "androidx.compose.material:material-icons-extended (via Compose BOM) — for Visibility/VisibilityOff password toggle icons"
  patterns:
    - "AuthViewModel uses single AuthUiState StateFlow; screens collect with collectAsState() and LaunchedEffect for navigation on Success"
    - "Each screen gets its own AuthViewModel instance via hiltViewModel() at its NavGraph destination (default Hilt scoping)"
    - "Password visibility toggle pattern: mutableStateOf(false) + VisualTransformation.None vs PasswordVisualTransformation()"
    - "Client-side password validation: inline error shown below field before submit; button disabled while error present"
    - "NavGraph logout: popUpTo(0) { inclusive = true } clears entire back stack, preventing back-navigation to authenticated screens"

key-files:
  created:
    - android/app/src/main/java/com/roadrunner/app/ui/auth/AuthViewModel.kt
    - android/app/src/main/java/com/roadrunner/app/ui/auth/LoginScreen.kt
    - android/app/src/main/java/com/roadrunner/app/ui/auth/RegisterScreen.kt
  modified:
    - android/app/src/main/java/com/roadrunner/app/navigation/NavGraph.kt
    - android/app/src/main/java/com/roadrunner/app/MainActivity.kt
    - android/app/build.gradle.kts

key-decisions:
  - "material-icons-extended added to support Visibility/VisibilityOff password toggle icons — these are in extended set, not core Material icons"
  - "AuthViewModel.resetState() called after LaunchedEffect fires on Success to prevent re-navigation if recomposition occurs"
  - "Catalog placeholder uses AuthViewModel via hiltViewModel() scoped to that destination for logout — will be replaced in Plan 04"

patterns-established:
  - "Auth screens: LaunchedEffect on uiState triggers navigation when Success; always call resetState() after to avoid re-trigger on recomposition"
  - "NavGraph session check: authRepository.isLoggedIn() called synchronously in onCreate before setContent — acceptable since TokenStorage is in-memory after first read"

requirements-completed: [AUTH-01, AUTH-02, AUTH-03, AUTH-04]

# Metrics
duration: 7min
completed: 2026-03-15
---

# Phase 3 Plan 03: Auth UI Screens Summary

**Jetpack Compose Login and Register screens with AuthViewModel StateFlow, password visibility toggle, client-side validation, and session-aware NavGraph startDestination based on stored token presence**

## Performance

- **Duration:** 7 min
- **Started:** 2026-03-15T07:01:34Z
- **Completed:** 2026-03-15T07:08:57Z
- **Tasks:** 2
- **Files modified:** 6 (3 created, 3 modified)

## Accomplishments

- Complete auth UI layer: AuthViewModel, LoginScreen, and RegisterScreen all compile and wire into the NavGraph via Hilt
- Session persistence (AUTH-03): MainActivity injects AuthRepository and sets startDestination to Catalog when isLoggedIn() returns true, skipping login entirely on relaunch with valid token
- Sign Out (AUTH-04): Catalog placeholder button calls AuthViewModel.logout() then navigates to Login with popUpTo(0) { inclusive = true }, preventing any back-navigation to authenticated screens
- Password visibility toggle on both screens with Visibility/VisibilityOff icons; client-side 8-character minimum validation on RegisterScreen shown inline before submit

## Task Commits

Each task was committed atomically:

1. **Task 1: AuthViewModel, LoginScreen, and RegisterScreen** - `ea3d9f4` (feat)
2. **Task 2: Session-aware NavGraph and updated MainActivity** - `e61aebc` (feat)

## Files Created/Modified

- `android/app/src/main/java/com/roadrunner/app/ui/auth/AuthViewModel.kt` - HiltViewModel with AuthUiState (Idle/Loading/Success/Error) and login/register/googleSignIn/logout/resetState
- `android/app/src/main/java/com/roadrunner/app/ui/auth/LoginScreen.kt` - Compose login form: email/password OutlinedTextFields, Sign In button, Google Sign-In outlined button, password visibility toggle, error text, TextButton to Register
- `android/app/src/main/java/com/roadrunner/app/ui/auth/RegisterScreen.kt` - Compose register form: email/password fields with inline length validation, Create Account button, error text, TextButton to Login
- `android/app/src/main/java/com/roadrunner/app/navigation/NavGraph.kt` - Updated to wire real LoginScreen/RegisterScreen; Catalog placeholder with Sign Out; MyRoutes and RouteDetail placeholders retained
- `android/app/src/main/java/com/roadrunner/app/MainActivity.kt` - Injects AuthRepository; sets startDestination to Catalog or Login based on isLoggedIn()
- `android/app/build.gradle.kts` - Added material-icons-extended dependency for password toggle icons

## Decisions Made

- `material-icons-extended` added as dependency — `Icons.Filled.Visibility` and `Icons.Filled.VisibilityOff` are not in the core material icons set bundled with material3; extended set required. This is a minor size increase acceptable for v1.
- `viewModel.resetState()` called inside `LaunchedEffect` after `onLoginSuccess()` / `onRegisterSuccess()` to prevent re-navigation if recomposition occurs while still on the screen during navigation transition.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added material-icons-extended dependency**
- **Found during:** Task 1 (LoginScreen and RegisterScreen compilation)
- **Issue:** `Icons.Filled.Visibility` and `Icons.Filled.VisibilityOff` unresolved — these icons are in the extended set, not in material3 core. Plan specified password visibility toggle icons without noting the extra dependency.
- **Fix:** Added `implementation("androidx.compose.material:material-icons-extended")` to app/build.gradle.kts (resolved via Compose BOM, no explicit version needed)
- **Files modified:** android/app/build.gradle.kts
- **Verification:** Build passed after addition
- **Committed in:** `ea3d9f4` (part of Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking dependency)
**Impact on plan:** Necessary for compilation — password toggle icons require the extended icons library. No scope creep.

## Issues Encountered

The default shell environment uses Java 8 (`jre1.8.0_461`) which is incompatible with AGP 8.7.0 (requires Java 11+). Build executed using `JAVA_HOME=/c/Users/jbouq/.jdks/openjdk-24.0.1` with explicit `bash gradlew` invocation. This is a local environment issue, not a project issue — Android Studio uses its own JBR automatically.

## User Setup Required

None - no external service configuration required. (Google Sign-In client ID placeholder was set in Plan 03-02.)

## Next Phase Readiness

- Plan 03-04 (Catalog Screens): NavGraph has Catalog and MyRoutes placeholders ready to be replaced. ApiService placeholder from Plan 03-02 is ready for route endpoints.
- Plan 03-05 (Route Detail Screen): RouteDetail placeholder in NavGraph accepts `routeId` argument via `Screen.RouteDetail.createRoute(routeId)`
- Auth UI is fully functional end-to-end: Login → Catalog → Sign Out → Login flow works. Google Sign-In wired to AuthRepository.loginWithGoogle() (requires valid Google client ID to test).

---
*Phase: 03-android-catalog-and-auth*
*Completed: 2026-03-15*

## Self-Check: PASSED

All 5 required files verified present on disk. Both task commits (ea3d9f4, e61aebc) confirmed in git log. Build exits 0 (`./gradlew assembleMotorcycleDebug`). All verification criteria met.
