# Project Research Summary

**Project:** Roadrunner
**Domain:** Android navigation app with encrypted/licensed GPX route content (offroad motorcycle)
**Researched:** 2026-03-14
**Confidence:** MEDIUM-HIGH

## Executive Summary

Roadrunner is an Android-first navigation app that sells curated offroad motorcycle routes through a per-route licensing model (day pass, multi-day rental, permanent purchase). The core business constraint — that GPX route data must never be extractable from a user's device — drives nearly every architectural and technology decision. Experts build this class of product with a strict three-layer trust model: content is encrypted server-side before it ever leaves the backend, delivered encrypted to the device, and decrypted only in memory at render time. Any shortcut in this chain (plaintext temp files, client-side-only license checks, keys in APK constants) breaks the entire business model and is unrecoverable without a forced app update.

The recommended approach is a Kotlin/Jetpack Compose Android app with OSMDroid 6.1.20 for map rendering (a project constraint; accept its archived status), Google Tink for AES-256-GCM encryption backed by Android Keystore, and Play Billing Library 8.3.0 for purchases. The backend is a Fastify/Node.js service with PostgreSQL and Redis, and a Next.js 15 admin dashboard handles content management. The most important architectural decision is that all license verification routes through the backend to the Google Play Developer API — the Android client never makes this call directly. The 1-hour grace period for active navigation sessions is a deliberate UX safety net for offroad environments with no cell signal, not a security hole.

The highest risks are all on the security and compliance side: client-side-only license enforcement is trivially bypassable and must be addressed from the start, not retrofitted; the `linkedPurchaseToken` field in Google Play Billing must be handled from day one or zombie tokens accumulate; and OSMDroid's default tile cache targets external storage, which breaks silently on Android 13+ devices. None of these risks are exotic — they are all well-documented and avoidable with a correct initial architecture. The product's defensible advantage over every competitor (REVER, onX, AllTrails) is the combination of per-route licensing and encrypted content — no competitor does both.

---

## Key Findings

### Recommended Stack

The Android app targets API 35 (mandatory for Play Store since Aug 2025) with a minimum of API 24 (required by Tink 1.20.0; covers 99%+ of active devices). The build system is Kotlin 2.3.0 with AGP 9.0.1 and Gradle 9.1+. Two library decisions require particular attention: `androidx.security:security-crypto` (EncryptedFile) was fully deprecated in Jul 2025 — use Google Tink 1.20.0 directly instead; and the old `android-database-sqlcipher` Maven artifact is deprecated — use `net.zetetic:sqlcipher-android:4.13.0`. The backend is Node.js 22 LTS with Fastify 5 (TypeScript-first, significantly faster than Express), Prisma 6 on PostgreSQL 17, and Redis 7 for license caching. The admin dashboard is Next.js 15.5 with Shadcn/ui and Auth.js.

**Core technologies:**
- Kotlin 2.3.0 + AGP 9.0.1: primary app language and build system — current stable, K2 compiler default
- Jetpack Compose 1.10 (BOM 2025.12): UI — official Google direction, Material 3 v1.4 included
- OSMDroid 6.1.20: map rendering + GPX overlay — project constraint; final release (archived Nov 2024), fully functional
- Google Tink 1.20.0 + Android Keystore: AES-256-GCM encryption — official replacement for deprecated security-crypto
- Play Billing Library 8.3.0: purchases — mandatory v7+ since Aug 2025; v8 adds auto-reconnection and multi-purchase options
- Room 2.8.4: local database for license records and route metadata — Kotlin-first in 2.8.x
- Fastify 5 / Node.js 22 LTS: backend API — TypeScript-first, 2-3x Express throughput
- PostgreSQL 17 + Redis 7: primary DB and license cache — transactional license records + TTL-based session tracking
- Next.js 15.5 + Shadcn/ui: admin dashboard — Server Actions eliminate boilerplate for GPX upload/CRUD

### Expected Features

Research confirms no competitor in the offroad motorcycle space uses per-route licensing with encrypted content. This is Roadrunner's defensible moat. The feature set is well-defined with clear dependencies.

**Must have (table stakes):**
- User account (email/password + Google Sign-In) — license is account-bound; required before any purchase
- Route catalog (list view with title, distance, difficulty, region, thumbnail) — browsing is the purchase entry point
- Route detail page (preview map with route line visible, metadata, purchase options) — decision-enabling before payment
- Google Play Billing purchase flow (day pass, multi-day rental, permanent) — the business model
- Navigation screen (OSMDroid base map + encrypted GPX polyline overlay + GPS position dot) — core product
- Offline map tile pre-caching for purchased routes — safety requirement, not a feature (offroad = no signal)
- Encrypted GPX storage with decrypt-at-render-only pipeline — core business protection
- Server-side license validation + 1-hour grace period for active sessions — integrity + offline safety net
- License status display on routes (Owned / Expires in N days / Expired) — trust and transparency
- My Routes / library screen — basic ownership UX
- Admin web dashboard (GPX upload, route metadata, catalog management) — must ship before or alongside the app

**Should have (competitive differentiators):**
- Elevation profile in route detail — riders assess physical demand before purchase (requires GPX Z-values)
- Active navigation ride stats HUD (speed, distance covered, distance remaining, elapsed time)
- Waypoints / POIs on route — admin-defined in GPX, displayed during navigation
- License renewal / expiry push notification — converts casual renters to permanent purchasers
- Route region / area browsing — geographic grouping, useful when catalog reaches 10+ routes
- Offline map pre-download prompt immediately after purchase — reduces "no map when I get there" support requests

**Defer (v2+):**
- Turn-by-turn voice guidance — high complexity, out of scope per project constraints; validate demand first
- Social features / ratings — scope explosion; moderation overhead; not a v1 differentiator
- iOS version — doubles engineering overhead; Android-only is the correct v1 constraint
- Multiple sellers / marketplace — architectural change; only if single-seller model saturates
- Sport car app variant — re-skin from same architecture; deliberately deferred per project scope

**Anti-features to reject explicitly:**
- GPX file export — destroys the content protection business model
- User-uploaded / community routes — conflicts with curated quality model and introduces moderation burden

### Architecture Approach

The system follows three layers: an Android app with Clean Architecture (Presentation → Domain → Data), a Fastify backend handling all license validation and GPX delivery, and a Next.js admin dashboard for content management. The critical security boundary is that GPX data is encrypted server-side at upload time, served as ciphertext to the Android client, stored in app-private internal storage, and decrypted in memory only at render time — the decrypted byte array is passed directly to the OSMDroid overlay renderer and then GC'd. No plaintext GPX ever touches the filesystem. All Google Play Developer API calls happen server-to-server only; the Android app never holds the service account key.

**Major components:**
1. Android UI (Jetpack Compose) — route browsing, navigation display, purchase prompts
2. Android Domain layer (Use Cases) — ValidateLicense, DecryptRoute, FetchCatalog, StartNavigation; no Android imports, fully testable
3. Android Data layer (Repositories) — RouteRepo, LicenseRepo, BillingRepo; abstracts Room, Retrofit, EncryptedFile, Play Billing
4. EncryptedFileStore — AES-256-GCM via Tink + Android Keystore; the security boundary between disk and memory
5. Backend License API — receives purchase token, verifies against Google Play Developer API, stores entitlement, issues signed JWT
6. Backend Route API — serves encrypted GPX to validated clients; checks license in DB before delivery
7. Backend Admin API — accepts GPX upload, encrypts server-side, stores to disk/S3, manages catalog
8. PostgreSQL (license records, route metadata, users) + Redis (license cache TTL, session grace period tracking)
9. Next.js Admin Dashboard — GPX upload form, route CRUD, license overview

The Android app uses product flavors (`motorcycle/` source set) to support a future sport car variant: all core logic in `:core`, branding in `:feature-motorcycle` and `:feature-sportscar`. The backend and dashboard are shared across variants.

### Critical Pitfalls

1. **Client-side-only license checks** — trivially bypassed via APK patching or Frida hooks; server validation must be in place before any beta distribution; the grace period extends active sessions but never permits starting new navigation without a fresh server check.

2. **GPX decrypted to a temp file at any point** — plaintext in `getCacheDir()` is extractable on rooted devices; decrypt to a `ByteArray` in memory only, pass directly to the OSMDroid overlay renderer, let it go out of scope and be GC'd.

3. **OSMDroid tile cache on external storage** — the default path (`/sdcard/osmdroid/`) breaks silently on Android 13+ with scoped storage; explicitly set tile cache to `context.getCacheDir()` in app startup before any map code runs; test on a physical Android 13+ device.

4. **`linkedPurchaseToken` not tracked** — on subscription renewals and upgrades, Google issues a new purchase token linked to the old one; failing to mark the old token as revoked creates zombie tokens that grant free access; this must be in the server DB schema from day one.

5. **Hard network requirement blocking mid-ride navigation** — offroad environments have no cell signal; license re-validation must never block an active navigation session; validate at session start, issue a session JWT with a 1-hour TTL, extend silently if network is unavailable during a running session.

6. **StrongBox used for bulk GPX decryption** — StrongBox crypto is slow (60+ seconds for files over 1MB); use StrongBox only to wrap/unwrap a session AES key; perform bulk decryption in software using the unwrapped key.

7. **`androidx.security-crypto` still in use** — fully deprecated Jul 2025; any new code using EncryptedFile from this library will not receive security patches; migrate to Google Tink 1.20.0 directly.

---

## Implications for Roadmap

Research reveals a hard dependency chain that dictates build order. The Android app cannot be meaningfully developed until the backend API exists. Routes cannot be purchased until they exist in the catalog. The catalog cannot be populated without the admin dashboard. Encryption must be validated in isolation before billing complexity is layered on top. The suggested phase structure mirrors the architecture's "Suggested Build Order" section with security verification gates added at critical transitions.

### Phase 1: Backend Foundation
**Rationale:** Every other component depends on the backend API. The Android app needs real endpoints to develop against; the admin dashboard needs upload APIs; the billing flow needs a license validation endpoint. Build this first to unblock everything else.
**Delivers:** PostgreSQL schema with license, route, user, and purchase token tables (including `revoked_at` and `linked_purchase_token` columns from day one); Fastify REST API with device JWT auth and admin JWT auth; `GET /routes`, `GET /routes/:id`, `GET /routes/:id/gpx`; `POST /admin/routes` with GPX upload and server-side AES-256-GCM encryption; Redis connection for license cache.
**Addresses:** Route catalog (backend side), admin route management, server-side GPX encryption
**Avoids:** Billing zombie token pitfall (schema must include linked token tracking from the start), service account key exposure (keep in server env vars, never in Android project)

### Phase 2: Admin Dashboard
**Rationale:** Routes must exist in the system before the app can display or purchase them. Admin tooling unblocks all further Android testing with real data.
**Delivers:** Next.js 15 dashboard with Auth.js; GPX upload form (metadata + file); route CRUD; preview of uploaded GPX on a map before publishing; basic license/purchase reporting.
**Uses:** Next.js 15.5, Shadcn/ui, Tailwind 4, Auth.js 5, Prisma adapter
**Implements:** Admin API (Backend Phase 1) consumed via Server Actions
**Avoids:** Empty catalog blocking Android development; GPX encrypted client-side in browser (encryption must happen server-side, not in the browser upload handler)

### Phase 3: Android — Map and Catalog
**Rationale:** Validate OSMDroid tile rendering and GPX overlay before adding encryption and billing complexity. Isolate rendering bugs from security bugs. OSMDroid's archived status and tile cache pitfall must be resolved here.
**Delivers:** OSMDroid integration with tile loading from app-private cache; Retrofit API client with RouteRepository; route catalog screen (list + detail with preview map); GPX overlay rendering from network response (unencrypted at this stage for development speed); Room DB for catalog caching; basic account screen (login/register).
**Uses:** OSMDroid 6.1.20, Retrofit 2.11, Room 2.8.4, Hilt 2.53, Jetpack Compose 1.10, kotlinx.coroutines 1.9
**Avoids:** OSMDroid tile cache on external storage (set `Configuration.getInstance().setOsmdroidTileCache()` to `getCacheDir()` in this phase before any other map code); default zoom level pitfall (set offroad-appropriate zoom showing 2-5km of route)
**Research flag:** Needs phase research — OSMDroid overlay performance with high point-count GPX routes; Douglas-Peucker simplification implementation

### Phase 4: Android — Encryption Layer
**Rationale:** Layer encryption onto a working rendering pipeline. Debugging is far simpler when map rendering is already validated. This phase establishes the security boundary that protects route content.
**Delivers:** Tink AES-256-GCM Streaming AEAD integration with Android Keystore; EncryptedFileStore service (write/read encrypted GPX blobs in `getFilesDir()`); DecryptRouteUseCase (decrypt to ByteArray → pass to OSMDroid overlay → discard); offline route download flow; Room tracking of "available offline" status; offline map tile pre-caching for route bounding box.
**Uses:** Google Tink 1.20.0, Android Keystore, android-gpx-parser 2.3.0
**Avoids:** Writing decrypted GPX to any file path; keeping decryption key in a static field or long-lived ViewModel; StrongBox for bulk decryption (wrap/unwrap pattern required); external storage permissions in AndroidManifest

### Phase 5: Billing and License Enforcement
**Rationale:** Billing requires a published app in Play Console for end-to-end testing; build all other functionality first so billing bugs do not block unrelated development. This is the most security-critical phase.
**Delivers:** Play Billing Library 8.3.0 integration (BillingRepository with `queryProductDetailsAsync`, `launchBillingFlow`, `queryPurchasesAsync`); backend `POST /license/validate` calling Google Play Developer API with `linkedPurchaseToken` invalidation; LicenseRepository with Room cache and TTL; ValidateLicenseUseCase with grace period logic (extend active session, block new session start); Real-Time Developer Notifications via Cloud Pub/Sub; navigation gate (blocks start on expired license, shows non-blocking expiry banner during active session).
**Uses:** Play Billing Library 8.3.0, `googleapis` npm 144.x, Redis 7 for session tracking, `@google-cloud/pubsub`
**Avoids:** Client-side-only license checks; hard network requirement blocking active navigation; device clock trusted for expiry (use server-issued `iat` claim); `PENDING` purchase state granting access before `PURCHASED` + server-verified
**Research flag:** Needs phase research — Play Console setup for test purchases, license testing environment configuration, RTDN Pub/Sub setup on Google Cloud

### Phase 6: Navigation UX Polish
**Rationale:** With core functionality complete, refine the navigation experience for the target use case (motorcycle, offroad, no hands on phone).
**Delivers:** Course-up (heading-up) map rotation with toggle; floating re-center button; "on route" vs "off route" visual indicator (color change when GPS deviates >50m from route line); license expiry TTL prominently displayed on route detail and during navigation; offline map tile pre-download prompt immediately after purchase; basic ride stats HUD (speed, distance covered, distance remaining, elapsed time) if validated by user feedback.
**Avoids:** North-up-only map (cognitive load on moving motorcycle); blocking dialog for license re-validation during active ride; license expiry only shown in settings screen

### Phase 7: Product Flavor and Security Hardening
**Rationale:** Done last because it only touches branding and release configuration. Core logic is already proven before flavors are wired in.
**Delivers:** `motorcycle/` product flavor source set with branding, strings, and icons; R8 minification enabled unconditionally in release builds with `@Keep` annotations for serialization models; Play Integrity API integration (verify device integrity at registration and before issuing content keys); HTTPS certificate pinning; ProGuard rules stripping `Log.*` from release builds; smoke test of build variants.
**Avoids:** R8 disabled in release (decompilable APK exposes decryption logic); SafetyNet API (deprecated May 2025 — use Play Integrity API instead); device clock for token expiry validation

### Phase Ordering Rationale

- Backend first because Android and Dashboard both depend on live APIs; building against mocks creates integration surprises
- Dashboard before Android navigation because the catalog must have real routes for meaningful map/purchase testing
- Map rendering before encryption because isolating rendering issues from cryptographic issues saves significant debugging time
- Encryption before billing because the route download and offline storage pipeline must be solid before purchase flow is wired to unlock it
- Billing last among core phases because it requires Play Console configuration and cannot be fully tested until the app is in review
- Polish and hardening last because they add no new dependencies and benefit from all prior code being stable

### Research Flags

Phases needing deeper research during planning:
- **Phase 3 (Android Map and Catalog):** OSMDroid GPX overlay performance at high point counts; Douglas-Peucker simplification integration with OSMBonusPack; offline tile pre-caching API surface
- **Phase 5 (Billing and License Enforcement):** Play Console internal test track setup; license testing accounts; RTDN Pub/Sub configuration on Google Cloud; `linkedPurchaseToken` lifecycle for one-time products (vs. subscriptions — the docs focus on subscriptions but behavior differs for one-time rentals)

Phases with standard patterns (can skip dedicated research-phase):
- **Phase 1 (Backend Foundation):** Fastify + Prisma + PostgreSQL + Redis patterns are well-documented with official guides
- **Phase 2 (Admin Dashboard):** Next.js 15 + Shadcn/ui is a standard 2025 admin stack with abundant examples
- **Phase 4 (Encryption Layer):** Tink AES-GCM Streaming AEAD pattern is documented in official Google Tink docs; Android Keystore key management is straightforward
- **Phase 7 (Flavor and Hardening):** Android product flavor setup and R8 configuration are well-documented in official Android build docs

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Majority of choices verified against official Android Developers, JetBrains, Vercel, and Google blogs. Two exceptions: SQLCipher version from Maven search results (MEDIUM); Fastify throughput advantage from community benchmarks (MEDIUM). Critical deprecation warnings (security-crypto, old SQLCipher artifact, Billing v4 APIs) are all confirmed against official sources. |
| Features | MEDIUM-HIGH | Table stakes and anti-features are well-grounded in competitor analysis and official Play Billing docs. Competitor feature analysis is based on public listings, not internal testing. The per-route licensing + encrypted content combination as a market differentiator is an inference from research, not confirmed by market data. |
| Architecture | HIGH | Core patterns (Clean Architecture, server-side license validation, in-memory decryption, product flavors) are all backed by official Android architecture guides and Google Play billing integration docs. MapLibre vs OSMDroid recommendation in ARCHITECTURE.md conflicts with project constraint in STACK.md — STACK.md correctly defers to the project constraint (OSMDroid 6.1.20). |
| Pitfalls | HIGH | Security pitfalls (key storage, client-side validation, linked purchase tokens) are backed by Android security docs, academic papers, and Google Play billing security guides. Performance traps (Douglas-Peucker, StrongBox bulk decrypt) are confirmed by OSMDroid GitHub issues and Android Keystore docs. |

**Overall confidence:** MEDIUM-HIGH

### Gaps to Address

- **ARCHITECTURE.md vs. STACK.md map library conflict:** ARCHITECTURE.md recommends MapLibre Native Android as the "maintained successor" to OSMDroid; STACK.md correctly identifies OSMDroid 6.1.20 as a project constraint. Resolution: use OSMDroid 6.1.20 as specified. If OSMDroid's archived status causes compatibility issues post-launch (new Android APIs breaking tile rendering), MapLibre is the documented migration path. Flag this in Phase 3 planning.
- **Tink minSdk boundary:** Tink 1.20.0 requires API 24; Play Billing 8.3.0 requires API 23; STACK.md resolves this by recommending `minSdkVersion = 24`. Confirm this is acceptable to the project owner before committing to it — the difference (Android 6 vs Android 7) covers less than 0.5% of devices.
- **SQLCipher necessity for v1:** STACK.md notes that if sensitive data is stored only as Tink-encrypted files (not in structured DB columns), Room without SQLCipher may be sufficient for v1. Determine during Phase 1 DB schema design whether any sensitive structured data (e.g., decryption key references, user PII beyond email) will be stored in Room columns.
- **Elevation profile data source:** FEATURES.md notes elevation profiles depend on GPX Z-values being present in uploaded files. If the admin does not include altitude data in uploaded GPX files, this feature requires an external elevation API (Open-Elevation or SRTM). Confirm with the project owner whether GPX files will include Z-values.
- **Play Integrity API integration timing:** PITFALLS.md recommends Play Integrity verification at registration and before issuing content keys. This requires a backend call and adds latency to the purchase flow. Decide during Phase 5 planning whether to include Play Integrity in v1 or defer to v1.x.

---

## Sources

### Primary (HIGH confidence)
- [Android Guide to App Architecture](https://developer.android.com/topic/architecture) — Clean Architecture layers, ViewModel/UseCase patterns
- [Google Play Billing release notes](https://developer.android.com/google/play/billing/release-notes) — v8.3.0 stable Dec 2025, v7 mandatory Aug 2025
- [Google Play Billing — One-time products with rental type](https://developer.android.com/google/play/billing/one-time-products) — day pass / rental / permanent product types
- [androidx.security Jetpack releases](https://developer.android.com/jetpack/androidx/releases/security) — security-crypto deprecated Jul 2025
- [Google Tink encrypt data docs](https://developers.google.com/tink/encrypt-data) — AES-GCM Streaming AEAD pattern
- [tink-crypto/tink-java GitHub releases](https://github.com/tink-crypto/tink-java/releases) — v1.20.0 confirmed
- [Android Keystore system](https://developer.android.com/privacy-and-security/keystore) — key storage and TEE backing
- [AGP 9.0.1 release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes) — Gradle 9.1+ requirement
- [Kotlin 2.3.0 release blog](https://blog.jetbrains.com/kotlin/2025/12/kotlin-2-3-0-released/) — K2 compiler default
- [Google Play target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878) — API 35 mandatory Aug 2025
- [Next.js 15.5 release](https://nextjs.org/blog/next-15-5) — stable Aug 2025
- [Jetpack Compose December '25 release](https://android-developers.googleblog.com/2025/12/whats-new-in-jetpack-compose-december-25.html) — Compose 1.10 stable
- [Integrate Google Play with your server backend](https://developer.android.com/google/play/billing/backend) — server-side purchase verification pattern
- [Play Integrity API overview](https://developer.android.com/google/play/integrity/overview) — device integrity verification
- [Implementing linkedPurchaseToken correctly](https://medium.com/androiddevelopers/implementing-linkedpurchasetoken-correctly-to-prevent-duplicate-subscriptions-82dfbf7167da) — Android Developers blog
- [osmdroid GitHub (archived Nov 2024)](https://github.com/osmdroid/osmdroid) — 6.1.20 final release confirmed
- [Android product flavors for white-labeling](https://developer.android.com/build/build-variants) — motorcycle/sportscar flavor strategy

### Secondary (MEDIUM confidence)
- [Fastify vs Express 2025 comparison](https://medium.com/codetodeploy/express-or-fastify-in-2025-whats-the-right-node-js-framework-for-you-6ea247141a86) — throughput benchmarks
- [MapLibre Native Android](https://maplibre.org/maplibre-native/android/api/) — OSMDroid migration path
- [Server-side purchase validation — Adapty](https://adapty.io/blog/android-in-app-purchases-server-side-validation/) — cross-referenced with official docs
- [REVER Motorcycle App Play Store listing](https://play.google.com/store/apps/details?id=com.reverllc.rever) — competitor feature analysis
- [onX Offroad offline maps feature](https://www.onxmaps.com/offroad/app/features/offline-maps) — competitor feature analysis
- [Adventure Cycling digital routes on Ride with GPS](https://www.adventurecycling.org/member_news/rolling-out-adventure-cycling-digital-routes-on-ride-with-gps/) — licensed route content model analog
- [OSMDroid Tile Caching — GitHub Wiki](https://github.com/osmdroid/osmdroid/wiki/Tile-Caching) — tile cache path configuration
- [OSMBonusPack DouglasPeuckerReducer](https://github.com/MKergall/osmbonuspack) — GPX point reduction for performance
- [Handling edge cases in Google Play Billing — RevenueCat](https://www.revenuecat.com/blog/engineering/google-play-edge-cases/) — PENDING state and edge cases
- [SQLCipher 4.x Zetetic blog](https://www.zetetic.net/blog/2025/03/25/sqlcipher-4.7.0-release/) — new `sqlcipher-android` artifact

### Tertiary (LOW confidence)
- [Riders Share — Best motorcycle route app comparison](https://www.riders-share.com/blog/article/is-there-an-app-for-motorcycle-routes) — competitor landscape, editorial
- [onX Offroad — 7 Essential Off-Road App Features](https://www.onxmaps.com/offroad/blog/essential-off-road-app-features) — feature expectations, marketing source

---
*Research completed: 2026-03-14*
*Ready for roadmap: yes*
