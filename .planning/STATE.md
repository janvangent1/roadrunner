---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
stopped_at: Completed 03-android-catalog-and-auth-05-PLAN.md
last_updated: "2026-03-15T07:26:20.681Z"
last_activity: "2026-03-15 — Plan 03-04 complete: Route data layer (RouteDtos, ApiService, RouteRepository), CatalogScreen with pull-to-refresh and license badges, MyRoutesScreen"
progress:
  total_phases: 7
  completed_phases: 3
  total_plans: 14
  completed_plans: 14
  percent: 93
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-14)

**Core value:** Riders can discover and legally ride premium curated offroad routes without ever being able to extract or share the underlying GPX data.
**Current focus:** Phase 1 — Backend Foundation

## Current Position

Phase: 3 of 7 (Android Catalog and Auth)
Plan: 4 of 5 completed in current phase
Status: In progress
Last activity: 2026-03-15 — Plan 03-04 complete: Route data layer (RouteDtos, ApiService, RouteRepository), CatalogScreen with pull-to-refresh and license badges, MyRoutesScreen

Progress: [█████████░] 93%

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
| Phase 02-admin-dashboard P05 | 3 | 2 tasks | 2 files |
| Phase 03-android-catalog-and-auth P01 | 14 | 2 tasks | 13 files |
| Phase 03-android-catalog-and-auth P02 | 2 | 2 tasks | 10 files |
| Phase 03-android-catalog-and-auth P03 | 7 | 2 tasks | 6 files |
| Phase 03-android-catalog-and-auth P04 | 5 | 2 tasks | 10 files |
| Phase 03-android-catalog-and-auth P05 | 2 | 2 tasks | 4 files |

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
- [Phase 02-admin-dashboard]: apiFetch('/routes/:id') (public endpoint) used to load route with waypoints — no dedicated admin single-route GET
- [Phase 02-admin-dashboard]: License loaded by fetching all via getAdminLicenses() and filtering by id in client — no GET /admin/licenses/:id
- [Phase 02-admin-dashboard]: Revoke toggle: updateLicense({ revoked: \!license.revokedAt }); local state updated from API response for immediate status reflect
- [Phase 03-android-catalog-and-auth]: Kotlin 2.0 requires org.jetbrains.kotlin.plugin.compose plugin; composeOptions block removed
- [Phase 03-android-catalog-and-auth]: Theme.AppCompat.Light.NoActionBar used for Activity window theme; Compose handles visual theming via RoadrunnerTheme
- [Phase 03-android-catalog-and-auth]: osmdroidTileCache set to filesDir (not cacheDir); androidx.preference:preference-ktx added for Configuration.load()
- [Phase 03-android-catalog-and-auth]: Launcher icon refs removed from AndroidManifest; icon assets deferred to Phase 7 branding
- [Phase 03-android-catalog-and-auth]: AuthApiService uses plain OkHttpClient (no auth interceptor) to avoid circular dependency in TokenRefreshAuthenticator
- [Phase 03-android-catalog-and-auth]: google_server_client_id string resource added as placeholder in strings.xml — developer fills in from Google Cloud Console
- [Phase 03-android-catalog-and-auth]: material-icons-extended added to support Visibility/VisibilityOff password toggle icons — these are in extended set not in material3 core
- [Phase 03-android-catalog-and-auth]: AuthViewModel.resetState() called after onLoginSuccess/onRegisterSuccess in LaunchedEffect to prevent re-navigation on recomposition
- [Phase 03-android-catalog-and-auth]: coreLibraryDesugaring uses com.android.tools:desugar_jdk_libs:2.1.2 (not com.android.tools.desugar_jdk_libs which is invalid Maven group)
- [Phase 03-android-catalog-and-auth]: PullToRefreshBox from androidx.compose.material3.pulltorefresh available in BOM 2024.09.00 (Material3 1.3.0) — no deprecated material fallback needed
- [Phase 03-android-catalog-and-auth]: Phase 3 preview map shows region marker only (no GPX overlay) — GPX overlay deferred to Phase 4
- [Phase 03-android-catalog-and-auth]: PurchaseOptionCard uses TextButton with no action and price placeholder €X.XX — v1 manual licensing; payment integration in Phase 5

### Pending Todos

None yet.

### Blockers/Concerns

- **Phase 3 research flag:** OSMDroid GPX overlay performance at high point counts — Douglas-Peucker simplification needed; assess during Phase 3 planning
- **Phase 5 research flag:** Play Console internal test track setup and RTDN Pub/Sub configuration — assess during Phase 5 planning
- **Open question:** `minSdkVersion = 24` (required by Tink 1.20.0) — confirm with project owner before committing; affects <0.5% of devices

## Session Continuity

Last session: 2026-03-15T07:22:21.764Z
Stopped at: Completed 03-android-catalog-and-auth-05-PLAN.md
Resume file: None
