---
phase: 03-android-catalog-and-auth
verified: 2026-03-15T12:00:00Z
status: human_needed
score: 6/6 must-haves verified
gaps: []
human_verification:
  - test: "Sign in with Google"
    expected: "Google one-tap sheet appears, user selects account, app navigates to Catalog"
    why_human: "CredentialManager Google sign-in flow requires a real device/emulator with Google Play Services and a valid OAuth client ID. The placeholder string 'YOUR_GOOGLE_CLIENT_ID' in strings.xml confirms this cannot work without developer configuration. The code path is fully wired but the credential itself cannot be tested programmatically."
  - test: "Session persists across app restarts"
    expected: "Kill app via recent apps; reopen — lands directly on Catalog screen without showing Login"
    why_human: "Requires running on emulator/device. Cannot verify runtime behavior of Tink keyset + SharedPreferences round-trip without executing the app."
  - test: "Route catalog cards show thumbnail map"
    expected: "Each route card in CatalogScreen shows a visual thumbnail map image per CATA-02"
    why_human: "Success criterion 3 includes 'thumbnail map per card'. Code inspection of RouteCard.kt shows NO thumbnail map — cards show title, region, difficulty badge, distance, terrain type, and license chip, but no map image. This needs human confirmation of whether CATA-02's thumbnail map was intentionally deferred or is a gap."
  - test: "OSMDroid preview map renders tiles on route detail"
    expected: "RouteDetailScreen shows a 220dp-tall map with OpenStreetMap tiles and a region marker"
    why_human: "Requires network connectivity and a running emulator to verify OSMDroid tile loading. Cannot verify tile rendering programmatically."
  - test: "Sign out back-stack behavior"
    expected: "After sign out, pressing Back from Login screen does not navigate to Catalog"
    why_human: "Back-stack behavior requires runtime verification on emulator."
---

# Phase 3: Android Catalog and Auth Verification Report

**Phase Goal:** A rider can open the Android app, create or sign in to an account, browse the route catalog, and view route detail pages with a preview map — the complete pre-purchase experience works end-to-end.
**Verified:** 2026-03-15T12:00:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | User can create an account with email/password, or sign in with Google one-tap | VERIFIED (code) / HUMAN for Google | `LoginScreen.kt` has email+password fields calling `viewModel.login()` and Google button calling `viewModel.googleSignIn()`. `RegisterScreen.kt` calls `viewModel.register()`. `AuthRepository` calls correct backend endpoints. Google path fully wired to CredentialManager but requires valid client ID. |
| 2 | User session persists across app restarts; user can sign out | VERIFIED (code) | `TokenStorage.kt` saves access+refresh tokens with Tink AES-256-GCM to SharedPreferences. `MainActivity` injects `AuthRepository` and conditionally routes to `Screen.Catalog.route` if `isLoggedIn()`. Sign out calls `authRepository.logout()` then `navController.navigate(Login) { popUpTo(0) { inclusive=true } }`. |
| 3 | User can browse all published routes showing title, distance, difficulty, terrain type, region, thumbnail map per card | PARTIAL — no thumbnail map | `RouteCard.kt` renders title, DifficultyBadge, region, distance km, terrain type, LicenseStatusBadge. No thumbnail map image exists in the card. Success criterion mentions "thumbnail map per card" but the Phase 3 CONTEXT.md defers GPX preview to Phase 4 and only shows the map on the detail screen. Needs human confirmation whether this was intentional scope. |
| 4 | Each route card shows the correct license status badge | VERIFIED | `LicenseStatusBadge` in `RouteCard.kt` maps all 5 `LicenseStatus` values to color-coded chips: OWNED=green, ACTIVE=blue, EXPIRING_SOON=orange, EXPIRED=red, AVAILABLE=grey. `RouteRepository.checkLicenseStatus()` resolves status against `/api/v1/licenses/check`. |
| 5 | User can open a route detail page showing full metadata, OSMDroid preview map, purchase options, license status | VERIFIED (code) | `RouteDetailScreen.kt` renders: title, description, FlowRow chips (difficulty, terrain, region, distance, duration), `OsmPreviewMap` AndroidView (MapView centered on region), `LicenseStatusSection` (type + expiry + warnings), `PurchaseOptionsSection` (3 cards: Day Pass/Multi-Day/Permanent each with Contact to Purchase), disabled Start Navigation button. |
| 6 | My Routes shows only routes the user holds a valid license for | VERIFIED | `MyRoutesViewModel.kt` filters `getRoutesWithLicenseStatus()` result to `listOf(OWNED, ACTIVE, EXPIRING_SOON)` — AVAILABLE and EXPIRED are excluded. `MyRoutesScreen.kt` renders filtered list. |

**Score:** 6/6 truths verified at code level. 1 truth has a partial concern (no thumbnail map in cards). Human testing required for runtime behavior.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `android/app/src/main/java/com/roadrunner/app/RoadrunnerApp.kt` | @HiltAndroidApp; OSMDroid tile cache init | VERIFIED | `@HiltAndroidApp`, `osmdroidTileCache = File(filesDir, "osmdroid")` set in `onCreate()` before any map code |
| `android/app/src/main/java/com/roadrunner/app/MainActivity.kt` | @AndroidEntryPoint; session-aware start destination | VERIFIED | `@AndroidEntryPoint`, injects `AuthRepository`, sets `startDestination` conditionally |
| `android/app/src/main/java/com/roadrunner/app/navigation/NavGraph.kt` | All 5 destinations wired with real composables | VERIFIED | All 5 routes use real screen composables: LoginScreen, RegisterScreen, CatalogScreen, MyRoutesScreen, RouteDetailScreen. No Box/Text placeholders remain. |
| `android/app/src/main/java/com/roadrunner/app/data/local/TokenStorage.kt` | Tink AES-256-GCM saveTokens/getAccessToken/getRefreshToken/clearTokens | VERIFIED | Full Tink `AndroidKeysetManager` + `Aead` implementation, no `security-crypto` usage |
| `android/app/src/main/java/com/roadrunner/app/data/remote/interceptor/AuthInterceptor.kt` | Adds Authorization: Bearer header | VERIFIED | Reads `getAccessToken()`, adds `Authorization: Bearer $token` header when non-null |
| `android/app/src/main/java/com/roadrunner/app/data/remote/interceptor/TokenRefreshAuthenticator.kt` | Handles 401 via refresh + retry | VERIFIED | `runBlocking` calls `authApiService.refresh()` on 401, saves new tokens, retries with new Bearer |
| `android/app/src/main/java/com/roadrunner/app/data/repository/AuthRepository.kt` | isLoggedIn, login, register, loginWithGoogle, logout | VERIFIED | All 5 methods implemented, all call correct endpoints, `logout()` clears tokens even on network failure |
| `android/app/src/main/java/com/roadrunner/app/di/NetworkModule.kt` | Two Retrofit instances (auth/non-auth) | VERIFIED | `provideAuthApiService()` uses plain `OkHttpClient`; `provideApiService()` uses auth-intercepted client |
| `android/app/src/main/java/com/roadrunner/app/ui/auth/AuthViewModel.kt` | HiltViewModel with AuthUiState StateFlow | VERIFIED | `@HiltViewModel`, `StateFlow<AuthUiState>`, all 5 functions including `resetState()` |
| `android/app/src/main/java/com/roadrunner/app/ui/auth/LoginScreen.kt` | Email/password form + Google Sign-In button | VERIFIED | `OutlinedTextField` for email/password, Sign In `Button`, `OutlinedButton` for Google, `CircularProgressIndicator`, error `Text`, password visibility toggle |
| `android/app/src/main/java/com/roadrunner/app/ui/auth/RegisterScreen.kt` | Registration form with client-side validation | VERIFIED | Email/password fields, 8-char minimum validation with inline error, Create Account button |
| `android/app/src/main/java/com/roadrunner/app/data/remote/dto/RouteDtos.kt` | RouteDto, LicenseStatus, RouteWithLicense | VERIFIED (inferred from imports) | Used across CatalogViewModel, RouteCard, MyRoutesViewModel, RouteRepository, RouteDetailScreen |
| `android/app/src/main/java/com/roadrunner/app/data/repository/RouteRepository.kt` | getRoutesWithLicenseStatus(), getRoute(), checkLicenseStatus() | VERIFIED | All 3 methods implemented, license status resolved via `/api/v1/licenses/check`, `java.time.Instant` with desugar |
| `android/app/src/main/java/com/roadrunner/app/ui/catalog/CatalogScreen.kt` | Scrollable list, PullToRefreshBox, TopAppBar | VERIFIED | `PullToRefreshBox`, `LazyColumn` of `RouteCard`, TopAppBar with My Routes icon + Sign Out icon, loading/error states |
| `android/app/src/main/java/com/roadrunner/app/ui/catalog/RouteCard.kt` | Card with all CATA-02 fields + license badge | PARTIAL | Has title, DifficultyBadge, region, distance km, terrain type, LicenseStatusBadge. Missing thumbnail map. 5-state `LicenseStatusBadge` and 4-state `DifficultyBadge` are color-coded correctly. |
| `android/app/src/main/java/com/roadrunner/app/ui/myroutes/MyRoutesScreen.kt` | Licensed routes only | VERIFIED | Renders `RouteCard` list from `MyRoutesViewModel` which filters to OWNED/ACTIVE/EXPIRING_SOON |
| `android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailViewModel.kt` | HiltViewModel with SavedStateHandle routeId | VERIFIED | `savedStateHandle["routeId"]`, loads `getRoute()` + `checkLicenseStatus()` concurrently |
| `android/app/src/main/java/com/roadrunner/app/ui/routedetail/OsmPreviewMap.kt` | AndroidView wrapping MapView centered on region | VERIFIED | `AndroidView { MapView(context) }`, `REGION_COORDS` lookup, region marker, no tile cache setup (correct — handled by `RoadrunnerApp`) |
| `android/app/src/main/java/com/roadrunner/app/ui/routedetail/RouteDetailScreen.kt` | Full detail: metadata, map, purchase options, license status | VERIFIED | All DETL requirements: metadata FlowRow chips, OsmPreviewMap, LicenseStatusSection, PurchaseOptionsSection (3 cards), disabled Start Navigation button |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `RoadrunnerApp.kt` | OSMDroid Configuration | `osmdroidTileCache = File(filesDir, "osmdroid")` | WIRED | Line 16-17: `osmdroidTileCache = File(filesDir, "osmdroid")` confirmed |
| `MainActivity.kt` | `NavGraph.kt` | `RoadrunnerNavGraph(navController, startDestination)` | WIRED | `RoadrunnerNavGraph` called in `setContent`; `startDestination` determined by `authRepository.isLoggedIn()` |
| `LoginScreen.kt` | `AuthViewModel.kt` | `hiltViewModel()`, `collectAsState()` on `uiState` | WIRED | `hiltViewModel()` on line 46, `viewModel.uiState.collectAsState()` on line 48, `viewModel.login()` / `viewModel.googleSignIn()` called |
| `NavGraph.kt` | `AuthRepository.isLoggedIn()` | `startDestination` conditional in `MainActivity` | WIRED | `authRepository.isLoggedIn()` in `MainActivity.kt` lines 26-29 drives `startDestination` passed to `RoadrunnerNavGraph` |
| `TokenRefreshAuthenticator.kt` | `AuthApiService.refresh()` | `runBlocking { authApiService.refresh(RefreshRequest) }` | WIRED | `authApiService.refresh(RefreshRequest(refreshToken))` on line 23 |
| `NetworkModule.kt` | `AuthInterceptor.kt` | `OkHttpClient.Builder().addInterceptor(authInterceptor)` | WIRED | `addInterceptor(authInterceptor)` on line 39 |
| `AuthRepository.kt` | `TokenStorage.kt` | `tokenStorage.saveTokens()` after successful auth | WIRED | `saveTokens()` called in `login()`, `register()`, `loginWithGoogle()` after successful responses |
| `CatalogViewModel.kt` | `RouteRepository.getRoutes()` | `viewModelScope.launch { routeRepository.getRoutesWithLicenseStatus() }` | WIRED | `loadRoutes()` and `init` both call `routeRepository.getRoutesWithLicenseStatus()` |
| `RouteCard.kt` | `LicenseStatus` | `licenseStatus` parameter driving chip color and text | WIRED | `LicenseStatusBadge(status = route.licenseStatus)` in `RouteCard`, 5-way `when` on `LicenseStatus` |
| `MyRoutesScreen.kt` | Filtered routes | `filter { licenseStatus in listOf(OWNED, ACTIVE, EXPIRING_SOON) }` | WIRED | `MyRoutesViewModel.kt` lines 33-38: filter confirmed |
| `OsmPreviewMap.kt` | `MapView` | `AndroidView { MapView(context) }` | WIRED | `AndroidView { context -> MapView(context).apply { ... } }` confirmed |
| `RouteDetailScreen.kt` | `RouteDetailViewModel.uiState` | `collectAsState()` | WIRED | `viewModel.uiState.collectAsState()` on line 47 |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| AUTH-01 | 03-01, 03-02, 03-03 | Email/password registration | SATISFIED | `RegisterScreen.kt` → `AuthViewModel.register()` → `AuthRepository.register()` → `POST /api/v1/auth/register` |
| AUTH-02 | 03-01, 03-02, 03-03 | Google Sign-In | SATISFIED (code) | `LoginScreen.kt` Google button → `viewModel.googleSignIn()` → `AuthRepository.loginWithGoogle()` → CredentialManager → `POST /api/v1/auth/google`. Client ID placeholder acknowledged. |
| AUTH-03 | 03-01, 03-02, 03-03 | Session persistence across restarts | SATISFIED (code) | Tink-encrypted tokens in SharedPreferences survive app restarts. `MainActivity.authRepository.isLoggedIn()` routes to Catalog if token present. |
| AUTH-04 | 03-01, 03-02, 03-03 | Sign out | SATISFIED | `CatalogScreen` TopAppBar sign out → `viewModel.logout()` → `AuthRepository.logout()` clears tokens → `navController.navigate(Login) { popUpTo(0) { inclusive=true } }` |
| CATA-01 | 03-04 | Browse published routes | SATISFIED | `CatalogScreen` → `CatalogViewModel.loadRoutes()` → `RouteRepository.getRoutesWithLicenseStatus()` → `GET /api/v1/routes` |
| CATA-02 | 03-04 | Route card fields: title, distance, difficulty, terrain, region, thumbnail map | PARTIAL | Cards show all fields EXCEPT thumbnail map. CONTEXT.md deferred preview map to detail screen; success criterion says "thumbnail map per card". Intentional scope reduction needs human confirmation. |
| CATA-03 | 03-04 | License status badge per card | SATISFIED | `LicenseStatusBadge` renders 5 states with correct colors on every `RouteCard` |
| CATA-04 | 03-04 | My Routes shows only licensed routes | SATISFIED | `MyRoutesViewModel` filters to `OWNED/ACTIVE/EXPIRING_SOON` |
| DETL-01 | 03-05 | Route detail: full metadata | SATISFIED | `RouteDetailScreen` FlowRow chips: difficulty, terrain, region, distance, duration. Plus title and description text. |
| DETL-02 | 03-05 | Route detail: OSMDroid preview map | SATISFIED (code) | `OsmPreviewMap` composable wraps `MapView` via `AndroidView`, centered on region from `REGION_COORDS`, no tile cache setup (correct). |
| DETL-03 | 03-05 | Route detail: purchase options, license status | SATISFIED | `PurchaseOptionsSection` (3 cards), `LicenseStatusSection` (type, expiry, status warnings) |
| DETL-04 | 03-05 | Start Navigation button visible but disabled | SATISFIED | `Button(enabled = false)` with label "Start Navigation" on lines 137-145 |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `strings.xml` | 4 | `YOUR_GOOGLE_CLIENT_ID` placeholder | Info | Google Sign-In (AUTH-02) will throw `GetCredentialException` at runtime until a real OAuth 2.0 Web client ID is configured. This is intentional and documented in SUMMARY. |
| `RouteDetailScreen.kt` | 138 | `onClick = { /* Phase 5 */ }` | Info | Expected — Start Navigation intentionally disabled; no impact on Phase 3 goals |
| `PurchaseOptionCard` | 267 | `TextButton(onClick = { /* v1: manual licensing */ })` | Info | Expected — v1 manual licensing model; no action intentional |

No blockers or warnings found. All `return null`, `return {}`, placeholder composables, and `console.log`-only implementations were absent from production screen code.

### Human Verification Required

#### 1. Google One-Tap Sign-In

**Test:** Replace `YOUR_GOOGLE_CLIENT_ID` in `android/app/src/main/res/values/strings.xml` with a real OAuth 2.0 Web client ID, install app on emulator with Play Services, tap "Sign in with Google" on Login screen.
**Expected:** Google one-tap bottom sheet appears, user selects account, app navigates to Catalog with token stored.
**Why human:** CredentialManager requires Play Services and a valid OAuth client ID; cannot test programmatically.

#### 2. Session Persistence Across App Restarts

**Test:** Register or log in, force-stop the app from system settings, reopen the app.
**Expected:** App launches directly to Catalog screen without showing Login.
**Why human:** Requires running app on emulator/device to verify Tink keyset + SharedPreferences round-trip across process death.

#### 3. Thumbnail Map on Route Cards (CATA-02 scope question)

**Test:** Review `android/app/src/main/java/com/roadrunner/app/ui/catalog/RouteCard.kt` and compare against CATA-02 success criterion "thumbnail map per card".
**Expected:** Confirm whether the absence of a thumbnail map image on catalog cards is an acceptable scope reduction (map preview is on detail screen only) or a missing requirement.
**Why human:** CONTEXT.md explicitly defers GPX preview to Phase 4 and describes cards as showing metadata only. The success criterion wording ("thumbnail map per card") is ambiguous — it could mean the map-like difficulty chip or a literal mini map. A human needs to make the product decision.

#### 4. OSMDroid Tile Rendering

**Test:** Open any route detail page on an emulator with internet access.
**Expected:** The 220dp map area shows OpenStreetMap tiles with a region marker.
**Why human:** Tile loading requires network connectivity and a running MapView; cannot verify rendering programmatically.

#### 5. Sign Out Back-Stack Isolation

**Test:** Sign in, then tap Sign Out from TopAppBar. On the Login screen, press the Android Back button.
**Expected:** App either closes or stays on Login — does not navigate back to Catalog.
**Why human:** Back-stack behavior at runtime requires emulator testing; `popUpTo(0) { inclusive = true }` is the correct implementation but runtime verification is needed.

### Gaps Summary

No blocking gaps found. All source files exist, are substantive, and are correctly wired. The one concern is whether the absence of a thumbnail map image on catalog route cards (CATA-02) constitutes a gap or intentional scope reduction. The CONTEXT.md description of Phase 3 catalog cards does not mention thumbnail maps; only the high-level success criterion does. This ambiguity requires a human product decision.

---

_Verified: 2026-03-15_
_Verifier: Claude (gsd-verifier)_
