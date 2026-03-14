# Stack Research

**Domain:** Android navigation app with GPX route licensing and DRM
**Researched:** 2026-03-14
**Confidence:** MEDIUM-HIGH (most choices verified against official sources; see per-item notes)

---

## Recommended Stack

### Android App — Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Kotlin | 2.3.0 | Primary language | Current stable (Dec 2025). AGP 9.0 has built-in Kotlin support; no separate plugin needed. K2 compiler default improves build speed. |
| Android Gradle Plugin (AGP) | 9.0.1 | Build system | Latest stable (Jan 2026). Requires Gradle 9.1+, JDK 17. Built-in Kotlin support removes boilerplate. |
| targetSdk | 35 (Android 15) | Play Store compliance | Mandatory for all new app submissions from Aug 31 2025. Apps targeting API 33 or lower are invisible to new users on Android 14+ devices. |
| minSdk | 23 (Android 6) | Minimum supported OS | Play Billing Library 8.1+ enforces minSdkVersion 23. Covers 99%+ of active Android devices. |
| Jetpack Compose | 1.10 (BOM 2025.12) | UI framework | Stable Dec 2025. Declarative UI eliminates XML layout complexity. Material 3 v1.4 included. Official Google direction for new Android UI. |
| Navigation 2.9.7 | 2.9.7 | Screen navigation | Latest stable (Jan 2026). Standard Compose navigation. Navigation3 exists (stable Nov 2025) but is additive; Nav2 remains the safe default for straightforward single-app navigation. |
| OSMDroid | 6.1.20 | Map rendering + GPX route overlay | Project constraint (OSMDroid specified). Version 6.1.20 is the **final release** — project archived Nov 2024. Fully functional for offline tile rendering and Polyline overlays; no further patches will come. Accept this as a fixed dependency. |
| Google Play Billing Library | 8.3.0 | In-app purchases (day pass, rental, permanent) | Latest stable (Dec 2025). Mandatory for new apps Aug 2025. Older versions (< 7) are deprecated. v8 adds automatic service reconnection, multi-purchase options, and removes obsolete `querySkuDetailsAsync()`. |

### Android App — Security & Data

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Google Tink (tink-android) | 1.20.0 | AES-256-GCM encryption of GPX data | The `androidx.security:security-crypto` library (EncryptedFile) was **fully deprecated** in v1.1.0 stable (Jul 2025). Google's official replacement recommendation is Tink directly. Tink provides AES-GCM Streaming AEAD — ideal for encrypting large GPX files in chunks. Backed by AndroidKeyStore for key storage. |
| Android Keystore | Platform API | Cryptographic key storage | Hardware-backed key storage where available. Keys never leave the secure enclave. Used by Tink to store the master key. No library version — built into the OS. |
| Room | 2.8.4 | Local database (license records, route metadata) | Latest stable (Nov 2025). Standard Jetpack persistence layer. Kotlin-first in 2.8.x. |
| SQLCipher for Android (`sqlcipher-android`) | 4.13.0 (Community) | Encrypted SQLite for Room | Needed if storing any structured sensitive data in the DB. The old `android-database-sqlcipher` artifact is deprecated; use `net.zetetic:sqlcipher-android`. Note: if only GPX blobs are sensitive and stored as encrypted files (via Tink), Room without SQLCipher may be sufficient for v1. |
| DataStore (Preferences) | 1.1.x | App preferences (session tokens, expiry timestamps) | Replaces SharedPreferences. No encryption needed for non-sensitive prefs; pair with AndroidKeyStore directly for any sensitive preference values now that security-crypto is deprecated. |

### Android App — Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Retrofit | 2.11.0 | HTTP client for backend API calls (license validation, catalog fetch) | Standard Android HTTP client. Pair with kotlinx.serialization or Gson. Used for all calls to the companion backend. |
| OkHttp | 4.12.0 | Underlying HTTP engine for Retrofit | Provides connection pooling, interceptors for auth headers. Required alongside Retrofit. |
| Hilt | 2.53 | Dependency injection | Standard Jetpack DI. Reduces manual wiring of ViewModel, Repository, and encryption service layers. |
| Coil | 3.x | Route thumbnail / preview image loading | Compose-native image loader. Lighter than Glide for Compose-first apps. |
| kotlinx.coroutines | 1.9.x | Async operations (map rendering, decryption, network) | Essential for non-blocking UI. Room, Retrofit, and Play Billing all have coroutine-native APIs. |
| kotlinx.serialization | 1.7.x | JSON serialization for API responses | Kotlin-native, tree-shaking friendly, no reflection. Preferred over Gson for Kotlin codebases. |
| GPSUtil / io.ticofab.android-gpx-parser | 2.3.0 | GPX file parsing | Parse GPX XML into track points before encryption or before rendering. Lightweight, no dependencies. Decrypt → parse in memory → render → discard. Never write parsed data to disk. |

### Backend — Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Node.js | 22 LTS | Runtime | Current LTS (ends Apr 2027). Stable event loop performance. TypeScript support via `tsx` or `ts-node`. |
| Fastify | 5.x | REST API framework | 2025 greenfield recommendation over Express. 2-3x higher throughput in benchmarks, TypeScript-first, built-in schema validation via JSON Schema / Zod. No legacy baggage. |
| TypeScript | 5.x | Language | Type safety across backend and admin dashboard. Shared types possible. |
| Prisma | 6.x | ORM | Standard TypeScript ORM for Node.js. Type-safe queries, automated migrations, clean schema syntax. Supports PostgreSQL (recommended DB). |
| PostgreSQL | 17 | Primary database | License records, user accounts, route catalog metadata, purchase tokens. Robust JSONB for flexible route metadata. Battle-tested for transactional license records. |
| Google Play Developer API (REST) | v3 | Server-side purchase verification | Official API for verifying `purchaseToken` against Google's servers. Use `googleapis` Node.js client. This is the only tamper-proof verification path — client-side cannot be trusted. |
| `googleapis` npm | 144.x | Google Play Developer API client | Official Google client library for Node.js. Handles OAuth2 service account auth against the Android Publisher API. |
| Redis | 7.x | License cache, session invalidation, grace period tracking | Avoids hammering Google's API on every navigation frame check. Cache validated licenses with a TTL. Required for the 1-hour grace period logic during active sessions. |

### Backend — Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Zod | 3.x | Request/response schema validation | Validate incoming purchase tokens and webhook payloads before processing. |
| jsonwebtoken / jose | 5.x | JWT for app-to-backend auth | Issue short-lived tokens to authenticated Android sessions. Signed, not encrypted — claims validated server-side. |
| `@google-cloud/pubsub` | latest | Real-Time Developer Notifications (RTDN) from Google Play | Required if you want push-based subscription expiry events instead of polling. Google Play sends Pub/Sub messages on subscription state changes. |
| bcrypt / argon2 | latest | Password hashing (admin accounts) | argon2 preferred for new projects (memory-hard). bcrypt is acceptable fallback. |

### Web Dashboard — Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Next.js | 15.5 | Admin dashboard framework | Stable Aug 2025. Server Actions eliminate boilerplate API routes for admin CRUD. App Router provides clean server/client split. SSR not critical for admin panels but SSG for static pages reduces latency. |
| React | 19 | UI library | Bundled with Next.js 15.5. |
| TypeScript | 5.x | Language | Shared types with backend reduces contract bugs. |
| Tailwind CSS | 4.x | Styling | Zero-runtime CSS-in-JS alternative. Utility-first, fast to iterate admin UIs. Next.js has native Tailwind integration. |
| Shadcn/ui | latest | Component library | Unstyled accessible components built on Radix UI + Tailwind. Copy-paste ownership model — no version lock-in. Standard 2025 admin dashboard building block. |
| NextAuth.js (Auth.js) | 5.x | Admin authentication | Credential + OAuth provider support. Protects `/admin` routes with session middleware. Integrates with Prisma adapter. |

---

## Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Android Studio Ladybug (2024.2) or newer | Android IDE | Required for AGP 9.x and Kotlin 2.3 tooling support. |
| Gradle 9.1+ | Build system | Required by AGP 9.0. Use Gradle wrapper. |
| Docker Compose | Local dev environment | Run PostgreSQL + Redis locally without cloud dependency during development. |
| Postman / Bruno | API testing | Test license validation endpoints before wiring to Android. |
| Android Emulator / Physical device | Testing | OSMDroid tile rendering requires network or pre-cached tiles; test on physical device for map UX fidelity. |

---

## Gradle/Maven Coordinates (Android)

```kotlin
// build.gradle.kts (app module)

// Maps
implementation("org.osmdroid:osmdroid-android:6.1.20")

// Billing
implementation("com.android.billingclient:billing:8.3.0")
implementation("com.android.billingclient:billing-ktx:8.3.0")

// Encryption
implementation("com.google.crypto.tink:tink-android:1.20.0")

// Database
implementation("androidx.room:room-runtime:2.8.4")
ksp("androidx.room:room-compiler:2.8.4")
// Only if encrypted DB needed (see notes):
implementation("net.zetetic:sqlcipher-android:4.13.0@aar")

// Networking
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

// DI
implementation("com.google.dagger:hilt-android:2.53")
ksp("com.google.dagger:hilt-android-compiler:2.53")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

// GPX parsing
implementation("io.ticofab.github:android-gpx-parser:2.3.0")
```

## NPM Packages (Backend)

```bash
# Core
npm install fastify @fastify/cors @fastify/jwt prisma @prisma/client
npm install googleapis zod redis jsonwebtoken

# Dev
npm install -D typescript tsx @types/node prisma
```

## NPM Packages (Web Dashboard)

```bash
npx create-next-app@latest admin-dashboard --typescript --tailwind --app
npm install next-auth@beta @auth/prisma-adapter
# shadcn/ui components added per-component via CLI
npx shadcn@latest init
```

---

## Alternatives Considered

| Recommended | Alternative | Why Not / When to Use Alternative |
|-------------|-------------|-----------------------------------|
| OSMDroid 6.1.20 | Mapbox Android SDK | Mapbox requires API key + paid tier at scale. OSMDroid is a project constraint; use as specified. If the project pivots away from the constraint, Mapbox is the production-quality alternative for offline routing. |
| OSMDroid 6.1.20 | Organic Maps SDK | Newer open-source option based on MAPS.ME, actively maintained. Viable if OSMDroid's archived status becomes a security or compatibility concern post-launch. |
| Google Tink 1.20.0 | androidx.security-crypto (EncryptedFile) | Deprecated as of Jul 2025. Do not use in new projects. |
| Google Tink 1.20.0 | Manual AES/javax.crypto | Tink wraps javax.crypto with safe defaults and avoids common misuse (IV reuse, wrong mode). Always prefer a well-reviewed crypto library over manual primitives. |
| Play Billing 8.3.0 | RevenueCat SDK | RevenueCat adds a vendor dependency and 1-2% revenue cut. Acceptable for teams that want subscription lifecycle management handled externally. For v1 single-seller model, native billing is simpler. |
| Fastify | Express | Express is fine but effectively in maintenance mode. For a greenfield project, Fastify's TypeScript-first design and significantly higher throughput are clear wins. |
| Fastify | NestJS | NestJS adds significant boilerplate/overhead for a lean backend. Justified for large teams with complex module separation; overkill for a single-seller license validation service. |
| PostgreSQL | Firebase Firestore | Firestore works well for simple document reads but poorly for transactional license records with expiry logic and relational user/route/license joins. PostgreSQL is clearly better for this data model. |
| Next.js 15 (admin) | Plain React SPA | Next.js Server Actions simplify admin form submissions (GPX upload, route management) without writing separate API endpoints. The added SSR capability is free. |
| Shadcn/ui | Material UI (MUI) | MUI has its own design language that clashes with Tailwind. Shadcn/ui is Tailwind-native with no runtime CSS overhead and no vendor lock-in. |

---

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| `androidx.security:security-crypto` (EncryptedFile, EncryptedSharedPreferences) | Fully deprecated Jul 2025. Google explicitly abandoned it. Will not receive security patches. | Google Tink 1.20.0 + AndroidKeyStore directly |
| `net.zetetic:android-database-sqlcipher` (old artifact) | Deprecated in favor of the new `sqlcipher-android` artifact. | `net.zetetic:sqlcipher-android:4.13.0` |
| OSMDroid versions < 6.1.20 | Known bugs fixed in 6.1.20. This is also the final release; no point using older builds. | 6.1.20 only |
| `querySkuDetailsAsync()` / `queryPurchaseHistoryAsync()` (Billing) | Removed in Billing Library 8.0.0. Code using these will not compile against 8.x. | `queryProductDetailsAsync()` / `queryPurchasesAsync()` |
| Client-side-only license validation | Trivially bypassable via APK patching or memory editing. Core business requirement is server-side enforcement. | Always validate purchase tokens against Google Play Developer API from the backend before granting route access. |
| Storing decrypted GPX on disk (even temporarily) | Any file in external storage or cache dirs can be extracted on rooted devices or via ADB. | Decrypt GPX to in-memory byte array, pass directly to OSMDroid overlay renderer, discard after use. Never write plaintext GPX to any file path. |
| Hardcoded encryption keys in APK | Trivially extracted via `apktool` or string analysis. | Generate AES key on first launch, store in AndroidKeyStore. Key never leaves the hardware-backed keystore. |
| `SharedPreferences` for license tokens | Plain XML stored in `/data/data/`, readable on rooted devices. | Encrypted DataStore with AndroidKeyStore-backed key, or store tokens only server-side and re-validate on session. |

---

## Stack Patterns by Variant

**For the GPX encryption + in-memory render pipeline:**
- Decrypt with Tink (AES-256-GCM Streaming AEAD) → parse with android-gpx-parser → build `Polyline` overlay → add to OSMDroid `MapView` → discard byte arrays after render. Never write decrypted bytes to a `File`.

**For license validation flow:**
- App purchases via Play Billing → receives `purchaseToken` → sends token + account ID to backend → backend calls Google Play Developer API v3 (`purchases.products.get` or `purchases.subscriptions.get`) → backend stores validated license record in PostgreSQL with expiry → returns signed JWT to app → app caches JWT in DataStore → on navigation start, app sends JWT to backend `/validate` endpoint → backend checks Redis cache → grants or denies.

**For the 1-hour grace period:**
- Store `session_start_time` and `license_expiry` in Redis when navigation starts. Backend `/validate` checks: if `now < expiry + 1h AND session_started_before_expiry`, allow. Clear session record when user stops navigation.

**For the future sport car app fork:**
- Keep all business logic (billing, encryption, license) in a `:core` Gradle module. Keep UI (map overlays, route card design) in `:feature-motorcycle` and `:feature-sportscar` modules. The backend and dashboard are shared; only the Android app skin changes.

---

## Version Compatibility Notes

| Component A | Compatible With | Notes |
|-------------|-----------------|-------|
| AGP 9.0.1 | Gradle 9.1+, JDK 17, Kotlin 2.3.x | AGP 9 requires JDK 17 minimum. AGP 8.x is compatible with JDK 11. |
| Play Billing 8.3.0 | minSdk 23+ | Library 8.1 raised minSdk to 23. If you need minSdk 21, stay on 8.0.x (not recommended). |
| Tink-android 1.20.0 | Android API 24+ | Tink dropped support for API 21-22 in v1.18.0 (Jun 2024). With minSdk 23, you are at the boundary — Tink requires API 24. Set minSdk to 24 or use Tink 1.17.x for API 23 support. **Recommendation: set minSdk to 24** — less than 0.5% of devices run API 23. |
| Room 2.8.4 | Kotlin 2.0+ | Room 2.8.x is Kotlin-first and requires Kotlin 2.0 minimum. |
| OSMDroid 6.1.20 | minSdk 17+ | No compatibility issues at minSdk 24. |
| SQLCipher-android 4.13.0 | minSdk 21+ | No issues at minSdk 24. |

**Revised minSdk recommendation:** Set `minSdkVersion = 24` (not 23) to satisfy Tink 1.20.0 while staying within Play Billing 8.x requirements. Android 7.0+ covers 99%+ of active devices.

---

## Sources

- [osmdroid GitHub (archived Nov 2024)](https://github.com/osmdroid/osmdroid) — confirmed archived status, 6.1.20 final release — HIGH confidence
- [javadoc.io osmdroid-android 6.1.20](https://javadoc.io/doc/org.osmdroid/osmdroid-android/latest/index.html) — current API surface — HIGH confidence
- [Google Play Billing release notes](https://developer.android.com/google/play/billing/release-notes) — v8.3.0 Dec 2025, mandatory deadlines — HIGH confidence (official Android docs)
- [androidx.security Jetpack releases](https://developer.android.com/jetpack/androidx/releases/security) — security-crypto deprecated v1.1.0 Jul 2025 — HIGH confidence (official Android docs)
- [tink-crypto/tink-java GitHub releases](https://github.com/tink-crypto/tink-java/releases) — v1.20.0 Dec 2024, tink-android same version — HIGH confidence (official repo)
- [Google Tink encrypt data docs](https://developers.google.com/tink/encrypt-data) — AES-GCM streaming AEAD pattern — HIGH confidence
- [Room Jetpack releases](https://developer.android.com/jetpack/androidx/releases/room) — v2.8.4 Nov 2025 stable — HIGH confidence
- [SQLCipher 4.7.0 release / Zetetic](https://www.zetetic.net/blog/2025/03/25/sqlcipher-4.7.0-release/) — 4.13.0 community latest, new `sqlcipher-android` artifact — MEDIUM confidence (official Zetetic blog, version scraped from Maven search results)
- [Kotlin 2.3.0 release blog](https://blog.jetbrains.com/kotlin/2025/12/kotlin-2-3-0-released/) — Dec 2025 stable — HIGH confidence
- [AGP 9.0.1 release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes) — Jan 2026, Gradle 9.1+ required — HIGH confidence
- [Navigation Jetpack releases](https://developer.android.com/jetpack/androidx/releases/navigation) — 2.9.7 stable Jan 2026 — HIGH confidence
- [Google Play target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878) — API 35 mandatory Aug 2025 — HIGH confidence
- [googleapis npm / Google Play Developer API v3](https://developer.android.com/google/play/billing/backend) — server-side purchase verification pattern — HIGH confidence
- [Fastify vs Express 2025 comparison](https://medium.com/codetodeploy/express-or-fastify-in-2025-whats-the-right-node-js-framework-for-you-6ea247141a86) — throughput benchmarks — MEDIUM confidence (community source, consistent across multiple articles)
- [Next.js 15.5 release](https://nextjs.org/blog/next-15-5) — Aug 2025 stable — HIGH confidence (official Vercel blog)
- [Jetpack Compose December '25 release](https://android-developers.googleblog.com/2025/12/whats-new-in-jetpack-compose-december-25.html) — Compose 1.10 stable — HIGH confidence (official Android blog)

---

*Stack research for: Android GPX navigation app with route licensing/DRM (Roadrunner)*
*Researched: 2026-03-14*
