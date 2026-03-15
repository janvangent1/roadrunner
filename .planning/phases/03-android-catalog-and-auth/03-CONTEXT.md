---
phase: 3
title: Android Catalog and Auth
status: ready
---

# Phase 3 Context: Android Catalog and Auth

## Decisions

### Android Stack (locked from research)
- **Language:** Kotlin
- **Min SDK:** 24 (required by Tink 1.20.0)
- **Target SDK:** 35 (Android 15)
- **UI:** Jetpack Compose
- **Architecture:** MVVM + Repository pattern (Clean Architecture)
- **Navigation:** Jetpack Navigation Compose
- **HTTP:** Retrofit 2 + OkHttp + Gson (or Moshi)
- **DI:** Hilt
- **Map library:** OSMDroid 6.1.20 (archived but functional — deliberate project decision)
- **Async:** Kotlin Coroutines + Flow
- **Build:** Gradle (Kotlin DSL), product flavor `motorcycle` (scaffold only — full branding in Phase 7)

### Project structure
Create Android project in `android/` directory at repo root alongside `backend/` and `dashboard/`.
Package name: `com.roadrunner.app`
Module structure: single `:app` module for v1 (clean architecture layers within the module)

### Authentication
- **Login screen:** Email + password form → `POST /api/v1/auth/login` → store JWT in `EncryptedSharedPreferences` (Jetpack Security)
- **Google Sign-In:** Use `CredentialManager` API (modern Android sign-in) → get Google ID token → `POST /api/v1/auth/google` → store JWT
- **Session persistence:** On app launch check for stored JWT → if valid navigate to catalog, else to login
- **Sign out:** Clear stored JWT + navigate to login

### Network layer
- Base URL configurable via `BuildConfig.BASE_URL` (set in `build.gradle.kts` per flavor/buildType)
- `AuthInterceptor` adds `Authorization: Bearer <token>` to all requests
- `TokenRefreshInterceptor` (or Authenticator) handles 401s → `POST /api/v1/auth/refresh` → retry
- No plaintext base URL hardcoded in source

### Screens this phase
```
LoginScreen         → email/password + Google Sign-In button
RegisterScreen      → email/password registration
CatalogScreen       → scrollable list of routes (published only)
RouteDetailScreen   → route metadata + preview map + license status
MyRoutesScreen      → user's licensed routes only
```

### Route catalog display
- Each route card: title, region, difficulty badge (color-coded), distance, terrain type, license status chip
- License status chip values: "Available", "Owned", "Expires in X days", "Expired"
- Pull-to-refresh
- No pagination needed for v1 (catalog will be small)

### Route detail screen
- Shows: title, description, difficulty, terrain type, region, estimated duration, distance
- Preview map: OSMDroid map view showing the route line (polyline) without requiring the user to own it — but the GPX data comes from the backend encrypted; for the preview we show a simplified bounding box or just metadata (no GPX data needed for preview in Phase 3 — GPX download/decrypt is Phase 4)
- In Phase 3, the preview map can show an empty map centered on the route's region or a placeholder — full GPX overlay is Phase 4
- Purchase options section: shows license types and "Contact to purchase" text (actual purchase flow is manual — Phase 3 just displays what's available)
- If user has a license: show license type, expiry date, and "Start Navigation" button (disabled in Phase 3 — enabled in Phase 5)

### OSMDroid setup
- Tile source: `TileSourceFactory.MAPNIK` (OpenStreetMap)
- Tile cache path: `context.filesDir` (NOT external storage — Android 13+ scoped storage requirement from pitfalls research)
- User agent: set to `BuildConfig.APPLICATION_ID`
- Internet permission required

### My Routes screen
- Simple list of routes the user has a license for
- Shows same route card as catalog but filtered

## Deferred to later phases
- GPX download and decryption (Phase 4)
- License validation / navigation unlock (Phase 5)
- Full navigation screen (Phase 6)
- R8 / Play Integrity (Phase 7)
