# Architecture Research

**Domain:** Android licensed-content navigation app with server-side enforcement and admin dashboard
**Researched:** 2026-03-14
**Confidence:** HIGH (Android patterns), MEDIUM (encryption security boundaries on rooted devices)

## Standard Architecture

### System Overview

```
┌────────────────────────────────────────────────────────────────┐
│                        Android App                             │
├──────────────┬──────────────┬──────────────┬───────────────────┤
│   UI Layer   │              │              │                   │
│  (Compose /  │  Navigation  │  Map Screen  │  Route Browser    │
│   Fragments) │  Screen      │  (MapLibre)  │  (Catalog)        │
├──────────────┴──────────────┴──────────────┴───────────────────┤
│                     ViewModel / State                           │
│  LicenseViewModel  NavigationViewModel  CatalogViewModel        │
├────────────────────────────────────────────────────────────────┤
│                     Domain / Use Cases                          │
│  ValidateLicense  DecryptRoute  FetchCatalog  StartNavigation   │
├──────────────┬─────────────────────────────────────────────────┤
│  Data Layer  │                                                  │
│  RouteRepo   │  LicenseRepo         BillingRepo                 │
├──────────────┴─────────────────────────────────────────────────┤
│                     Infrastructure                              │
│  EncryptedFileStore  RoomDB  RetrofitApiClient  PlayBilling     │
└────────────────────────────────────────────────────────────────┘
         │                        │
         ▼                        ▼
┌─────────────────┐    ┌─────────────────────────────────────────┐
│  Google Play    │    │            Backend Server                │
│  Billing API    │    ├──────────────┬──────────────────────────┤
│                 │    │  License API │  Route Distribution API   │
│  - Purchase     │    │  /validate   │  /routes  /routes/{id}   │
│  - Acknowledge  │    │  /status     │  /routes/{id}/gpx        │
│  - Verify token │    ├──────────────┴──────────────────────────┤
└─────────────────┘    │  Admin API                               │
                       │  /admin/routes (CRUD + upload)          │
                       ├─────────────────────────────────────────┤
                       │  Google Play Developer API (server auth) │
                       │  purchases.products.get                  │
                       │  purchases.subscriptions.get             │
                       └─────────────────────────────────────────┘
                                        │
                       ┌────────────────▼────────────────────────┐
                       │         Admin Web Dashboard              │
                       │  React SPA — GPX upload, route mgmt,    │
                       │  license overview, pricing config        │
                       └─────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| Android UI | Route browsing, navigation display, purchase prompts | Jetpack Compose or Fragments |
| ViewModel | UI state, exposes StateFlow, calls use cases | Android ViewModel + Kotlin Coroutines |
| Use Cases | Single-action business logic, coordinates repos | Plain Kotlin classes (no Android deps) |
| RouteRepository | Fetches catalog, downloads encrypted GPX, stores locally | Retrofit + Room + EncryptedFile |
| LicenseRepository | Validates license server-side, caches status with TTL | Retrofit + Room (cache) |
| BillingRepository | Initiates Google Play purchase, returns purchase token | Play Billing Library 7+ |
| EncryptedFileStore | Writes/reads GPX bytes using AES-256-GCM via Android Keystore | Jetpack Security EncryptedFile |
| MapLibre MapView | Renders tiles + GPX overlay, never exposes raw route data | MapLibre Native Android |
| Backend License API | Receives purchase token, calls Google Play Developer API, stores entitlement | Node.js / Express |
| Backend Route API | Serves encrypted GPX to validated clients, enforces license check | Node.js / Express |
| Backend Admin API | Accepts GPX upload, encrypts server-side, stores, manages catalog | Node.js / Express |
| Admin Web Dashboard | React SPA, GPX upload form, route CRUD, pricing | React + Vite |
| Database (backend) | Stores routes, licenses, users, entitlements | PostgreSQL |
| File storage (backend) | Stores encrypted GPX files | Local disk or S3-compatible |

---

## Recommended Project Structure

### Android App

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/roadrunner/
│   │   │   ├── ui/
│   │   │   │   ├── catalog/          # Route browse + detail screens
│   │   │   │   ├── navigation/       # Map + active navigation screen
│   │   │   │   ├── purchase/         # Buy / license flow UI
│   │   │   │   └── components/       # Shared UI components
│   │   │   ├── viewmodel/            # ViewModels per feature screen
│   │   │   ├── domain/
│   │   │   │   ├── usecase/          # ValidateLicense, DecryptRoute, etc.
│   │   │   │   └── model/            # Domain entities (Route, License)
│   │   │   ├── data/
│   │   │   │   ├── repository/       # RouteRepo, LicenseRepo, BillingRepo
│   │   │   │   ├── local/
│   │   │   │   │   ├── db/           # Room database + DAOs
│   │   │   │   │   └── encryption/   # EncryptedFileStore
│   │   │   │   ├── remote/
│   │   │   │   │   ├── api/          # Retrofit service interfaces
│   │   │   │   │   └── dto/          # Network data transfer objects
│   │   │   │   └── billing/          # Google Play Billing wrapper
│   │   │   └── di/                   # Hilt modules
│   │   └── res/
│   └── motorcycle/                   # Flavor: motorcycle branding/assets
│       └── res/
│           ├── values/strings.xml    # "Roadrunner" branding
│           └── drawable/             # Motorcycle-specific icons
```

### Backend

```
backend/
├── src/
│   ├── routes/
│   │   ├── license.js         # POST /validate, GET /status/:userId/:routeId
│   │   ├── routes.js          # GET /routes, GET /routes/:id, GET /routes/:id/gpx
│   │   └── admin.js           # Admin CRUD + GPX upload
│   ├── services/
│   │   ├── playBilling.js     # Google Play Developer API calls
│   │   ├── licenseService.js  # Entitlement logic + grace period
│   │   ├── routeService.js    # Route catalog, encrypted GPX delivery
│   │   └── encryptionService.js # Server-side GPX encryption
│   ├── middleware/
│   │   ├── auth.js            # JWT for admin, device auth for app
│   │   └── rateLimiter.js
│   ├── db/
│   │   ├── migrations/        # PostgreSQL schema migrations
│   │   └── models/            # ORM models (Prisma or Sequelize)
│   └── config/
│       └── googlePlay.js      # Service account config
├── uploads/                   # Temporary GPX upload staging
└── storage/                   # Encrypted GPX files on disk
```

### Admin Dashboard

```
admin-dashboard/
├── src/
│   ├── pages/
│   │   ├── RouteList.jsx      # Browse / manage routes
│   │   ├── RouteEdit.jsx      # Edit metadata + re-upload GPX
│   │   ├── RouteUpload.jsx    # New route + GPX file upload
│   │   └── LicenseReport.jsx  # License/purchase overview (read-only)
│   ├── api/
│   │   └── adminApi.js        # Axios client for backend admin endpoints
│   └── components/
│       └── GpxMapPreview.jsx  # Preview uploaded GPX before publishing
```

### Structure Rationale

- **ui/ vs viewmodel/ vs domain/ vs data/:** Clean Architecture layers. UI and ViewModel depend on Domain; Domain never imports Android or Data. Allows testing use cases without Android emulator.
- **motorcycle/ flavor source set:** Android product flavors let a future sport car variant share all core logic while swapping only branding, strings, and package name. Zero code duplication.
- **encryption/ under local/:** Keeps the GPX decryption concern isolated. MapLibre only receives a decoded byte array, never a file path it could expose.
- **backend services/ separate from routes/:** Route handlers stay thin; all business logic lives in services, making unit testing straightforward without HTTP stubs.

---

## Architectural Patterns

### Pattern 1: Layered Clean Architecture (Android)

**What:** Presentation → Domain → Data. Each layer only depends inward. ViewModels call use cases; use cases call repositories; repositories call local/remote sources.
**When to use:** Any Android app with non-trivial business logic. Mandatory here because license validation and decryption logic must be testable without a real device.
**Trade-offs:** More boilerplate up front. Pays back when adding the sport car flavor or writing unit tests.

**Example:**
```kotlin
// Use case — no Android imports, fully testable
class ValidateLicenseUseCase(
    private val licenseRepo: LicenseRepository
) {
    suspend operator fun invoke(routeId: String, userId: String): LicenseStatus {
        return licenseRepo.validateLicense(routeId, userId)
    }
}
```

### Pattern 2: Decrypt-at-Render, Never Write Plaintext

**What:** Encrypted GPX bytes are downloaded and stored with Jetpack Security `EncryptedFile` (AES-256-GCM, key in Android Keystore). When the user begins navigation, the file is decrypted in memory and the byte array is passed directly to the map layer. It is never written to a temp file or exposed through a file URI.
**When to use:** Any time you need to protect binary content on-device from extraction via ADB, root file explorers, or backup extraction.
**Trade-offs:** A sophisticated attacker with root and the right memory dump tooling can still capture the decrypted bytes at runtime. This is the accepted limit — the goal is to eliminate casual/automated extraction, not nation-state-level attacks.

**Example:**
```kotlin
fun readDecryptedGpx(routeId: String): ByteArray {
    val encryptedFile = EncryptedFile.Builder(
        context,
        File(context.filesDir, "$routeId.gpx.enc"),
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
    ).build()
    return encryptedFile.openFileInput().use { it.readBytes() }
    // Caller passes ByteArray directly to MapLibre — no temp file
}
```

### Pattern 3: Server-Side License Verification with Client Cache + Grace Period

**What:** On app launch and before navigation start, the app posts the user's purchase token to your backend. The backend calls `purchases.products.get` on the Google Play Developer API, confirms the purchase is valid and unexpired, records the entitlement, and returns a signed status response. The app caches this locally with a TTL (e.g., 24 hours for day passes, shorter for expiring rentals). A 1-hour grace period keeps active navigation sessions alive if connectivity is lost exactly at expiry.
**When to use:** Whenever client-side-only license checks are insufficient (e.g., shared account attack vector). Required for this project.
**Trade-offs:** Requires network for initial validation. The grace period handles legitimate offline scenarios during active rides.

---

## Data Flow

### Purchase and License Activation Flow

```
User taps "Buy"
    ↓
BillingRepository → Google Play Billing (in-app purchase dialog)
    ↓ (purchase token returned on success)
BillingRepository → POST /license/validate {userId, purchaseToken, routeId}
    ↓
Backend LicenseService → Google Play Developer API purchases.products.get
    ↓ (purchase confirmed)
Backend → stores entitlement in PostgreSQL (userId, routeId, expiresAt, token)
    ↓
Backend → returns {status: "active", expiresAt, signedToken}
    ↓
LicenseRepository → stores in Room (caches for TTL duration)
    ↓
CatalogViewModel → unlocks route for download + navigation
```

### Route Download Flow (after license active)

```
User taps "Download Route"
    ↓
RouteRepository → GET /routes/{id}/gpx (bearer: device auth token)
    ↓
Backend → checks license in DB for requesting userId + routeId
    ↓ (license valid)
Backend → reads encrypted GPX bytes from disk, streams to client
    ↓ (note: GPX is already encrypted server-side before storage)
RouteRepository → writes bytes to EncryptedFile (app internal storage)
    ↓
Route marked as "available offline" in Room DB
```

### Navigation Start Flow

```
User taps "Start Navigation"
    ↓
ValidateLicenseUseCase → checks cached Room license record
    ├── expired AND within grace period → allow (start 1h countdown)
    ├── expired AND outside grace period → block, prompt purchase
    └── valid → proceed
    ↓
DecryptRouteUseCase → EncryptedFileStore.readDecryptedGpx(routeId)
    ↓ (ByteArray in memory)
MapLibre MapView → parseGpx(bytes) → draw polyline overlay
    ↓
NavigationViewModel → exposes location updates, bearing, distance remaining
    ↓ (ByteArray goes out of scope, GC'd; never persisted as plaintext)
```

### Admin Route Upload Flow

```
Admin logs into dashboard
    ↓
React dashboard → POST /admin/routes (multipart: metadata + .gpx file)
    ↓
Backend Admin API → validates JWT (admin-only)
    ↓
EncryptionService → reads raw GPX, encrypts with AES-256-GCM, derives key per-route
    ↓
Backend → stores encrypted bytes to disk/storage
Backend → stores route metadata in PostgreSQL (name, distance, difficulty, price, encryptedPath)
    ↓
Admin dashboard → shows route as "published" in catalog
```

### State Management (Android)

```
Room DB (source of truth for local data)
    ↓ (Flow / LiveData)
Repository exposes StateFlow<LicenseStatus>, StateFlow<List<Route>>
    ↓
ViewModel collects, transforms to UI state
    ↓
Compose / Fragment observes UI state, re-renders
```

---

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| Google Play Billing (client) | Play Billing Library 7+ (required by Aug 2025) | Must acknowledge purchases within 3 days or Google auto-refunds |
| Google Play Developer API (server) | Service account OAuth2, REST purchases.products.get | Never call this from the Android app — server only |
| Google Play Real-Time Developer Notifications | Cloud Pub/Sub subscription on backend | Catches subscription cancellations, refunds, renewals automatically |
| Android Keystore | Jetpack Security EncryptedFile — no direct Keystore calls needed | Hardware-backed on API 28+ (most modern devices) |
| MapLibre Native Android | MapView in layout, tiles from OSM tile server or local MBTiles | OSMDroid is archived (Nov 2024), MapLibre is the maintained successor |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Android App ↔ Backend | HTTPS REST, device authenticated with JWT | Token issued by backend on first launch / account registration |
| Backend ↔ Google Play API | Google API client library (server auth) | Never expose service account key to app |
| Admin Dashboard ↔ Backend | HTTPS REST, separate admin JWT | Admin routes behind middleware check |
| MapLibre ↔ RouteRepository | ByteArray passed in memory | No file URI handoff — prevents file system extraction |
| ViewModel ↔ Use Cases | Coroutine suspend functions | Use cases return domain models, not DTOs |

---

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| 0–1k users | Single-server Node.js + PostgreSQL on one VPS. No queue needed. S3-compatible storage recommended over local disk from day one (avoids migration pain). |
| 1k–50k users | Add Redis for license status caching (avoid DB hit on every nav start check). CDN for map tiles if self-hosting. Consider separating admin API from user API. |
| 50k+ users | Horizontal scaling of the API server. Read replicas for PostgreSQL. Consider presigned S3 URLs for GPX delivery (offloads bandwidth from app server). RTDN processing can move to a dedicated worker. |

### Scaling Priorities

1. **First bottleneck:** License validation endpoint — every navigation start hits it. Redis TTL cache on `userId:routeId` license status resolves this early.
2. **Second bottleneck:** GPX file delivery — large files, many concurrent downloads. Presigned S3 URLs let the storage layer serve files directly, removing the Node.js process from the data path.

---

## Anti-Patterns

### Anti-Pattern 1: Client-Side-Only License Checking

**What people do:** Store a boolean "hasPurchased" in SharedPreferences or Room, check it locally before navigation.
**Why it's wrong:** Trivially bypassable with a rooted device or APK mod. A shared Google account can unlock routes for unlimited devices. This defeats the entire business model.
**Do this instead:** Every navigation start (or at minimum every cold start) sends a validation request to your backend, which calls Google Play Developer API. Cache the server response with a short TTL, not the raw boolean.

### Anti-Pattern 2: Writing Decrypted GPX to a Temp File

**What people do:** Decrypt the GPX to a temp file in cache dir, pass the file URI to the map library.
**Why it's wrong:** Files in cache dir are accessible to root users, ADB backup (pre-Android 12), and some backup tools. The plaintext route can be extracted while the file exists.
**Do this instead:** Decrypt to a `ByteArray` in memory, pass directly to the parser. The array is GC'd when navigation ends. No plaintext ever touches the filesystem.

### Anti-Pattern 3: Storing the Encryption Key in Code or SharedPreferences

**What people do:** Hardcode an AES key in a constants file, or derive it from a hardcoded salt. Alternatively: store the key in SharedPreferences (even obfuscated).
**Why it's wrong:** Keys in compiled code are recoverable with decompilation + Frida. SharedPreferences is readable on rooted devices.
**Do this instead:** Use Android Keystore exclusively. Jetpack Security `MasterKeys.getOrCreate()` handles key generation and storage inside the Keystore. The key never leaves the TEE on supported hardware.

### Anti-Pattern 4: Calling Google Play Developer API from the Android App

**What people do:** Embed the Google Play service account JSON key in the APK to validate purchases client-side.
**Why it's wrong:** The service account key in the APK can be extracted, giving an attacker full write access to your Play Console API — including publishing apps under your account.
**Do this instead:** The app only handles the in-app purchase dialog and receives the purchase token. The token is sent to your backend, which holds the service account key in a secure environment variable and calls the Google API.

### Anti-Pattern 5: Using OSMDroid in 2025+

**What people do:** Follow older tutorials and add osmdroid as the map dependency.
**Why it's wrong:** OSMDroid was archived in November 2024. Version 6.1.20 is the final release — no security fixes, no new Android API compatibility patches.
**Do this instead:** Use MapLibre Native Android. It is the actively maintained open-source OpenStreetMap renderer, supports offline MBTiles/PMTiles, handles GPX overlays, and does not require a paid API key.

---

## Suggested Build Order

The architecture has hard dependencies that dictate sequencing. Build in this order to avoid rework:

```
Phase 1 — Backend Foundation
    PostgreSQL schema + Prisma/Sequelize models
    Auth middleware (device JWT + admin JWT)
    Route catalog API (GET /routes, GET /routes/:id)
    Admin API (POST /admin/routes with GPX upload)
    Server-side GPX encryption on upload

    Reason: Android app needs a real API to develop against.
    Admin dashboard needs the upload API to exist.

Phase 2 — Admin Dashboard
    React SPA with GPX upload form
    Route metadata management (name, price, difficulty)
    Preview uploaded GPX on a map before publishing

    Reason: Routes must exist in the system before the app
    can display or purchase them. Admin tooling unblocks
    all further Android testing.

Phase 3 — Android: Catalog + Route Display
    MapLibre integration + tile loading
    Retrofit API client + RouteRepository
    Route catalog screen (list + detail)
    GPX overlay rendering (in-memory, from network response)
    Room DB for catalog caching

    Reason: Validate the map rendering before adding encryption
    complexity. Isolate rendering bugs from encryption bugs.

Phase 4 — Android: Encryption Layer
    EncryptedFile storage for downloaded GPX
    Decrypt-at-render pipeline (ByteArray → MapLibre)
    Download + offline availability tracking in Room

    Reason: Encryption is layered on top of a working
    rendering pipeline. Much easier to debug this way.

Phase 5 — Billing + License Enforcement
    Google Play Billing Library integration (BillingRepository)
    Backend: POST /license/validate → Google Play Developer API
    LicenseRepository with Room cache + TTL
    Backend: RTDN via Cloud Pub/Sub for subscription events
    Grace period logic (1-hour active session extension)
    Navigation gate (ValidateLicenseUseCase blocks start if expired)

    Reason: Billing requires a published app in Play Console for
    end-to-end testing. Build all other functionality first
    so billing bugs don't block unrelated development.

Phase 6 — Android: Product Flavor Setup
    motorcycle/ flavor source set with branding
    Verify core code is flavor-agnostic
    Build variant smoke test

    Reason: Done last because it only touches branding.
    Core logic is already proven before flavors are wired in.
```

---

## Sources

- [Android Guide to App Architecture — official](https://developer.android.com/topic/architecture) — HIGH confidence
- [Integrate Google Play with your server backend](https://developer.android.com/google/play/billing/backend) — HIGH confidence
- [Server-side license verification — Android Developers](https://developer.android.com/google/play/licensing/server-side-verification) — HIGH confidence
- [Android Keystore system](https://developer.android.com/privacy-and-security/keystore) — HIGH confidence
- [Jetpack Security EncryptedFile](https://www.cobeisfresh.com/blog/how-to-encrypt-data-on-android-with-jetpack-security) — MEDIUM confidence (third-party article, cross-referenced with official API)
- [Google Play Billing Library release notes](https://developer.android.com/google/play/billing/release-notes) — HIGH confidence (requirement: v7+ by Aug 2025)
- [osmdroid archived Nov 2024](https://github.com/osmdroid/osmdroid) — HIGH confidence (official GitHub archive)
- [MapLibre Native Android](https://maplibre.org/maplibre-native/android/api/) — HIGH confidence
- [MapLibre offline maps on Android](https://medium.com/@ty2/how-to-display-offline-maps-using-maplibre-mapbox-39ad0f3c7543) — MEDIUM confidence (community article)
- [Android product flavors for white-labeling](https://developer.android.com/build/build-variants) — HIGH confidence
- [google-play-billing-validator npm package](https://github.com/Deishelon/google-play-billing-validator) — MEDIUM confidence (community library, widely used)
- [Server-side purchase validation on Google Play — Adapty](https://adapty.io/blog/android-in-app-purchases-server-side-validation/) — MEDIUM confidence (cross-referenced with official docs)

---
*Architecture research for: Android GPX navigation app with server-side licensing (Roadrunner)*
*Researched: 2026-03-14*
