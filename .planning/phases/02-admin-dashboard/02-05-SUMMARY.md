---
phase: 02-admin-dashboard
plan: "05"
subsystem: ui
tags: [nextjs, react, shadcn, react-hook-form, zod, tailwind, routes, licenses, waypoints]

# Dependency graph
requires:
  - phase: 02-admin-dashboard-03
    provides: WaypointEditor component, routes catalog pages, shadcn/ui components
  - phase: 02-admin-dashboard-04
    provides: licenses list and grant pages, getLicenseStatus helper
  - phase: 02-admin-dashboard-01
    provides: api.ts with updateRoute, deleteRoute, replaceWaypoints, updateLicense, apiFetch
provides:
  - Route edit page at /routes/[id] with metadata form, waypoint manager, publish toggle, delete
  - License edit/revoke page at /licenses/[id] with type/expiry modification and revoke/reinstate toggle
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - apiFetch('/routes/:id') used directly for public endpoint that includes waypoints array
    - getAdminLicenses() fetch-all-then-filter pattern for single-item edit (no GET /admin/licenses/:id)
    - Revoke toggle via updateLicense({ revoked: !license.revokedAt }) — server sets/clears revokedAt
    - Local state updated with API response on revoke to reflect immediate status change

key-files:
  created:
    - dashboard/src/app/(admin)/routes/[id]/page.tsx
    - dashboard/src/app/(admin)/licenses/[id]/page.tsx
  modified: []

key-decisions:
  - "apiFetch('/routes/:id') (public endpoint) used to load route with waypoints — no dedicated admin single-route GET"
  - "License loaded by fetching all via getAdminLicenses() and filtering by id in client — no GET /admin/licenses/:id"
  - "Revoke/reinstate is a toggle: updateLicense({ revoked: !license.revokedAt }); local state updated from response"

patterns-established:
  - "Route edit page: two independent save actions (metadata PATCH + waypoints PUT) avoid coupling"
  - "License revoke toggle: immediate optimistic UI update via setLicense(updated) from API response"

requirements-completed: [ADMIN-02, ADMIN-04, ADMIN-05]

# Metrics
duration: 3min
completed: 2026-03-15
---

# Phase 2 Plan 05: Route Edit Page and License Edit/Revoke Page Summary

**Route edit page at /routes/[id] with metadata form + WaypointEditor + delete, and license edit/revoke page at /licenses/[id] with type/expiry change and revoke/reinstate toggle — completing all 6 ADMIN requirements**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-15T06:16:58Z
- **Completed:** 2026-03-15T06:19:30Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- /routes/[id] loads route metadata + waypoints via apiFetch('/routes/:id'), pre-fills form and WaypointEditor; saves metadata and waypoints independently; delete redirects to /routes
- /licenses/[id] loads license via getAdminLicenses() filter, displays user/route/status info, allows type and expiry modification, and toggles revoke/reinstate with immediate status badge update
- All 6 ADMIN requirements now covered across Plans 01-05: ADMIN-01/02/05 (03+05), ADMIN-03/06 (04), ADMIN-04 (05)

## Task Commits

Each task was committed atomically:

1. **Task 1: Route edit page — metadata, waypoints, publish/delete** - `fd3cfc7` (feat)
2. **Task 2: License edit and revoke page** - `102fced` (feat)

## Files Created/Modified

- `dashboard/src/app/(admin)/routes/[id]/page.tsx` - Route edit form with metadata fields (title, description, difficulty, terrainType, region, duration, distance, published), WaypointEditor integration, Save Metadata + Save Waypoints actions, Delete button with confirmation
- `dashboard/src/app/(admin)/licenses/[id]/page.tsx` - License detail display (user email, route title, status badge), type/expiry edit form with conditional expiresAt field, Revoke/Reinstate toggle button

## Decisions Made

- apiFetch('/routes/:id') used directly (public endpoint) to load route with waypoints array included; getAdminRoutes() does not include waypoints
- License loaded client-side by fetching all via getAdminLicenses() and filtering by id since no single-license GET endpoint exists
- Revoke/reinstate implemented as toggle via updateLicense({ revoked: !license.revokedAt }); local state updated from API response for immediate UI reflection

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- All 6 ADMIN requirements (ADMIN-01 through ADMIN-06) are now fully implemented across Phase 02
- Phase 02 admin dashboard is complete — all pages functional pending a running backend
- Phase 03 (mobile app) can begin; admin dashboard provides the route/license management foundation

---
*Phase: 02-admin-dashboard*
*Completed: 2026-03-15*
