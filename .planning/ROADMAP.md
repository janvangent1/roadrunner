# Roadmap: Roadrunner

## Overview

Roadrunner is built in seven phases that follow the hard dependency chain imposed by the architecture: the backend API must exist before anything else can be tested against real data; the admin dashboard must ship before the Android app has a catalog to display; the Android rendering and encryption layers are deliberately separated so map bugs and crypto bugs are never entangled; license enforcement comes after all content pipelines are solid; navigation UX is refined once the core system is provably correct; and security hardening closes the release. Every phase delivers a coherent, independently verifiable capability.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Backend Foundation** - Fastify API, PostgreSQL schema, Redis, and server-side GPX encryption pipeline (completed 2026-03-14)
- [x] **Phase 2: Admin Dashboard** - Next.js dashboard for route upload, catalog management, and manual license grants (completed 2026-03-15)
- [ ] **Phase 3: Android Catalog and Auth** - Android app with account system, route catalog, route detail, and OSMDroid preview map
- [ ] **Phase 4: Android Encryption Layer** - Tink AES-256-GCM encrypted GPX storage and decrypt-at-render pipeline
- [ ] **Phase 5: License Enforcement** - Server-side license validation, grace period logic, and navigation gating
- [ ] **Phase 6: Navigation Experience** - Full navigation screen with HUD, off-route indicator, waypoints, and offline tile caching
- [ ] **Phase 7: Security Hardening and Flavor Scaffold** - R8 hardening, Play Integrity API, HTTPS pinning, and motorcycle product flavor

## Phase Details

### Phase 1: Backend Foundation
**Goal**: The backend API, database schema, and server-side encryption pipeline exist and accept real data, unblocking all Android and dashboard development against live endpoints.
**Depends on**: Nothing (first phase)
**Requirements**: Infrastructure phase — no direct v1 requirement IDs, but this phase is the prerequisite for all 34 v1 requirements. Schema must include `linked_purchase_token` and `revoked_at` columns from day one (zombie token prevention).
**Success Criteria** (what must be TRUE):
  1. `GET /routes` returns a list of routes from the database with metadata
  2. `POST /admin/routes` accepts a GPX file upload and stores it encrypted (AES-256-GCM) — the plaintext GPX is never written to disk after encryption
  3. `GET /routes/:id/gpx` returns the encrypted GPX blob to an authenticated client
  4. The PostgreSQL schema includes user, route, license, and purchase_token tables with `linked_purchase_token` and `revoked_at` columns
  5. Redis is connected and the license cache layer responds to GET/SET operations
**Plans**: 4 plans

Plans:
- [ ] 01-01-PLAN.md — Project scaffold, Docker Compose, and Prisma schema (all four tables)
- [ ] 01-02-PLAN.md — Auth endpoints (register, login, google, refresh, logout) and JWT middleware
- [ ] 01-03-PLAN.md — Route endpoints and Tink AES-256-GCM GPX encryption pipeline
- [ ] 01-04-PLAN.md — Admin license management and license check with Redis cache

### Phase 2: Admin Dashboard
**Goal**: The admin can upload GPX routes with full metadata, manage the catalog, and manually grant or revoke licenses — the catalog is populated and ready for real Android testing.
**Depends on**: Phase 1
**Requirements**: ADMIN-01, ADMIN-02, ADMIN-03, ADMIN-04, ADMIN-05, ADMIN-06
**Success Criteria** (what must be TRUE):
  1. Admin can sign in to the dashboard and only authenticated admins can access it
  2. Admin can upload a GPX file with title, description, difficulty, terrain type, region, and estimated duration — the route appears in the catalog
  3. Admin can add named waypoints/POIs (label, coordinates, type) to a route
  4. Admin can grant a license to a user by email, selecting license type (day pass with date / multi-day rental with expiry / permanent) and the grant appears in the license table
  5. Admin can revoke or modify an existing license grant and the change is immediately reflected
  6. Admin can publish, unpublish, edit, and delete routes; unpublished routes do not appear in the app catalog
**Plans**: 5 plans

Plans:
- [ ] 02-01-PLAN.md — Backend gap (GET/PUT admin routes), Next.js scaffold, shared types, API client, Docker service
- [ ] 02-02-PLAN.md — Login page, auth guard (AdminLayout), NavBar
- [ ] 02-03-PLAN.md — Routes catalog table (/routes) and route create page (/routes/new) with GPX upload and WaypointEditor
- [ ] 02-04-PLAN.md — Licenses table (/licenses) and grant license page (/licenses/new)
- [ ] 02-05-PLAN.md — Route edit page (/routes/[id]) and license edit/revoke page (/licenses/[id])

### Phase 3: Android Catalog and Auth
**Goal**: A rider can open the Android app, create or sign in to an account, browse the route catalog, and view route detail pages with a preview map — the complete pre-purchase experience works end-to-end.
**Depends on**: Phase 2
**Requirements**: AUTH-01, AUTH-02, AUTH-03, AUTH-04, CATA-01, CATA-02, CATA-03, CATA-04, DETL-01, DETL-02, DETL-03, DETL-04
**Success Criteria** (what must be TRUE):
  1. User can create an account with email and password, or sign in with Google one-tap
  2. User session persists across app restarts without re-login; user can sign out from any screen
  3. User can browse all published routes in a list showing title, distance, difficulty, terrain type, region, and thumbnail map per card
  4. Each route card shows the correct license status badge (Owned / Available / Expires in X / Expired) based on the user's grants
  5. User can open a route detail page showing full metadata, a preview map with the route line visible, available purchase options with prices, and current license status with expiry date if applicable
  6. "My Routes" library shows only routes the user holds a valid license for
**Plans**: 5 plans

Plans:
- [ ] 03-01-PLAN.md — Gradle scaffold, Hilt, NavGraph, OSMDroid init, motorcycle flavor
- [ ] 03-02-PLAN.md — Network layer (Retrofit/OkHttp), Tink token storage, AuthRepository
- [ ] 03-03-PLAN.md — Auth screens: LoginScreen, RegisterScreen, session-aware NavGraph
- [ ] 03-04-PLAN.md — Route data layer, CatalogScreen with license badges, MyRoutesScreen
- [ ] 03-05-PLAN.md — RouteDetailScreen with OSMDroid preview map and purchase options

### Phase 4: Android Encryption Layer
**Goal**: GPX route data is stored on-device encrypted and decrypted only in memory at render time — the security boundary that protects route content is in place and validated.
**Depends on**: Phase 3
**Requirements**: PROT-01, PROT-02, PROT-03
**Success Criteria** (what must be TRUE):
  1. A downloaded GPX file on the device is stored as an AES-256-GCM ciphertext blob in app-private internal storage — no plaintext GPX exists on disk at any time
  2. The route line renders correctly on the OSMDroid map by decrypting the GPX to a `ByteArray` in memory and passing it directly to the overlay renderer — no intermediate file is written
  3. There is no button, menu item, share sheet, or API endpoint in the app that allows a user to export or access the GPX data
  4. On a rooted device, the file stored in `getFilesDir()` cannot be opened as valid GPX without the app's Keystore-backed key
**Plans**: TBD

### Phase 5: License Enforcement
**Goal**: Licenses are validated server-side on every navigation start, expired licenses block new navigation sessions, active sessions continue for up to one hour after expiry, and users can see exactly what they own.
**Depends on**: Phase 4
**Requirements**: LIC-01, LIC-02, LIC-03, LIC-04
**Success Criteria** (what must be TRUE):
  1. Tapping "Start Navigation" triggers a server-side license check — a user with a valid license proceeds; a user with no license or an expired license is blocked with a clear message
  2. A user whose license expires during an active navigation session continues navigating uninterrupted for up to 1 hour; after that hour, navigation stops and the user must renew before starting a new session
  3. Starting a new navigation session after license expiry is blocked even if the device clock has been tampered with (validation uses server-issued timestamp)
  4. The route catalog, route detail page, and "My Routes" screen all display accurate license type and expiry date/time for each owned route
**Plans**: TBD

### Phase 6: Navigation Experience
**Goal**: The navigation screen is fully functional for offroad motorcycle use: real-time GPS position, ride stats HUD, off-route warning, waypoint pins, and offline map tiles all work without a cell signal.
**Depends on**: Phase 5
**Requirements**: NAV-01, NAV-02, NAV-03, NAV-04, NAV-05, NAV-06, WAYPT-01, WAYPT-02
**Success Criteria** (what must be TRUE):
  1. A user with a valid license can start navigation and sees the OSMDroid base map with the purchased route rendered as a polyline overlay
  2. The user's real-time GPS position dot moves on the map as the device moves
  3. The navigation HUD displays current speed, distance covered, distance remaining, and elapsed time — all updating in real time
  4. When GPS position diverges more than ~50 m from the route line, a visible "off-route" indicator activates; it clears when the user returns to the route
  5. Admin-defined waypoints/POIs appear as labeled map pins on the navigation map during an active session
  6. Map tiles for a purchased route area are pre-cached on-device and navigation works fully offline with no cell signal
**Plans**: TBD

### Phase 7: Security Hardening and Flavor Scaffold
**Goal**: The release build is hardened against reverse engineering, device integrity is verified before content keys are issued, and the Android project is structured to support the future sport car app variant with no architectural rework.
**Depends on**: Phase 6
**Requirements**: Architectural objective (product flavor scaffold for future sport car variant per PROJECT.md constraint; no standalone v1 requirement ID)
**Success Criteria** (what must be TRUE):
  1. The release APK has R8 minification enabled; `Log.*` calls are stripped; decompiling the APK does not expose plaintext decryption logic or API keys
  2. Play Integrity API verification runs at account registration and before the first content key is issued — a device that fails the check is denied content delivery
  3. HTTPS certificate pinning is active in release builds; a MITM proxy cannot intercept API traffic without triggering a certificate error
  4. A `motorcycle` product flavor builds successfully with the correct branding, strings, and icons; the project structure supports adding a `sportscar` flavor without touching `:core` module code
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6 → 7

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Backend Foundation | 4/4 | Complete    | 2026-03-14 |
| 2. Admin Dashboard | 5/5 | Complete    | 2026-03-15 |
| 3. Android Catalog and Auth | 4/5 | In Progress|  |
| 4. Android Encryption Layer | 0/? | Not started | - |
| 5. License Enforcement | 0/? | Not started | - |
| 6. Navigation Experience | 0/? | Not started | - |
| 7. Security Hardening and Flavor Scaffold | 0/? | Not started | - |
