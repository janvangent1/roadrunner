# Roadrunner

A licensed GPS route platform for offroad motorcycle riding. Sell curated GPX routes on a day-pass, multi-day rental, or permanent license model — the route data is encrypted end-to-end and never accessible outside the app.

---

## What it does

Riders browse a catalog of premium offroad routes, purchase a license, and navigate in-app. The GPX track is stored on-device encrypted with AES-256-GCM and decrypted only in memory at render time — it cannot be extracted, exported, or shared.

**For riders:**
- Browse route catalog with difficulty, terrain type, distance, and preview map
- Purchase day passes, multi-day rentals, or permanent licenses (manual grant by admin in v1)
- Navigate with a full-screen OSMDroid map, real-time GPS dot, and ride stats HUD
- Waypoints (fuel stops, water crossings, caution zones) shown as map pins
- Off-route indicator when you stray more than 50 m from the line
- Map tiles pre-cached for fully offline navigation — works with no cell signal

**For the route seller (admin):**
- Web dashboard to upload GPX files with full metadata
- Annotate routes with waypoints and POIs
- Grant, revoke, and manage licenses per user and route
- Publish / unpublish routes from the catalog

---

## Architecture

```
┌──────────────────────┐     HTTPS (Cloudflare Tunnel)    ┌──────────────────────────┐
│   Android App        │ ◄───────────────────────────────► │   Raspberry Pi           │
│                      │                                   │                          │
│  Jetpack Compose     │                                   │  Fastify API  :4000      │
│  OSMDroid map        │                                   │  Next.js dash :4001      │
│  Tink AES-256-GCM    │                                   │  PostgreSQL   :5432      │
│  Play Integrity API  │                                   │  Redis        :6379      │
│  OkHttp cert pinning │                                   │                          │
└──────────────────────┘                                   └──────────────────────────┘
```

| Layer | Technology |
|-------|-----------|
| Android app | Kotlin, Jetpack Compose, Hilt, Retrofit, OSMDroid 6.1.20 |
| Encryption | Google Tink AES-256-GCM + Android Keystore (StreamingAead) |
| Backend API | Fastify (TypeScript), Prisma ORM, PostgreSQL 16, Redis 7 |
| Admin dashboard | Next.js 15 (App Router), Shadcn/ui, Tailwind CSS |
| Infrastructure | Docker Compose, Cloudflare Tunnel |

---

## Repository structure

```
roadrunner/
├── android/          # Android app (Kotlin + Jetpack Compose)
├── backend/          # Fastify REST API (TypeScript)
├── dashboard/        # Next.js admin dashboard
├── docker-compose.yml
├── SETUP.md          # Full deployment guide
└── README.md
```

---

## Features

### Route protection
- GPX files encrypted server-side before storage (Tink AES-256-GCM, AAD = routeId)
- Client stores only the encrypted blob at `filesDir/gpx/<routeId>.enc`
- Decryption in memory only — plaintext never touches the filesystem
- No export mechanism, no FileProvider, no share intent
- ProGuard strips all `Log.*` calls from release builds

### License enforcement
- Every navigation start triggers `POST /api/v1/licenses/check` — device clock is never trusted
- Server issues a short-lived session JWT with `expiresAt`
- Active session continues up to 1 hour after expiry (grace period); new sessions blocked
- License types: day pass, multi-day rental (with expiry), permanent

### Security hardening (release builds)
- R8 minification + resource shrinking
- OkHttp certificate pinning on all API traffic (Cloudflare Tunnel cert)
- Play Integrity API check on first launch — blocks rooted/tampered devices
- 24-hour integrity result cache (DataStore)

### Navigation
- Full-screen OSMDroid map with GPX polyline overlay
- `MyLocationNewOverlay` + `CompassOverlay` for course-up map rotation
- HUD: speed (km/h), distance covered, distance remaining, elapsed time
- Off-route detection: haversine point-to-segment, 50 m activate / 40 m clear hysteresis
- Waypoint pins with type icons (⛽ fuel, 💧 water, ⚠️ caution, ℹ️ info)
- Tile pre-caching via WorkManager (zoom levels 10–15, stored in `cacheDir`)

### Multi-flavor
- `motorcycle` flavor — fully branded as "Roadrunner Moto"
- `sportscar` flavor — defined in Gradle, no source set (proves extensibility for a future sport car variant)

---

## Getting started

See [SETUP.md](SETUP.md) for the full deployment guide. The short version:

```bash
# 1. Clone
git clone https://github.com/janvangent1/roadrunner.git
cd roadrunner

# 2. Configure backend
cp backend/.env.example backend/.env
# fill in JWT secrets, Tink keyset, Google OAuth client ID

# 3. Start backend + dashboard on your Pi
docker compose up -d

# 4. Fill in the Android placeholders in android/app/build.gradle.kts
#    - TINK_KEYSET_B64 (must match backend TINK_KEYSET_JSON, base64-encoded)
#    - CERT_PIN_SHA256 (release only — SHA-256 of your Cloudflare Tunnel cert)
#    - google_server_client_id in strings.xml
#    - play_integrity_cloud_project_number in strings.xml

# 5. Build debug APK
cd android && ./gradlew installMotorcycleDebug
```

### Requirements

| Requirement | Version |
|-------------|---------|
| JDK | 17+ (24 recommended — see `android/gradle.properties`) |
| Docker + Compose | Latest |
| Android Studio | Hedgehog+ |
| Android device | API 24+ (Android 7.0) |
| Raspberry Pi | 3B+ or newer (2 GB RAM minimum) |

---

## Port assignments

Designed to coexist with other services on the same Raspberry Pi (Domoticz on 8080, Regenboog on 3000/3001, TrackWise on 8000):

| Service | Host port |
|---------|-----------|
| Roadrunner API | 4000 |
| Roadrunner Dashboard | 4001 |
| PostgreSQL | 5432 (internal) |
| Redis | 6379 (internal) |

---

## Environment variables

`backend/.env` (copy from `.env.example`):

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | PostgreSQL connection string |
| `REDIS_URL` | Redis connection string |
| `JWT_SECRET` | Access token signing key (generate with `openssl rand -hex 32`) |
| `JWT_REFRESH_SECRET` | Refresh token signing key |
| `TINK_KEYSET_JSON` | Base64-encoded binary Tink AES-256-GCM keyset for GPX encryption |
| `GOOGLE_CLIENT_ID` | Google OAuth 2.0 Web Client ID |
| `PORT` | API listen port (default 4000) |
| `ANDROID_PACKAGE_NAME` | `com.roadrunner.app` — used by Play Integrity token decoding |

---

## License enforcement flow

```
Rider taps "Start Navigation"
        │
        ▼
POST /api/v1/licenses/check { routeId }
        │
   ┌────┴────┐
valid?        no
   │          │
   ▼          ▼
Store      Show "License expired / not found"
sessionToken    Block navigation
+ expiresAt
in memory
   │
   ▼
Navigation starts
   │
   ▼ (every 30s)
Check: now > expiresAt + 1h ?
   │
  yes ──► Show expiry dialog ──► popBackStack()
```

---

## v2 roadmap

- Google Play Billing (replace manual license grants)
- Per-route key delivery authenticated by session token (replace BuildConfig keyset)
- Elevation profile on route detail
- Push notifications for expiring rentals
- Sport car app variant (flavor scaffold already in place)

---

## Credits

Built with [OSMDroid](https://github.com/osmdroid/osmdroid) · [Google Tink](https://github.com/google/tink) · [Fastify](https://fastify.dev) · [Prisma](https://prisma.io) · [Shadcn/ui](https://ui.shadcn.com)
