---
phase: 02-admin-dashboard
plan: "03"
subsystem: ui
tags: [nextjs, react, shadcn, radix-ui, react-hook-form, zod, tailwind]

# Dependency graph
requires:
  - phase: 02-admin-dashboard-02
    provides: Auth utilities, layout, NavBar, api.ts with getAdminRoutes/createRoute/updateRoute/deleteRoute
  - phase: 02-admin-dashboard-01
    provides: api.ts function signatures and Route/Waypoint types
provides:
  - Routes catalog table at /routes with publish toggle and delete
  - Route create form at /routes/new with GPX upload and metadata
  - WaypointEditor reusable component for add/remove/edit waypoints
  - Shadcn/ui Select, Table, Badge, Dialog components
affects: [02-admin-dashboard-05]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Radix UI primitives wrapped as Shadcn/ui components with cn() className merging
    - Controlled waypoint list state managed in parent; WaypointEditor is pure controlled component
    - FormData multipart submission with JSON.stringify for nested arrays (waypoints field)
    - sortOrder derived from array index at submit time, not stored in component state

key-files:
  created:
    - dashboard/src/components/ui/select.tsx
    - dashboard/src/components/ui/table.tsx
    - dashboard/src/components/ui/badge.tsx
    - dashboard/src/components/ui/dialog.tsx
    - dashboard/src/components/WaypointEditor.tsx
    - dashboard/src/app/(admin)/routes/page.tsx
    - dashboard/src/app/(admin)/routes/new/page.tsx
  modified: []

key-decisions:
  - "WaypointRow uses string types for latitude/longitude to preserve form input state; caller converts to float on submit"
  - "sortOrder is index-derived on submit (not tracked in component state) to keep WaypointEditor stateless about ordering"

patterns-established:
  - "WaypointEditor is a fully controlled component: value prop + onChange callback; no internal state for rows"
  - "Route pages use useEffect + useState for client-side data fetching with toast error handling"

requirements-completed: [ADMIN-01, ADMIN-02, ADMIN-05]

# Metrics
duration: 2min
completed: 2026-03-14
---

# Phase 2 Plan 03: Routes Catalog and Create Page Summary

**Routes catalog table at /routes with publish toggle and delete, plus GPX upload + metadata form at /routes/new using reusable WaypointEditor component**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-14T22:22:42Z
- **Completed:** 2026-03-14T22:24:47Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments

- /routes page renders full catalog table (Title, Region, Difficulty, Distance, Published status) with inline publish toggle and delete with confirmation dialog
- /routes/new page provides GPX file upload, complete 7-field metadata form with Zod validation, and WaypointEditor integration; submits multipart FormData to createRoute()
- WaypointEditor reusable component allows adding and removing waypoint rows with label, latitude, longitude, type fields; sortOrder derived from array index on submit

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Shadcn/ui Select, Table, Badge, Dialog and WaypointEditor** - `577bf3c` (feat)
2. **Task 2: Build /routes catalog table and /routes/new create page** - `55e764b` (feat)

## Files Created/Modified

- `dashboard/src/components/ui/select.tsx` - Shadcn/ui Select with full Radix UI primitives
- `dashboard/src/components/ui/table.tsx` - Shadcn/ui Table with header, body, row, head, cell, caption
- `dashboard/src/components/ui/badge.tsx` - Shadcn/ui Badge with cva variants (default, secondary, destructive, outline)
- `dashboard/src/components/ui/dialog.tsx` - Shadcn/ui Dialog with overlay, content, header, footer, title, description
- `dashboard/src/components/WaypointEditor.tsx` - Reusable controlled waypoint list editor, exports WaypointEditor and WaypointRow
- `dashboard/src/app/(admin)/routes/page.tsx` - Routes catalog table page with publish toggle and delete
- `dashboard/src/app/(admin)/routes/new/page.tsx` - Route create page with GPX upload, metadata form, and WaypointEditor

## Decisions Made

- WaypointRow uses string types for latitude/longitude to preserve form input state; caller converts to float on submit
- sortOrder is index-derived on submit (not tracked in component state) to keep WaypointEditor purely controlled

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- WaypointEditor component is ready for reuse in the route edit page (Plan 05)
- /routes and /routes/new pages are fully functional, requiring only a running backend
- Dialog component is available for any confirmation dialogs in subsequent plans

---
*Phase: 02-admin-dashboard*
*Completed: 2026-03-14*

## Self-Check: PASSED

All 7 files verified present on disk. Both task commits (577bf3c, 55e764b) confirmed in git log.
