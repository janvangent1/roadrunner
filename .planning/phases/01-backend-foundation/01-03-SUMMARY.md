---
phase: 01-backend-foundation
plan: 03
subsystem: api
tags: [fastify, prisma, tink, aes256gcm, redis, typescript, gpx, encryption, routes, multipart]

# Dependency graph
requires:
  - phase: 01-backend-foundation/01-01
    provides: Fastify scaffold with buildApp() factory, Prisma schema with Route/Waypoint/License models
  - phase: 01-backend-foundation/01-02
    provides: requireAuth/requireAdmin preHandler hooks, prisma singleton, redis singleton with redisGet/redisSet

provides:
  - Tink AES-256-GCM in-memory GPX encryption (initTink/encryptGpx/decryptGpx) via backend/src/lib/tink.ts
  - GET /api/v1/routes — published route catalog with Redis 5-minute cache (no GPX bytes)
  - GET /api/v1/routes/:id — route detail with waypoints (no GPX bytes)
  - GET /api/v1/routes/:id/gpx — encrypted GPX blob served to users with valid non-expired, non-revoked license
  - POST /api/v1/admin/routes — multipart GPX upload; encrypts in memory before storing; plaintext never on disk
  - PATCH /api/v1/admin/routes/:id — metadata update and/or GPX replacement with re-encryption
  - DELETE /api/v1/admin/routes/:id — route deletion with cascade waypoints; cache invalidated
affects: [01-04, 02-admin-api, 03-android-app, 04-gpx-pipeline, 05-licensing]

# Tech tracking
tech-stack:
  added:
    - tink-crypto ^0.1.1 (already in package.json; uses binaryInsecure.deserializeKeyset + KeysetHandle.getPrimitive(Aead))
  patterns:
    - Tink initialization: aead.register() + binaryInsecure.deserializeKeyset(base64-decoded binary keyset) called at app startup in server.ts
    - In-memory GPX encryption: streamToBuffer() drains multipart stream into Buffer — no temp files; encryptGpx(buffer, routeIdAad) then stored as Bytes in Prisma
    - Redis catalog caching: GET /routes checks 'catalog' key first (5min TTL); POST/PATCH/DELETE call redis.del('catalog')
    - Route ID used as Associated Authenticated Data (AAD) for encryption — decryption must use same routeId AAD
    - gpxEncrypted field always excluded from non-GPX route responses via Prisma select

key-files:
  created:
    - backend/src/lib/tink.ts
    - backend/src/routes/routes.ts
    - backend/src/routes/adminRoutes.ts
  modified:
    - backend/src/app.ts
    - backend/src/server.ts
    - backend/.env.example

key-decisions:
  - "tink-crypto actual API uses binaryInsecure.deserializeKeyset(Uint8Array) + keysetHandle.getPrimitive(Aead) — not aead.keysetHandle()/aead.primitive() as described in plan pseudocode; adapted to real API"
  - "TINK_KEYSET_JSON env var stores base64-encoded binary Tink keyset (not JSON); env var name kept as-is for backward compatibility with existing .env.example"
  - "Route ID used as AAD for Tink encryption — binds ciphertext to the specific route; same AAD required for decryption in Plan 04"
  - "POST /admin/routes generates route ID (uuid) before encryption so it can be used as AAD, then passes that ID to prisma.route.create"
  - "streamToBuffer() accumulates all multipart stream bytes in memory — never writes to disk; satisfies plaintext-never-on-disk invariant"

patterns-established:
  - "Route responses always use Prisma select to explicitly exclude gpxEncrypted — never rely on omit-by-default"
  - "Redis cache invalidation uses redis.del() directly (not redisSet with 0 TTL) for immediate eviction"
  - "Tink register() called before deserializeKeyset() — idempotent, safe to call multiple times"

requirements-completed: []

# Metrics
duration: 6min
completed: 2026-03-14
---

# Phase 1 Plan 03: Route Pipeline Summary

**Tink AES-256-GCM in-memory GPX encryption with full route CRUD API (public catalog + license-gated download + admin management) using Redis catalog caching**

## Performance

- **Duration:** 6 min
- **Started:** 2026-03-14T21:53:32Z
- **Completed:** 2026-03-14T21:59:00Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- Tink encryption helper using real tink-crypto binary API: `initTink()` loads keyset from base64-encoded binary env var; `encryptGpx()`/`decryptGpx()` operate entirely in memory with AAD support
- Three public route endpoints with Redis 5-minute catalog cache; GPX endpoint enforces license check server-side (device clock never trusted)
- Three admin endpoints: POST (multipart GPX upload, stream-to-buffer, encrypt-before-store), PATCH (metadata and/or GPX replacement), DELETE (cascade, cache invalidated)

## Task Commits

Each task was committed atomically:

1. **Task 1: Tink GPX encryption helper** - `3d3da3d` (feat)
2. **Task 2: Route endpoints (public catalog + admin CRUD)** - `e42da70` (feat)

## Files Created/Modified

- `backend/src/lib/tink.ts` - initTink/encryptGpx/decryptGpx using binaryInsecure.deserializeKeyset + getPrimitive(Aead)
- `backend/src/routes/routes.ts` - GET /routes (Redis cached), GET /routes/:id, GET /routes/:id/gpx (license-gated)
- `backend/src/routes/adminRoutes.ts` - POST/PATCH/DELETE /admin/routes with Tink encryption, multipart stream handling
- `backend/src/app.ts` - Registered routeHandlers at /api/v1/routes and adminRouteHandlers at /api/v1/admin/routes
- `backend/src/server.ts` - Added initTink() call before server.listen()
- `backend/.env.example` - Added keyset generation command comment for TINK_KEYSET_JSON

## Decisions Made

- The plan's pseudocode used a hypothetical `aead.keysetHandle()` / `aead.primitive()` API that does not exist in tink-crypto v0.1.1. The real API uses `aead.register()` + `binaryInsecure.deserializeKeyset(binaryKeyset)` + `keysetHandle.getPrimitive(Aead)`. Adapted accordingly — the three key invariants (AES-256-GCM, in-memory only, async Buffer return) are all satisfied.
- `TINK_KEYSET_JSON` env var name retained but the value format changed from base64-encoded JSON keyset to base64-encoded binary keyset (Tink's native serialization format). The `.env.example` comment was updated to document the correct generation command.
- Route ID used as AAD for Tink AEAD encryption — this binds each ciphertext cryptographically to its route. The same `routeId` AAD must be provided in Plan 04's license check / GPX decrypt flow.
- In POST `/admin/routes`, the route UUID is generated before calling `encryptGpx()` so it can be used as AAD, then passed as `id` to `prisma.route.create()`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Adapted tink-crypto API from plan pseudocode to actual package API**
- **Found during:** Task 1 (Tink GPX encryption helper)
- **Issue:** Plan pseudocode used `aead.keysetHandle(JSON.parse(keysetJson))` and `aead.primitive(keysetHandle)` — these methods do not exist in tink-crypto v0.1.1. The actual API uses `aead.register()`, `binaryInsecure.deserializeKeyset(Uint8Array)`, and `keysetHandle.getPrimitive(Aead)`.
- **Fix:** Implemented using actual API. TINK_KEYSET_JSON env var now stores base64-encoded binary keyset instead of base64-encoded JSON keyset. The plan's three invariants (AES-256-GCM, in-memory, async Buffer) are all maintained.
- **Files modified:** `backend/src/lib/tink.ts`, `backend/.env.example`
- **Verification:** `npx tsc --noEmit` — zero TypeScript errors
- **Committed in:** `3d3da3d` (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 bug — API adaptation)
**Impact on plan:** Required adaptation for functional code. All security invariants maintained. No scope creep.

## Issues Encountered

None beyond the tink-crypto API adaptation documented above.

## User Setup Required

A new environment variable format for `TINK_KEYSET_JSON` is now required. The keyset is now a base64-encoded binary Tink keyset (not JSON). Generate with:

```bash
node -e "
  const {aead, binaryInsecure} = require('tink-crypto');
  aead.register();
  aead.generateNew(aead.aes256GcmKeyTemplate()).then(h => {
    console.log(Buffer.from(binaryInsecure.serializeKeyset(h)).toString('base64'));
  });
"
```

Add the output as `TINK_KEYSET_JSON` in your `.env` file.

## Next Phase Readiness

- `encryptGpx` and `decryptGpx` are exported from `backend/src/lib/tink.ts` — Plan 04 (license check) must import `decryptGpx` and use the same `routeId` as AAD
- Route CRUD endpoints are complete; Docker stack must be running (`docker compose up`) and migration applied before testing
- The `catalog` Redis key is the shared cache key used by both routes.ts and adminRoutes.ts — any code that modifies route published state must call `redis.del('catalog')`
- All route responses explicitly exclude `gpxEncrypted` via Prisma `select` — this pattern must be maintained in any future route queries

---
*Phase: 01-backend-foundation*
*Completed: 2026-03-14*
