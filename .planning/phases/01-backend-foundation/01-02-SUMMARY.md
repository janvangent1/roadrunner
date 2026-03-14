---
phase: 01-backend-foundation
plan: 02
subsystem: auth
tags: [fastify, jwt, bcrypt, prisma, redis, google-auth, typescript, middleware]

# Dependency graph
requires:
  - phase: 01-backend-foundation/01-01
    provides: Fastify scaffold with buildApp() factory, Prisma schema with User/RefreshToken models, ioredis in package.json
provides:
  - Singleton PrismaClient (globalThis pattern, hot-reload safe)
  - Singleton ioredis client with redisGet/redisSet helpers
  - FastifyJWT type augmentation (payload.sub + payload.role)
  - POST /api/v1/auth/register — email+password registration, bcrypt hashed, returns access+refresh tokens
  - POST /api/v1/auth/login — email+password auth, returns access+refresh tokens
  - POST /api/v1/auth/google — Google ID token verification via google-auth-library, upserts user
  - POST /api/v1/auth/refresh — refresh token rotation (old token revoked, new token issued)
  - POST /api/v1/auth/logout — revokes refresh token in DB (requires valid access JWT)
  - requireAuth preHandler hook — verifies Bearer JWT, 401 on failure
  - requireAdmin preHandler hook — checks role===ADMIN, 403 on failure
affects: [01-03, 01-04, 02-admin-api, 03-android-app, 04-gpx-pipeline, 05-licensing]

# Tech tracking
tech-stack:
  added:
    - bcrypt ^6.0.0 (password hashing, saltRounds=12)
    - fastify-plugin ^4.5.1 (scope-escaping plugin wrapper for auth routes)
    - uuid ^9.0.1 (raw refresh token generation)
    - "@types/bcrypt ^5.0.0 (devDependency)"
    - "@types/uuid ^9.0.0 (devDependency)"
  patterns:
    - Refresh token rotation: raw UUID stored in DB as bcrypt hash; raw token returned to client; old token revoked on use
    - fastify-plugin wrapper for auth routes to avoid scope encapsulation (JWT decorator accessible outside plugin)
    - Zod safeParse validation on all request bodies (returns 400 with issue details on failure)
    - Graceful shutdown via app.addHook('onClose') — disconnects Prisma and quits Redis

key-files:
  created:
    - backend/src/lib/prisma.ts
    - backend/src/lib/redis.ts
    - backend/src/types/fastify.d.ts
    - backend/src/middleware/requireAuth.ts
    - backend/src/middleware/requireAdmin.ts
    - backend/src/routes/auth.ts
  modified:
    - backend/src/app.ts
    - backend/src/server.ts
    - backend/package.json

key-decisions:
  - "Refresh token stored as bcrypt hash in DB — raw UUID returned to client; scanning all valid tokens with bcrypt.compare() is correct approach for small user base"
  - "fastify-plugin used to wrap authRoutes so JWT decorator is accessible to routes registered in parent scope"
  - "Google ID token upsert: find by googleId first, then by email (link accounts), then create new — prevents duplicate accounts"
  - "app.addHook('onClose') chosen over SIGTERM in app.ts since server.ts already handles SIGTERM; onClose fires on server.close()"

patterns-established:
  - "requireAuth: always applied as preHandler in route options, not as global hook"
  - "requireAdmin: must come after requireAuth (relies on request.user being set)"
  - "Auth routes use fastify-plugin fp() wrapper so JWT fastify.jwt is accessible inside the plugin"

requirements-completed: []

# Metrics
duration: 3min
completed: 2026-03-14
---

# Phase 1 Plan 02: Authentication Subsystem Summary

**Five auth endpoints (register/login/google/refresh/logout) with JWT access+refresh token rotation, bcrypt-hashed passwords, Google ID token verification, and requireAuth/requireAdmin middleware hooks using @fastify/jwt**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-14T22:07:24Z
- **Completed:** 2026-03-14T22:09:48Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments

- Singleton Prisma and Redis clients using globalThis pattern (safe for ts-node-dev hot reloads)
- Five auth endpoints with Zod request validation: register (bcrypt hash, 409 on duplicate), login (bcrypt compare, 401 on mismatch), google (verifyIdToken via google-auth-library, upsert), refresh (token rotation, old token revoked), logout (requires JWT, revokes token in DB)
- requireAuth and requireAdmin preHandler hooks ready for use by Plans 01-03 and 01-04
- Access tokens expire 15 minutes; refresh tokens expire 7 days and are stored as bcrypt hashes

## Task Commits

Each task was committed atomically:

1. **Task 1: Singleton clients and FastifyJWT type augmentation** - `0e9dc0b` (feat)
2. **Task 2: Auth endpoints and middleware hooks** - `0ff089b` (feat)

## Files Created/Modified

- `backend/src/lib/prisma.ts` - Singleton PrismaClient using globalThis pattern
- `backend/src/lib/redis.ts` - Singleton ioredis client with redisGet/redisSet helpers and error logging
- `backend/src/types/fastify.d.ts` - FastifyJWT module augmentation: payload { sub, role } and user { sub, role }
- `backend/src/middleware/requireAuth.ts` - preHandler hook: request.jwtVerify(); 401 on failure
- `backend/src/middleware/requireAdmin.ts` - preHandler hook: role === ADMIN check; 403 on failure
- `backend/src/routes/auth.ts` - Five auth endpoints wrapped in fastify-plugin; Zod validation; bcrypt; uuid; google-auth-library
- `backend/src/app.ts` - Registers authRoutes at /api/v1/auth; onClose hook to disconnect Prisma and Redis
- `backend/src/server.ts` - Fixed pre-existing TypeScript error in logger.error call
- `backend/package.json` - Added bcrypt, fastify-plugin, uuid (deps); @types/bcrypt, @types/uuid (devDeps)

## Decisions Made

- Refresh tokens stored as bcrypt hashes in DB; raw UUID returned to client. On /refresh and /logout, all non-expired/non-revoked tokens for the user are fetched and bcrypt.compare() is used to find the match. This is correct for small user bases (low token count per user).
- `fastify-plugin` wraps `authRoutes` to prevent Fastify scope encapsulation — ensures the JWT decorator registered in `buildApp()` is accessible inside the auth plugin.
- Google Sign-In upsert logic: find by googleId first (returning user), then by email (link Google to existing account), then create new user. This prevents duplicate accounts for the same email.
- `app.addHook('onClose')` for Prisma/Redis disconnect rather than direct SIGTERM handling in `app.ts`, because `server.ts` already handles SIGTERM via `server.close()`, which triggers `onClose`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed pre-existing TypeScript error in server.ts**
- **Found during:** Task 1 (TypeScript compile check)
- **Issue:** `server.log.error('Error during shutdown', err)` — Fastify's Pino logger error() method does not accept `unknown` as second argument; TypeScript error TS2769
- **Fix:** Changed to `server.log.error({ err }, 'Error during shutdown')` — correct Pino logging pattern using structured object
- **Files modified:** `backend/src/server.ts`
- **Verification:** `npx tsc --noEmit` passed with zero errors
- **Committed in:** `0e9dc0b` (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Fix required to unblock TypeScript compile check. No scope creep.

## Issues Encountered

None beyond the pre-existing TypeScript error fixed above.

## User Setup Required

None — no new external service configuration required beyond Plan 01 setup (JWT_SECRET, REDIS_URL, GOOGLE_CLIENT_ID already documented in .env.example).

## Next Phase Readiness

- `requireAuth` and `requireAdmin` are exported and ready for use in Plans 01-03 (routes API) and 01-04 (license endpoints)
- `prisma` singleton is exported from `lib/prisma.ts` — all subsequent plans should import from this path
- `redis` singleton and `redisGet`/`redisSet` helpers exported from `lib/redis.ts` — ready for Plans 01-04 license check caching and route catalog caching
- Auth endpoints are complete; the Docker stack must be running (`docker compose up`) and migration applied (`docker compose exec api npx prisma migrate deploy`) before testing

---
*Phase: 01-backend-foundation*
*Completed: 2026-03-14*
