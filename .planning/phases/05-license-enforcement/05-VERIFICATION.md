---
phase: 05-license-enforcement
verified: 2026-03-15T14:00:00Z
status: gaps_found
score: 7/9 must-haves verified
re_verification: false
gaps:
  - truth: "A Snackbar appears when checkLicense fails, showing the server-returned reason (e.g. 'License expired', 'No license found')"
    status: partial
    reason: "The backend POST /check always returns code 'LICENSE_INVALID' on 403 regardless of reason (expired vs. not found vs. revoked). The Android client maps 'EXPIRED', 'NOT_FOUND', 'REVOKED' — none of which will ever match. All failures fall to the else branch and display 'No license found', not the reason-specific messages the plan requires."
    artifacts:
      - path: "backend/src/routes/licenses.ts"
        issue: "Both 403 branches return { code: 'LICENSE_INVALID' }. The client expects 'EXPIRED', 'NOT_FOUND', or 'REVOKED'."
      - path: "android/app/src/main/java/com/roadrunner/app/data/repository/LicenseRepository.kt"
        issue: "parseErrorCode + when block at lines 95-100 maps 'EXPIRED'/'NOT_FOUND'/'REVOKED' but backend never sends these codes."
    missing:
      - "Either: update backend to return code 'EXPIRED' / 'NOT_FOUND' / 'REVOKED' by querying the specific license state"
      - "Or: update Android client to handle 'LICENSE_INVALID' and map it to an appropriate user message"

  - truth: "Catalog, route detail, and My Routes show accurate license type and expiry date/time"
    status: partial
    reason: "Route detail (RouteDetailScreen) shows full license type and formatted expiry date. However, the catalog RouteCard and My Routes screen (which reuses RouteCard) only render a status label badge ('Active', 'Expired', etc.) — the expiresAt field is passed to LicenseStatusBadge but is never displayed. Success criterion 4 explicitly requires expiry date/time in the catalog and My Routes views."
    artifacts:
      - path: "android/app/src/main/java/com/roadrunner/app/ui/catalog/RouteCard.kt"
        issue: "LicenseStatusBadge receives expiresAt but only renders a label chip. Expiry date/time is not shown in catalog or My Routes list items."
    missing:
      - "LicenseStatusBadge (or RouteCard) should render the expiresAt date when licenseStatus is ACTIVE or EXPIRING_SOON"

human_verification:
  - test: "Tap 'Start Navigation' on a route with an active license"
    expected: "Button shows CircularProgressIndicator during check, then navigates to NavigationScreen with 'Navigation coming in Phase 6'"
    why_human: "End-to-end navigation flow with real server; UI state transitions during async call cannot be verified statically"

  - test: "Wait 30 seconds on NavigationScreen after the server-issued session has expired"
    expected: "ExpiryDialog appears with 'Session Expired' title; tapping OK clears session and navigates to Catalog with full back-stack cleared"
    why_human: "30-second polling loop and session expiry timing requires a running device"

  - test: "Tap 'Start Navigation' on a route with EXPIRED license status"
    expected: "Button is disabled; label 'License expired — contact us to renew' shown beneath button; no network call made"
    why_human: "Conditional enabled state requires a real device with appropriate license fixture"

  - test: "Tap 'Start Navigation' on a route with AVAILABLE license status"
    expected: "Button is disabled; label 'Purchase a license to start navigation' shown beneath button"
    why_human: "Requires real device and route without license"
---

# Phase 5: License Enforcement Verification Report

**Phase Goal:** Licenses are validated server-side on every navigation start, expired licenses block new navigation sessions, active sessions continue for up to one hour after expiry, and users can see exactly what they own.
**Verified:** 2026-03-15T14:00:00Z
**Status:** gaps_found
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | GET /api/v1/licenses/my returns authenticated user's licenses as JSON array | VERIFIED | `licenses.ts` line 102–132: GET /my with requireAuth, prisma.license.findMany(revokedAt=null), maps dates to ISO strings |
| 2 | LicenseRepository.getMyLicenses() fetches from endpoint and updates in-memory cache | VERIFIED | `LicenseRepository.kt` lines 23–37: calls apiService.getMyLicenses(), clears + addAll on success, preserves stale on failure |
| 3 | LicenseRepository.checkLicense(routeId) calls POST /licenses/check and returns NavigationSession or user-readable failure | VERIFIED (partial gap) | Call and success path verified; failure path has code mismatch — see gap 1 |
| 4 | Tapping "Start Navigation" with OWNED/ACTIVE/EXPIRING_SOON calls POST /check; on success navigates to NavigationScreen | VERIFIED | `RouteDetailScreen.kt` lines 155–180: canNavigate check; `RouteDetailViewModel.kt` lines 69–82: startNavigation() calls licenseRepository.checkLicense, stores session, calls onNavigate() |
| 5 | Tapping "Start Navigation" with EXPIRED or AVAILABLE is blocked with label | VERIFIED | `RouteDetailScreen.kt` lines 155–180: `enabled = canNavigate && !uiState.isStartingNavigation`; label text shown when !canNavigate |
| 6 | NavigationScreen polls every 30 seconds; ExpiryDialog shown when sessionExpiresAt elapses; dismiss clears session | VERIFIED | `NavigationScreen.kt` lines 33–41: LaunchedEffect(Unit) with while(true)/delay(30_000L)/isSessionExpired() break; ExpiryDialog wired at lines 43–50 |
| 7 | New navigation sessions after expiry are blocked by server-issued timestamp (not device clock) | VERIFIED | `NavigationSessionManager.kt`: sessionExpiresAtMillis set from Instant.parse(serverTimestamp); `licenses.ts` buildSessionResponse uses Date.now() + 1h; device clock only used for grace-period polling, not the security gate |
| 8 | Snackbar shows server-returned reason on checkLicense failure | FAILED | Backend always returns `code: 'LICENSE_INVALID'`; Android maps 'EXPIRED'/'NOT_FOUND'/'REVOKED' which never match; all failures show 'No license found' |
| 9 | Catalog, route detail, and My Routes show license type and expiry date/time | PARTIAL | Route detail fully shows type + formatted expiry. Catalog RouteCard and My Routes pass expiresAt to LicenseStatusBadge but the badge only renders a status label, not the date. |

**Score:** 7/9 truths verified (2 with gaps)

---

### Required Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| `backend/src/routes/licenses.ts` | VERIFIED (gap in 403 codes) | GET /my exists, requireAuth, prisma.license.findMany; POST /check uses server clock, signs JWT with 1h sessionExpiresAt |
| `android/.../dto/LicenseDtos.kt` | VERIFIED | LicenseDto and NavigationSession defined correctly |
| `android/.../remote/ApiService.kt` | VERIFIED | getMyLicenses() and checkLicense() both declared |
| `android/.../repository/LicenseRepository.kt` | VERIFIED (gap in error mapping) | @Singleton, _cachedLicenses, all three methods present and substantive |
| `android/.../data/local/NavigationSessionManager.kt` | VERIFIED | @Singleton, storeSession() parses server ISO timestamp, isSessionExpired() uses System.currentTimeMillis() |
| `android/.../ui/navigation/NavigationScreen.kt` | VERIFIED | Scaffold + TopAppBar + 30s polling LaunchedEffect + ExpiryDialog wiring |
| `android/.../ui/navigation/ExpiryDialog.kt` | VERIFIED | AlertDialog with "Session Expired" title, single OK button |
| `android/.../navigation/Screen.kt` | VERIFIED | Navigation object with createRoute(routeId) present |
| `android/.../navigation/NavGraph.kt` | VERIFIED | NavGraphEntryPoint @EntryPoint, Navigation composable destination wired, RouteDetail passes onStartNavigation |
| `android/.../ui/routedetail/RouteDetailViewModel.kt` | VERIFIED | startNavigation(), clearNavigationError(), licenseRepository + sessionManager injected, loadRoute() calls getMyLicenses() first |
| `android/.../ui/routedetail/RouteDetailScreen.kt` | VERIFIED | Conditional button, Snackbar, LicenseStatusSection with type + formatted expiry |
| `android/.../data/repository/RouteRepository.kt` | VERIFIED | LicenseRepository injected; getRoutesWithLicenseStatus() calls getMyLicenses() then computeLicenseStatus(); checkLicenseStatus() delegates to computeLicenseStatus() |
| `android/.../ui/catalog/RouteCard.kt` | PARTIAL | LicenseStatusBadge renders status label only; expiresAt parameter accepted but not displayed |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| GET /my handler | prisma.license.findMany | revokedAt: null filter | WIRED | `licenses.ts` line 107–120 |
| LicenseRepository.checkLicense | ApiService.checkLicense | Retrofit POST /licenses/check | WIRED | `LicenseRepository.kt` line 81 |
| LicenseRepository.getMyLicenses | _cachedLicenses | clear() + addAll() on success | WIRED | `LicenseRepository.kt` lines 28–30 |
| RouteDetailScreen Start Navigation | RouteDetailViewModel.startNavigation() | onClick lambda line 160 | WIRED | `RouteDetailScreen.kt` line 160 |
| RouteDetailViewModel.startNavigation | LicenseRepository.checkLicense | licenseRepository.checkLicense(routeId) line 72 | WIRED | `RouteDetailViewModel.kt` line 72 |
| RouteDetailViewModel.startNavigation (success) | NavigationSessionManager | sessionManager.storeSession(token, expiresAt) line 74 | WIRED | `RouteDetailViewModel.kt` line 74 |
| NavigationScreen LaunchedEffect | NavigationSessionManager.isSessionExpired() | 30-second polling loop | WIRED | `NavigationScreen.kt` lines 33–41 |
| RouteRepository.getRoutesWithLicenseStatus | LicenseRepository.getMyLicenses() | first call in try block | WIRED | `RouteRepository.kt` line 27 |
| RouteDetailViewModel.loadRoute | LicenseRepository.getMyLicenses | called before checkLicenseStatus | WIRED | `RouteDetailViewModel.kt` line 48 |
| Backend POST /check 403 code | LicenseRepository error mapping | code field 'EXPIRED'/'NOT_FOUND'/'REVOKED' | NOT WIRED | Backend sends 'LICENSE_INVALID'; client never matches specific codes |

---

### Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| LIC-01 | 05-01, 05-02 | License validated server-side on every navigation start | SATISFIED | POST /check called in startNavigation(); server uses server clock for expiry; Redis caches for 60s |
| LIC-02 | 05-01, 05-02 | Navigation blocked when license expired | SATISFIED | canNavigate excludes EXPIRED/AVAILABLE; button disabled; POST /check also rejects expired licenses at server |
| LIC-03 | 05-01, 05-02 | Active session continues up to 1h after expiry; new session blocked | SATISFIED | sessionExpiresAt is server-issued JWT (Date.now() + 1h); NavigationSessionManager stores and polls this; new POST /check after expiry returns 403 |
| LIC-04 | 05-01, 05-03 | User can see license type and expiry date/time | PARTIALLY SATISFIED | Route detail: full type + formatted expiry shown. Catalog + My Routes: status badge shown but expiry date not rendered. ROADMAP success criterion 4 explicitly requires catalog. |

---

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| `NavigationScreen.kt` line 68 | `Text("Navigation coming in Phase 6")` | Info | Placeholder per plan — Phase 6 replaces this. Intentional. |
| `RouteDetailScreen.kt` lines 297–299 | `onClick = { /* v1: manual licensing — no action */ }` | Info | Purchase buttons have no action — v1 design, manual licensing. Intentional. |
| `backend/src/routes/licenses.ts` lines 54, 81 | 403 always sends `code: 'LICENSE_INVALID'` | Warning | Error reason indistinguishable client-side; Snackbar always shows "No license found" |

---

### Human Verification Required

#### 1. Start Navigation happy path

**Test:** With a valid (non-expired) license, tap "Start Navigation" on Route Detail
**Expected:** Button briefly shows spinner, then NavigationScreen appears with "Navigation coming in Phase 6" and a Back arrow
**Why human:** Async state transition during network call; requires running server + device

#### 2. Session expiry polling during active navigation

**Test:** Set server to issue a session token with a very short expiry (or manipulate device clock), open NavigationScreen, wait up to 30s after the session window closes
**Expected:** ExpiryDialog appears with "Session Expired" title; tapping OK navigates to Catalog with full back stack cleared (cannot navigate back)
**Why human:** 30-second poll timing requires a running device; session expiry requires server coordination

#### 3. Start Navigation with expired license

**Test:** With an expired license for a route, open Route Detail
**Expected:** "Start Navigation" button is disabled; "License expired — contact us to renew" label visible beneath it
**Why human:** Requires a real license fixture with a past expiresAt

#### 4. Start Navigation with no license

**Test:** Open Route Detail for a route without any license
**Expected:** Button disabled; "Purchase a license to start navigation" label visible; status badge shows "Available"
**Why human:** Requires a route without a license fixture

---

## Gaps Summary

Two gaps block full goal achievement:

**Gap 1 — Backend/client 403 error code contract mismatch (Warning severity):**
The plan specified reason-specific 403 codes (`EXPIRED`, `NOT_FOUND`, `REVOKED`) to give users clear messages when navigation is blocked. The backend implementation sends `code: 'LICENSE_INVALID'` for all failures, which the Android client's when-branch never matches. As a result, every failed license check shows "No license found" regardless of whether the license is expired, revoked, or simply absent. The core security enforcement (blocking navigation on 403) still works correctly — only the message specificity is degraded.

**Gap 2 — Catalog and My Routes missing expiry date display (LIC-04 partial):**
ROADMAP success criterion 4 requires the catalog to show expiry date/time for owned routes. The `RouteCard.LicenseStatusBadge` composable receives the `expiresAt` string but only renders a status label chip ("Active", "Expired", etc.). The expiry date is invisible in catalog and My Routes list views. Users must navigate into a route's detail screen to see when a time-based license expires. Route detail itself is complete.

Both gaps are isolated and fixable without structural changes:
- Gap 1: update either the backend 403 response codes or the Android mapping
- Gap 2: add expiry text to `LicenseStatusBadge` for ACTIVE and EXPIRING_SOON states

---

_Verified: 2026-03-15T14:00:00Z_
_Verifier: Claude (gsd-verifier)_
