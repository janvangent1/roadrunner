# Requirements: Roadrunner

**Defined:** 2026-03-14
**Core Value:** Riders can discover and legally ride premium curated offroad routes without ever being able to extract or share the underlying GPX data.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Authentication

- [ ] **AUTH-01**: User can create an account with email and password
- [ ] **AUTH-02**: User can sign in with Google (one-tap Android sign-in)
- [ ] **AUTH-03**: User session persists across app restarts without re-login
- [ ] **AUTH-04**: User can sign out

### Route Catalog

- [ ] **CATA-01**: User can browse all available routes in a list view
- [ ] **CATA-02**: Each route card shows title, distance, difficulty, terrain type, region, and thumbnail map
- [ ] **CATA-03**: User can see license status badge on each route (Owned / Available / Expires in X / Expired)
- [ ] **CATA-04**: User can access "My Routes" library showing only their purchased/licensed routes

### Route Detail

- [ ] **DETL-01**: User can view route detail page with full metadata before purchasing
- [ ] **DETL-02**: Route detail shows distance, difficulty, terrain type, estimated duration, region, and preview map with route line visible (data locked)
- [ ] **DETL-03**: Route detail shows available purchase options (day pass / multi-day rental / permanent) with prices
- [ ] **DETL-04**: User can see current license status and expiry date/time if applicable

### Licensing

- [ ] **LIC-01**: License is account-bound and validated server-side on every navigation start
- [ ] **LIC-02**: Navigation is blocked when a license has expired
- [ ] **LIC-03**: An active navigation session continues for up to 1 hour after license expiry (grace period); starting a new session after expiry is blocked
- [ ] **LIC-04**: User can see which routes they have access to and what type of license they hold (day pass / multi-day rental with expiry date / permanent)

### Navigation

- [ ] **NAV-01**: User can launch navigation for any route with a valid license
- [ ] **NAV-02**: Navigation displays an OpenStreetMap base map (OSMDroid) with the purchased route rendered as a polyline overlay
- [ ] **NAV-03**: Navigation shows the user's real-time GPS position dot on the map
- [ ] **NAV-04**: Navigation HUD displays glanceable ride stats: current speed, distance covered, distance remaining, elapsed time
- [ ] **NAV-05**: Navigation shows a visual "off-route" indicator when GPS position diverges significantly from the route line
- [ ] **NAV-06**: Map tiles for a purchased route area are pre-cached on-device for fully offline navigation

### Route Protection

- [ ] **PROT-01**: GPX route data is stored on-device encrypted with AES-256-GCM (Google Tink)
- [ ] **PROT-02**: GPX data is decrypted only in-memory at render time — plaintext is never written to accessible device storage
- [ ] **PROT-03**: There is no mechanism in the app to export or share the GPX file

### Waypoints

- [ ] **WAYPT-01**: Admin can annotate a route with named waypoints/POIs (fuel stops, water crossings, tricky sections) during upload
- [ ] **WAYPT-02**: Waypoints are displayed as labeled map pins during navigation

### Admin Dashboard

- [x] **ADMIN-01**: Admin can upload a GPX file and fill in route metadata (title, description, difficulty, terrain type, region, estimated duration)
- [x] **ADMIN-02**: Admin can add waypoints/POIs to a route (label, coordinates, type)
- [ ] **ADMIN-03**: Admin can manually grant a license to a user account (by email) for a specific route, selecting license type: day pass (with date), multi-day rental (with expiry date), or permanent
- [ ] **ADMIN-04**: Admin can revoke or modify an existing license grant
- [ ] **ADMIN-05**: Admin can publish, unpublish, edit, and delete routes from the catalog
- [ ] **ADMIN-06**: Admin can view all active license grants per route and per user

---

## v2 Requirements

Deferred to a future release. Tracked but not in the current roadmap.

### Payments

- **PAY-V2-01**: In-app purchasing via Google Play Billing (day pass, multi-day rental, permanent) — replace manual grants with automated payment + license flow

### Navigation Enhancements

- **NAV-V2-01**: Elevation profile chart displayed on route detail page (requires Z-values in GPX files)
- **NAV-V2-02**: Offline map tile pre-download prompt triggered immediately after purchase

### Engagement

- **ENG-V2-01**: Push notification when a rental license is within 24 hours of expiry
- **ENG-V2-02**: In-app renewal prompt shown on route detail when a rental has recently expired

### Catalog

- **CATA-V2-01**: Region/area browsing — map-based or filter by geographic region (relevant once catalog has 10+ routes)
- **CATA-V2-02**: Star rating on purchased routes (post-ride feedback, no moderation needed)

---

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| GPX file export / download | Destroys the core business model — content protection is the product |
| In-app payments (Google Play Billing) | Deferred to v2; v1 uses manual license grants by admin after direct payment |
| Turn-by-turn voice guidance | High complexity; conflicts with motorcycle intercom systems; explicitly out of scope per PROJECT.md |
| iOS version | Android-only v1; double engineering overhead; defer until Android v1 is profitable |
| Community / user-uploaded routes | Introduces content moderation burden and quality dilution; single-seller model is the value proposition |
| Social features (comments, likes, follows) | Scope explosion; competing with AllTrails on their home turf with no advantage |
| Multiple sellers / marketplace | Architectural change; only if single-seller model saturates |
| Sport car app variant | Separate future project per PROJECT.md; re-skin from same architecture after v1 ships |
| Real-time trail conditions / weather | Requires data partnerships that don't exist at launch |
| Offline rerouting | OSMDroid has no routing engine; offroad routes often deviate intentionally |

---

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | Phase 3 | Pending |
| AUTH-02 | Phase 3 | Pending |
| AUTH-03 | Phase 3 | Pending |
| AUTH-04 | Phase 3 | Pending |
| CATA-01 | Phase 3 | Pending |
| CATA-02 | Phase 3 | Pending |
| CATA-03 | Phase 3 | Pending |
| CATA-04 | Phase 3 | Pending |
| DETL-01 | Phase 3 | Pending |
| DETL-02 | Phase 3 | Pending |
| DETL-03 | Phase 3 | Pending |
| DETL-04 | Phase 3 | Pending |
| LIC-01 | Phase 5 | Pending |
| LIC-02 | Phase 5 | Pending |
| LIC-03 | Phase 5 | Pending |
| LIC-04 | Phase 5 | Pending |
| NAV-01 | Phase 6 | Pending |
| NAV-02 | Phase 6 | Pending |
| NAV-03 | Phase 6 | Pending |
| NAV-04 | Phase 6 | Pending |
| NAV-05 | Phase 6 | Pending |
| NAV-06 | Phase 6 | Pending |
| PROT-01 | Phase 4 | Pending |
| PROT-02 | Phase 4 | Pending |
| PROT-03 | Phase 4 | Pending |
| WAYPT-01 | Phase 6 | Pending |
| WAYPT-02 | Phase 6 | Pending |
| ADMIN-01 | Phase 2 | Complete |
| ADMIN-02 | Phase 2 | Complete |
| ADMIN-03 | Phase 2 | Pending |
| ADMIN-04 | Phase 2 | Pending |
| ADMIN-05 | Phase 2 | Pending |
| ADMIN-06 | Phase 2 | Pending |

**Coverage:**
- v1 requirements: 34 total
- Mapped to phases: 34 (complete)
- Unmapped: 0

**Phase Distribution:**
- Phase 1 (Backend Foundation): Infrastructure prerequisite — no standalone requirement IDs; enables all 34
- Phase 2 (Admin Dashboard): ADMIN-01, ADMIN-02, ADMIN-03, ADMIN-04, ADMIN-05, ADMIN-06 (6 requirements)
- Phase 3 (Android Catalog and Auth): AUTH-01, AUTH-02, AUTH-03, AUTH-04, CATA-01, CATA-02, CATA-03, CATA-04, DETL-01, DETL-02, DETL-03, DETL-04 (12 requirements)
- Phase 4 (Android Encryption Layer): PROT-01, PROT-02, PROT-03 (3 requirements)
- Phase 5 (License Enforcement): LIC-01, LIC-02, LIC-03, LIC-04 (4 requirements)
- Phase 6 (Navigation Experience): NAV-01, NAV-02, NAV-03, NAV-04, NAV-05, NAV-06, WAYPT-01, WAYPT-02 (8 requirements)
- Phase 7 (Security Hardening and Flavor Scaffold): Architectural objective per PROJECT.md (product flavor scaffold for future sport car variant)

---
*Requirements defined: 2026-03-14*
*Last updated: 2026-03-14 after roadmap creation — traceability complete*
