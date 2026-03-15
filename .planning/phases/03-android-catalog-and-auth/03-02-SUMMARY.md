---
phase: 03-android-catalog-and-auth
plan: "02"
subsystem: android

tags: [android, kotlin, retrofit, okhttp, tink, hilt, jwt, auth, interceptor, credentialmanager]

# Dependency graph
requires:
  - phase: 03-android-catalog-and-auth-01
    provides: Buildable Android skeleton with Hilt, NavGraph, and all Phase 3 dependencies
provides:
  - Tink AES-256-GCM encrypted JWT storage via AndroidKeysetManager (TokenStorage)
  - OkHttp AuthInterceptor attaching Bearer token to authenticated requests
  - OkHttp TokenRefreshAuthenticator handling 401s with runBlocking refresh and retry
  - AuthRepository with isLoggedIn, login, register, loginWithGoogle, logout
  - Hilt NetworkModule providing two separate OkHttpClient configs (auth and non-auth)
  - AuthApiService and ApiService (placeholder) Retrofit interfaces
affects:
  - 03-android-catalog-and-auth
  - 04-gpx-navigation

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Two Retrofit instances: AuthApiService uses plain OkHttpClient; ApiService uses auth-intercepted client"
    - "TokenRefreshAuthenticator uses runBlocking (acceptable on OkHttp background thread) to call refresh endpoint"
    - "AuthInterceptor skips header injection only when token is null — all requests get Bearer token if present"
    - "TokenStorage lazy-initializes Tink Aead to avoid overhead at injection time"
    - "AuthRepository.logout() clears tokens locally even if network call fails — always succeeds"

key-files:
  created:
    - android/app/src/main/java/com/roadrunner/app/data/remote/dto/AuthDtos.kt
    - android/app/src/main/java/com/roadrunner/app/data/remote/AuthApiService.kt
    - android/app/src/main/java/com/roadrunner/app/data/remote/ApiService.kt
    - android/app/src/main/java/com/roadrunner/app/data/local/TokenStorage.kt
    - android/app/src/main/java/com/roadrunner/app/data/remote/interceptor/AuthInterceptor.kt
    - android/app/src/main/java/com/roadrunner/app/data/remote/interceptor/TokenRefreshAuthenticator.kt
    - android/app/src/main/java/com/roadrunner/app/data/repository/AuthRepository.kt
    - android/app/src/main/java/com/roadrunner/app/di/NetworkModule.kt
  modified:
    - android/app/src/main/java/com/roadrunner/app/di/AppModule.kt
    - android/app/src/main/res/values/strings.xml

key-decisions:
  - "AuthApiService uses a plain OkHttpClient (no auth interceptor) to avoid circular dependency during token refresh in TokenRefreshAuthenticator"
  - "google_server_client_id string resource added as placeholder in strings.xml — developer fills in from Google Cloud Console before release"

patterns-established:
  - "AuthApiService (unauthenticated) and ApiService (authenticated) are two separate Retrofit instances — never mix"
  - "TokenStorage uses lazy Aead initialization; all encrypt/decrypt calls go through instance methods"
  - "AuthRepository.loginWithGoogle() requires activityContext (not application context) for CredentialManager"

requirements-completed: [AUTH-01, AUTH-02, AUTH-03, AUTH-04]

# Metrics
duration: 2min
completed: 2026-03-15
---

# Phase 3 Plan 02: Network Layer and Auth Data Layer Summary

**Retrofit + OkHttp network layer with Tink AES-256-GCM encrypted JWT storage, Bearer auth interceptor, automatic token refresh on 401, and AuthRepository covering all four auth requirements**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-15T06:56:37Z
- **Completed:** 2026-03-15T06:58:40Z
- **Tasks:** 2
- **Files modified:** 10 (8 created, 2 modified)

## Accomplishments

- Complete auth data layer: DTOs, Retrofit interfaces, Tink-backed encrypted storage, OkHttp interceptors, and AuthRepository all compile and wire via Hilt
- TokenStorage uses Google Tink 1.15.0 (AES-256-GCM via AndroidKeysetManager + Android Keystore) — not the deprecated `androidx.security:security-crypto`
- Two-client Retrofit pattern separates unauthenticated AuthApiService from authenticated ApiService, eliminating circular dependency in TokenRefreshAuthenticator
- AuthRepository covers all four AUTH requirements: register, email login, Google sign-in via CredentialManager, and logout with local token clearing

## Task Commits

Each task was committed atomically:

1. **Task 1: DTOs, Retrofit services, and Tink-backed token storage** - `fc7ba21` (feat)
2. **Task 2: OkHttp interceptors, Hilt NetworkModule, and AuthRepository** - `65585a6` (feat)

## Files Created/Modified

- `android/app/src/main/java/com/roadrunner/app/data/remote/dto/AuthDtos.kt` - Request/response data classes for all 5 auth endpoints
- `android/app/src/main/java/com/roadrunner/app/data/remote/AuthApiService.kt` - Retrofit interface for unauthenticated auth endpoints
- `android/app/src/main/java/com/roadrunner/app/data/remote/ApiService.kt` - Retrofit interface placeholder for authenticated route endpoints (Plan 04)
- `android/app/src/main/java/com/roadrunner/app/data/local/TokenStorage.kt` - Tink AES-256-GCM encrypted JWT storage with saveTokens/getAccessToken/getRefreshToken/clearTokens
- `android/app/src/main/java/com/roadrunner/app/data/remote/interceptor/AuthInterceptor.kt` - OkHttp Interceptor adding Authorization Bearer header
- `android/app/src/main/java/com/roadrunner/app/data/remote/interceptor/TokenRefreshAuthenticator.kt` - OkHttp Authenticator handling 401s with runBlocking refresh and token retry
- `android/app/src/main/java/com/roadrunner/app/data/repository/AuthRepository.kt` - Auth business logic: isLoggedIn, login, register, loginWithGoogle, logout
- `android/app/src/main/java/com/roadrunner/app/di/NetworkModule.kt` - Hilt module providing AuthApiService, OkHttpClient, ApiService
- `android/app/src/main/java/com/roadrunner/app/di/AppModule.kt` - Updated to provide ApplicationContext binding
- `android/app/src/main/res/values/strings.xml` - Added google_server_client_id placeholder string

## Decisions Made

- AuthApiService injected into TokenRefreshAuthenticator uses a dedicated plain OkHttpClient (no auth interceptor) to prevent infinite 401 loop during token refresh
- `google_server_client_id` resource string is a placeholder (`YOUR_GOOGLE_CLIENT_ID`) — the developer must fill this in from Google Cloud Console before Google Sign-In works in production

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

Google Sign-In requires the developer to replace the `YOUR_GOOGLE_CLIENT_ID` placeholder in `android/app/src/main/res/values/strings.xml` with the actual OAuth 2.0 Web client ID from Google Cloud Console before Google Sign-In will work.

## Next Phase Readiness

- Plan 03-03 (Auth Screens: Login + Register): AuthRepository is injectable via `hiltViewModel()` — screens can call `authRepository.login()`, `authRepository.register()`, `authRepository.loginWithGoogle()`
- Plan 03-04 (Catalog Screens): ApiService placeholder is ready for route endpoints to be added
- TokenStorage pattern established: all subsequent plans needing auth state check `tokenStorage.getAccessToken() != null` via AuthRepository.isLoggedIn()
- NavGraph session check: Plans 03-03+ can call `authRepository.isLoggedIn()` in NavGraph startDestination logic to route to Login or Catalog on app launch

---
*Phase: 03-android-catalog-and-auth*
*Completed: 2026-03-15*

## Self-Check: PASSED

All 9 required files verified present on disk. Both task commits (fc7ba21, 65585a6) confirmed in git log. Build exits 0 (`./gradlew assembleMotorcycleDebug`). All verification criteria met.
