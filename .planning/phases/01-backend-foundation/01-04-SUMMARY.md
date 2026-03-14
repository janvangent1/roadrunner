---
phase: 01-backend-foundation
plan: "04"
subsystem: license-management
tags: [licenses, redis-cache, jwt, admin-api, navigation-gating]
dependency_graph:
  requires: ["01-02"]
  provides: ["license-check-endpoint", "admin-license-endpoints"]
  affects: ["navigation-session-jwt", "redis-license-cache"]
tech_stack:
  added: []
  patterns: ["redis-cache-aside-60s-ttl", "server-clock-validation", "fastify-plugin-export"]
key_files:
  created:
    - backend/src/routes/adminLicenses.ts
    - backend/src/routes/licenses.ts
  modified:
    - backend/src/app.ts
decisions:
  - "JWT sign uses `as any` cast to bypass strict payload type constraint — payload shape is correct at runtime; typed sign overload only allows known fields from auth token"
  - "licenseType stored in Redis cache entry so cache-hit path can return full response without DB round-trip"
  - "Negative license results also cached for 60s TTL to prevent DB hammering on repeated invalid checks"
metrics:
  duration: 2 min
  completed_date: "2026-03-14"
  tasks_completed: 2
  files_changed: 3
---

# Phase 1 Plan 4: License Management Endpoints Summary

Admin license CRUD + Redis-cached server-clock license check endpoint issuing 1-hour navigation JWTs.

## What Was Built

### Task 1: Admin license management endpoints

`backend/src/routes/adminLicenses.ts` — Fastify plugin exported as `adminLicenseHandlers`.

All routes protected by `[requireAuth, requireAdmin]` preHandlers.

- **POST /api/v1/admin/licenses** — grants a license by looking up user by email and verifying the route exists; validates that `expiresAt` is provided for `DAY_PASS`/`MULTI_DAY` types; creates `prisma.license` record with `revokedAt: null`, `linkedPurchaseToken: null`; invalidates `license:{userId}:{routeId}` cache key on write; returns 201 with license + `userEmail` + `routeTitle` for readability.

- **PATCH /api/v1/admin/licenses/:id** — modifies type, expiresAt, or sets/clears revokedAt using server clock; invalidates Redis cache key on every write; at least one field required enforced via Zod `.refine()`.

- **GET /api/v1/admin/licenses** — lists all licenses with joined `user.email` and `route.title`; filterable by `routeId` and `userId` query params; ordered by `createdAt desc`.

### Task 2: License check endpoint with Redis cache

`backend/src/routes/licenses.ts` — Fastify plugin exported as `licenseHandlers`. Protected by `[requireAuth]`.

- **POST /api/v1/licenses/check** — implements three-stage logic:
  1. Redis cache check on `license:{userId}:{routeId}`: returns 403 immediately on cached-invalid, or issues JWT on cached-valid (skips DB).
  2. DB query with `revokedAt: null` and server-clock `OR [expiresAt: null, expiresAt: { gt: new Date() }]` — device time is never used.
  3. Issues 1-hour navigation session JWT with `sub`, `routeId`, `sessionType: 'navigation'`, `issuedAt`, `sessionExpiresAt` (all from server clock).
  - Both positive and negative DB results are cached for 60s TTL.
  - Returns `{ valid, sessionToken, sessionExpiresAt, licenseType, licenseExpiresAt }`.

`backend/src/app.ts` updated — added registrations:
```
app.register(licenseHandlers, { prefix: '/api/v1/licenses' });
app.register(adminLicenseHandlers, { prefix: '/api/v1/admin/licenses' });
```

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] JWT sign overload type mismatch**

- **Found during:** Task 2
- **Issue:** `fastify.jwt.sign()` TypeScript overload enforces a specific payload shape matching the auth token `{ sub, role }`. Adding `routeId`, `sessionType`, etc. caused TS2769 "no overload matches".
- **Fix:** Used `(fastify.jwt.sign as any)(payload, options)` with an eslint-disable comment. The payload shape is correct at runtime; the type constraint is a limitation of the registered JWT plugin declaration.
- **Files modified:** `backend/src/routes/licenses.ts`
- **Commit:** 0bf6ef6

**2. [Rule 1 - Bug] Complex function overload causing TS errors**

- **Found during:** Task 2 (first draft)
- **Issue:** Helper function `issueSessionToken` was written with overload signatures that referenced unresolvable conditional types, producing TS2345/TS2394/TS2493 errors.
- **Fix:** Replaced overloads with single concrete signature `function buildSessionResponse(fastify, reply: FastifyReply, ...)` returning `FastifyReply`.
- **Files modified:** `backend/src/routes/licenses.ts`
- **Commit:** 0bf6ef6

## Self-Check

Files created/modified:
- backend/src/routes/adminLicenses.ts — FOUND
- backend/src/routes/licenses.ts — FOUND
- backend/src/app.ts — FOUND

Commits:
- 97c8ba7 — feat(01-04): implement admin license management endpoints
- 0bf6ef6 — feat(01-04): implement license check endpoint and register all license routes

## Self-Check: PASSED
