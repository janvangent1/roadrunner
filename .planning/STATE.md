---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
stopped_at: Completed 02-admin-dashboard-04-PLAN.md
last_updated: "2026-03-14T22:28:45.230Z"
last_activity: "2026-03-14 — Plan 01-02 complete: Auth subsystem (register/login/google/refresh/logout), JWT middleware"
progress:
  total_phases: 7
  completed_phases: 1
  total_plans: 9
  completed_plans: 8
  percent: 50
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-14)

**Core value:** Riders can discover and legally ride premium curated offroad routes without ever being able to extract or share the underlying GPX data.
**Current focus:** Phase 1 — Backend Foundation

## Current Position

Phase: 1 of 7 (Backend Foundation)
Plan: 2 of 4 in current phase
Status: In progress
Last activity: 2026-03-14 — Plan 01-02 complete: Auth subsystem (register/login/google/refresh/logout), JWT middleware

Progress: [█████░░░░░] 50%

## Performance Metrics

**Velocity:**
- Total plans completed: 2
- Average duration: 5.5 min
- Total execution time: 0.18 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-backend-foundation | 2/4 | 11 min | 5.5 min |

**Recent Trend:**
- Last 5 plans: 01-01 (8 min), 01-02 (3 min)
- Trend: Fast

*Updated after each plan completion*
| Phase 01-backend-foundation P03 | 6 | 2 tasks | 6 files |
| Phase 01-backend-foundation P04 | 2 | 2 tasks | 3 files |
| Phase 02-admin-dashboard P01 | 3 | 2 tasks | 14 files |
| Phase 02-admin-dashboard P02 | 2 | 2 tasks | 11 files |
| Phase 02-admin-dashboard P03 | 2 | 2 tasks | 7 files |
| Phase 02-admin-dashboard P04 | 2 | 2 tasks | 2 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- v1 payments: No in-app billing — licenses granted manually by admin via dashboard; Google Play Billing deferred to v2
- Map library: OSMDroid 6.1.20 (archived Nov 2024) — deliberate project constraint; tile cache must be set to `getCacheDir()` before any map code, not external storage
- Encryption: Google Tink 1.20.0 + Android Keystore (AES-256-GCM); `androidx.security:security-crypto` is deprecated Jul 2025 and must not be used
- License validation: Server-side only; device clock must never be trusted for expiry; StrongBox used only to wrap/unwrap session key, not for bulk decryption
- DB schema: `linked_purchase_token` and `revoked_at` columns required from day one to prevent zombie tokens
- [Phase 01-backend-foundation]: tink-crypto npm package is v0.1.1 not 0.0.1; corrected in package.json
- [Phase 01-backend-foundation]: Migration created manually (no Docker in dev env); must run prisma migrate deploy on first docker compose up
- [Phase 01-backend-foundation]: buildApp() is async factory returning Promise<FastifyInstance> for plugin support and test isolation
- [Phase 01-backend-foundation]: Refresh token stored as bcrypt hash in DB; raw UUID returned to client; bcrypt.compare scanning used for match
- [Phase 01-backend-foundation]: fastify-plugin wraps authRoutes to prevent scope encapsulation and expose JWT decorator to child plugins
- [Phase 01-backend-foundation]: Google Sign-In upsert: find by googleId, then by email (link), then create new user
- [Phase 01-backend-foundation]: app.addHook('onClose') used for Prisma/Redis disconnect instead of SIGTERM in app.ts
- [Phase 01-backend-foundation]: tink-crypto actual API uses binaryInsecure.deserializeKeyset + getPrimitive(Aead) — not plan pseudocode; TINK_KEYSET_JSON stores base64-encoded binary keyset
- [Phase 01-backend-foundation]: Route ID used as Tink AAD for encryption — Plan 04 must use same routeId AAD when decrypting GPX
- [Phase 01-backend-foundation]: POST /admin/routes generates UUID before encrypt so it can be used as AAD, then passes that UUID as id to prisma.route.create
- [Phase 01-backend-foundation]: JWT sign uses 'as any' cast to bypass strict payload type — navigation session payload is correct at runtime
- [Phase 01-backend-foundation]: Negative license results also cached for 60s TTL to prevent DB hammering on invalid repeated checks
- [Phase 02-admin-dashboard]: GET /admin/routes excludes gpxEncrypted from select; PUT /:id/waypoints uses prisma transaction for atomic replacement
- [Phase 02-admin-dashboard]: Next.js output set to standalone for minimal Docker image on Raspberry Pi
- [Phase 02-admin-dashboard]: Auth guard is client-side only (useEffect in AdminLayout) because JWT lives in localStorage, not cookies; middleware cannot inspect it
- [Phase 02-admin-dashboard]: WaypointRow uses string types for latitude/longitude to preserve form input state; caller converts to float on submit
- [Phase 02-admin-dashboard]: sortOrder is index-derived on submit (not tracked in component state) to keep WaypointEditor purely controlled
- [Phase 02-admin-dashboard]: getLicenseStatus helper returns label+variant tuple; client-side filter on fetched data; Zod refine for cross-field expiresAt requirement

### Pending Todos

None yet.

### Blockers/Concerns

- **Phase 3 research flag:** OSMDroid GPX overlay performance at high point counts — Douglas-Peucker simplification needed; assess during Phase 3 planning
- **Phase 5 research flag:** Play Console internal test track setup and RTDN Pub/Sub configuration — assess during Phase 5 planning
- **Open question:** `minSdkVersion = 24` (required by Tink 1.20.0) — confirm with project owner before committing; affects <0.5% of devices

## Session Continuity

Last session: 2026-03-14T22:28:45.226Z
Stopped at: Completed 02-admin-dashboard-04-PLAN.md
Resume file: None
