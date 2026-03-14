---
phase: 02-admin-dashboard
plan: "01"
subsystem: backend-admin-api, dashboard-scaffold
tags: [next.js, typescript, tailwind, shadcn, docker, fastify, prisma]
dependency_graph:
  requires: [01-backend-foundation]
  provides: [admin-routes-api, admin-waypoints-api, dashboard-shell, typed-api-client]
  affects: [02-02, 02-03, 02-04]
tech_stack:
  added: [next@15.2.0, react@19, sonner, react-hook-form, zod, lucide-react, class-variance-authority, tailwindcss-animate]
  patterns: [standalone-nextjs-build, typed-fetch-client, prisma-transaction-replace]
key_files:
  created:
    - dashboard/package.json
    - dashboard/tsconfig.json
    - dashboard/next.config.ts
    - dashboard/tailwind.config.ts
    - dashboard/postcss.config.mjs
    - dashboard/Dockerfile
    - dashboard/.env.example
    - dashboard/src/app/globals.css
    - dashboard/src/app/layout.tsx
    - dashboard/src/app/page.tsx
    - dashboard/src/types/index.ts
    - dashboard/src/lib/api.ts
  modified:
    - backend/src/routes/adminRoutes.ts
    - docker-compose.yml
decisions:
  - GET /admin/routes excludes gpxEncrypted from select (same exclusion pattern as public routes)
  - PUT /:id/waypoints uses prisma.$transaction([deleteMany, createMany]) for atomic replacement
  - dashboard/src/lib/api.ts uses localStorage accessToken — server components will use cookie-based auth when needed
  - Next.js output set to standalone for minimal Docker image size on Raspberry Pi
metrics:
  duration: "3 min"
  completed_date: "2026-03-14"
  tasks_completed: 2
  files_created: 12
  files_modified: 2
---

# Phase 02 Plan 01: Backend Admin API Gaps + Dashboard Scaffold Summary

**One-liner:** GET /admin/routes and PUT /admin/routes/:id/waypoints added to backend; Next.js 15 app shell in dashboard/ with Shadcn/ui, shared TypeScript types, typed API client, and docker-compose service.

## Tasks Completed

| # | Task | Commit | Key Files |
|---|------|--------|-----------|
| 1 | Add GET /admin/routes and PUT /admin/routes/:id/waypoints | 44824f7 | backend/src/routes/adminRoutes.ts |
| 2 | Scaffold Next.js 15 dashboard, types, API client, Docker service | e931c75 | dashboard/src/types/index.ts, dashboard/src/lib/api.ts, docker-compose.yml |

## What Was Built

### Backend: Two new admin route handlers

**GET /api/v1/admin/routes** — Lists all routes including unpublished ones. Supports optional `?published=true|false` query param. Excludes `gpxEncrypted` from select. Maps `distanceKm` Decimal to Number.

**PUT /api/v1/admin/routes/:id/waypoints** — Replaces all waypoints for a route atomically using `prisma.$transaction([deleteMany, createMany])`. Validates route existence (404 if missing) and waypoint type values (400 if invalid). Returns the new waypoints ordered by `sortOrder`.

Both handlers use `requireAuth + requireAdmin` guards.

### Dashboard: Next.js 15 project shell

- `dashboard/src/types/index.ts` — Shared TypeScript types mirroring Prisma schema: `Route`, `Waypoint`, `License`, `Difficulty`, `WaypointType`, `LicenseType`, `ApiError`
- `dashboard/src/lib/api.ts` — Typed fetch helpers: `login`, `getAdminRoutes`, `createRoute`, `updateRoute`, `deleteRoute`, `replaceWaypoints`, `getAdminLicenses`, `grantLicense`, `updateLicense`, `apiFetch`
- Tailwind CSS + Shadcn/ui configuration with neutral theme CSS variables
- Standalone Next.js Dockerfile for minimal Raspberry Pi container
- docker-compose.yml `dashboard` service on port 3001

## Deviations from Plan

None — plan executed exactly as written.

## Self-Check: PASSED

Files verified present:
- backend/src/routes/adminRoutes.ts: FOUND (contains GET / and PUT /:id/waypoints)
- dashboard/src/types/index.ts: FOUND
- dashboard/src/lib/api.ts: FOUND
- docker-compose.yml: FOUND (contains dashboard service on port 3001)

Commits verified:
- 44824f7: FOUND (feat(02-01): add GET /admin/routes and PUT /:id/waypoints)
- e931c75: FOUND (feat(02-01): scaffold Next.js 15 dashboard)
