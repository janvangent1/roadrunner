---
phase: 1
title: Backend Foundation
status: ready
---

# Phase 1 Context: Backend Foundation

## Decisions

### Hosting
- **Platform:** Raspberry Pi (local), no cloud costs
- **Exposure:** Cloudflare Tunnel (free) to expose the Pi API publicly — required so the Android app can validate licenses while the rider is away from home WiFi
- **Runtime:** Docker Compose — one compose file with Fastify app, PostgreSQL, and Redis containers

### Stack (locked from research)
- **API framework:** Fastify (TypeScript)
- **Database:** PostgreSQL (via Docker)
- **Cache:** Redis (via Docker)
- **ORM:** Prisma
- **GPX encryption:** Google Tink (AES-256-GCM, server-side) — NOT Node.js built-in crypto

### Authentication
- **App users:** Email + password → JWT (access token short-lived, refresh token stored in DB)
- **Admins:** Same email/password flow but `role = ADMIN` on the user record — no separate admin auth system
- **Android Google Sign-In:** Android handles the OAuth dance; backend receives a Google ID token, verifies it with Google, creates/finds the user account, returns a JWT

### API Design
- Single unified API (`/api/v1/`) with role-based middleware
- Admin-only routes gated by `requireAdmin` middleware
- Public routes (catalog, route metadata) require auth but not admin

### Key endpoints Phase 1 must deliver:
```
POST /api/v1/auth/register          → email/password account creation
POST /api/v1/auth/login             → returns access + refresh JWT
POST /api/v1/auth/google            → Google ID token → JWT
POST /api/v1/auth/refresh           → rotate refresh token
POST /api/v1/auth/logout            → revoke refresh token

GET  /api/v1/routes                 → catalog list (metadata only, no GPX)
GET  /api/v1/routes/:id             → route detail + metadata + waypoints
GET  /api/v1/routes/:id/gpx         → encrypted GPX blob (requires valid license)

POST /api/v1/admin/routes           → upload GPX + metadata (admin only)
PATCH /api/v1/admin/routes/:id      → edit metadata, publish/unpublish
DELETE /api/v1/admin/routes/:id     → delete route

POST  /api/v1/admin/licenses        → grant license (email, routeId, type, expiry)
PATCH /api/v1/admin/licenses/:id    → modify license (extend, change type, revoke)
GET   /api/v1/admin/licenses        → list all grants (filter by route or user)

POST /api/v1/licenses/check         → validate license before navigation start (returns session token with server timestamp)
```

### Database Schema
Four tables, all with `created_at` / `updated_at`:

**users** — id, email, password_hash, google_id (nullable), role (USER/ADMIN), created_at, updated_at

**routes** — id, title, description, difficulty (EASY/MODERATE/HARD/EXPERT), terrain_type, region, estimated_duration_minutes, distance_km, published (bool), gpx_encrypted (bytes), created_at, updated_at

**waypoints** — id, route_id (FK), label, latitude, longitude, type (FUEL/WATER/CAUTION/INFO), sort_order, created_at

**licenses** — id, user_id (FK), route_id (FK), type (DAY_PASS/MULTI_DAY/PERMANENT), expires_at (nullable — NULL for permanent), revoked_at (nullable), linked_purchase_token (nullable — reserved for v2 Play Billing), created_at, updated_at

Index: `licenses(user_id, route_id)` for fast per-user license lookups.

### GPX Encryption (server-side)
- On upload: Tink `StreamingAead` (AES-256-GCM) encrypts GPX bytes with a keyset stored in an env var (base64 Tink JSON keyset)
- Encrypted bytes stored in `routes.gpx_encrypted`
- On download: server decrypts and re-encrypts with a per-session key sent alongside the blob, OR serves the raw encrypted blob and sends the decryption key separately via a signed endpoint — **decision: serve raw Tink-encrypted blob; client decrypts with the same server keyset distributed at app install time as a sealed Tink keyset (no plaintext key in APK)**
- Plaintext GPX never written to disk; encryption/decryption happens in memory

### License Check Flow
1. App calls `POST /licenses/check` with `{ routeId, deviceId }`
2. Server checks: license exists, not revoked, not expired (server clock — not device clock)
3. Returns: `{ valid: true, sessionToken: "<JWT>", expiresAt: "<ISO>" }` with 1-hour session window baked in

### Redis Usage
- Cache license validity: key `license:{userId}:{routeId}` → TTL 60s (so license revocation propagates within 1 min)
- Cache route catalog list: key `catalog` → TTL 5 min

## Deferred Ideas
- Cloudflare R2 or S3 storage for GPX files (current: stored as binary in PostgreSQL — fine for small catalog)
- Rate limiting on license check endpoint
- Automated backups of PostgreSQL data off-Pi
- v2: Google Play Billing integration (purchase token validation against Play Developer API)
