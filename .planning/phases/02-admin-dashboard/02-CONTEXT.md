---
phase: 2
title: Admin Dashboard
status: ready
---

# Phase 2 Context: Admin Dashboard

## Decisions

### Stack (locked from research)
- **Framework:** Next.js 15 (App Router, TypeScript)
- **UI components:** Shadcn/ui + Tailwind CSS
- **Forms:** React Hook Form + Zod validation
- **HTTP client:** fetch (native, no axios) against the Phase 1 Fastify API
- **File upload:** Native HTML file input → multipart/form-data to `POST /api/v1/admin/routes`
- **Auth:** JWT stored in httpOnly cookie or localStorage — admin logs in via `POST /api/v1/auth/login` with `role=ADMIN` account

### Hosting
- Runs on the same Raspberry Pi as the backend
- Docker Compose — add `dashboard` service to `docker-compose.yml`
- Served via Next.js standalone build (not `next start` in dev mode)

### Scope
This is an admin-only tool, not a public-facing app. Design should be functional and clear, not polished. No need for dark mode, animations, or marketing copy.

### Pages / Routes
```
/login                          → Admin sign-in form
/                               → Redirect to /routes if logged in
/routes                         → Route catalog list (all routes, incl. unpublished)
/routes/new                     → Upload GPX + fill metadata + add waypoints
/routes/[id]                    → Edit route metadata, manage waypoints, publish/unpublish/delete
/licenses                       → All license grants (filter by route or user email)
/licenses/new                   → Grant a new license (pick user email, route, type, expiry)
/licenses/[id]                  → Edit or revoke a license
```

### Key UI patterns
- Route list: table with columns — Title, Region, Difficulty, Published (toggle), Distance, Actions (Edit / Delete)
- Route form: fields for title, description, difficulty (select), terrain_type (text), region (text), estimated_duration_minutes (number), distance_km (number), published (checkbox)
- Waypoint editor: inline list below route form — add/remove waypoints with label, lat/lng, type (select: FUEL/WATER/CAUTION/INFO), sort_order auto-managed
- License list: table with columns — User Email, Route, Type, Expires At, Status (Active/Expired/Revoked), Actions (Edit / Revoke)
- License form: user email input, route selector (dropdown of all routes), type selector (DAY_PASS/MULTI_DAY/PERMANENT), expires_at date picker (hidden if PERMANENT)

### API contract (from Phase 1)
All requests use `Authorization: Bearer <jwt>` header.
- `POST /api/v1/auth/login` → `{ accessToken, refreshToken }`
- `GET /api/v1/admin/routes` — not implemented in Phase 1; dashboard needs `GET /api/v1/routes` (returns published only) — need to verify if admin can see unpublished. If not, may need a small backend addition.
- `POST /api/v1/admin/routes` → multipart with GPX file + JSON metadata fields
- `PATCH /api/v1/admin/routes/:id` → JSON metadata or multipart GPX replacement
- `DELETE /api/v1/admin/routes/:id`
- `POST /api/v1/admin/licenses` → `{ userEmail, routeId, type, expiresAt }`
- `PATCH /api/v1/admin/licenses/:id` → `{ type?, expiresAt?, revokedAt? }`
- `GET /api/v1/admin/licenses` → list of all grants

### Backend gap to address
`GET /api/v1/routes` returns only `published: true` routes. The admin dashboard needs all routes (including drafts). The dashboard should call `GET /api/v1/admin/routes` — if this doesn't exist in Phase 1, add it as a small backend addition in Phase 2's first plan.

## Deferred Ideas
- Route preview map on the edit page (requires map component — defer to Phase 3)
- Bulk operations (publish many, delete many)
- CSV export of license history
- User management page (list all users)
