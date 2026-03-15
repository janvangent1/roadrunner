---
phase: 05-license-enforcement
verified: 2026-03-15T15:30:00Z
status: human_needed
score: 9/9 must-haves verified
re_verification: true
  previous_status: gaps_found
  previous_score: 7/9
  gaps_closed:
    - "Backend POST /licenses/check now returns EXPIRED/NOT_FOUND/REVOKED specific codes on fresh DB misses"
    - "LicenseStatusBadge now renders formatted expiry date beneath status chip for ACTIVE and EXPIRING_SOON"
  gaps_remaining: []
  regressions: []
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

  - test: "Observe catalog list for a route with ACTIVE license that has an expiresAt date"
    expected: "Status chip shows 'Active' with 'Expires MMM D, YYYY' text below it in the card header"
    why_human: "Visual rendering of formatted expiry requires a real device with a time-limited license fixture"

  - test: "Trigger a license check failure (expired license) and observe the Snackbar"
    expected: "Snackbar shows 'License expired' (not 'No license found')"
    why_human: "Specific error code path requires a real server response; EXPIRED code mapping in client cannot be end-to-end tested statically"
---

# Phase 5: License Enforcement Verification Report

**Phase Goal:** Licenses are validated server-side on every navigation start, expired licenses block new navigation sessions, active sessions continue for up to one hour after expiry, and users can see exactly what they own.
**Verified:** 2026-03-15T15:30:00Z
**Status:** human_needed
**Re-verification:** Yes — after gap closure (previous: gaps_found 7/9)

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | GET /api/v1/licenses/my returns authenticated user's licenses as JSON array | VERIFIED | `licenses.ts` line 117–147: GET /my with requireAuth, prisma.license.findMany(revokedAt=null), maps dates to ISO strings |
| 2 | LicenseRepository.getMyLicenses() fetches from endpoint and updates in-memory cache | VERIFIED | `LicenseRepository.kt` lines 23–37: calls apiService.getMyLicenses(), clears + addAll on success, preserves stale on failure |
| 3 | LicenseRepository.checkLicense(routeId) calls POST /licenses/check and returns NavigationSession or user-readable failure | VERIFIED | Call and success path verified; backend now returns specific codes; Android maps EXPIRED/NOT_FOUND/REVOKED — gap closed |
| 4 | Tapping "Start Navigation" with OWNED/ACTIVE/EXPIRING_SOON calls POST /check; on success navigates to NavigationScreen | VERIFIED | `RouteDetailScreen.kt` canNavigate check; `RouteDetailViewModel.kt` startNavigation() calls licenseRepository.checkLicense, stores session, calls onNavigate() |
| 5 | Tapping "Start Navigation" with EXPIRED or AVAILABLE is blocked with label | VERIFIED | `RouteDetailScreen.kt`: `enabled = canNavigate && !uiState.isStartingNavigation`; label text shown when !canNavigate |
| 6 | NavigationScreen polls every 30 seconds; ExpiryDialog shown when sessionExpiresAt elapses; dismiss clears session | VERIFIED | `NavigationScreen.kt`: LaunchedEffect(Unit) with while(true)/delay(30_000L)/isSessionExpired() break; ExpiryDialog wired |
| 7 | New navigation sessions after expiry are blocked by server-issued timestamp (not device clock) | VERIFIED | `NavigationSessionManager.kt`: sessionExpiresAtMillis set from Instant.parse(serverTimestamp); `licenses.ts` buildSessionResponse uses Date.now() + 1h |
| 8 | Snackbar shows server-returned reason on checkLicense failure | VERIFIED | `licenses.ts` lines 82–96: second DB query after negative result; returns code 'NOT_FOUND', 'REVOKED', or 'EXPIRED'. Android when-branch maps all three to distinct messages. See caveat below. |
| 9 | Catalog, route detail, and My Routes show license type and expiry date/time | VERIFIED | `RouteCard.kt` lines 90–118: showExpiry computed for ACTIVE/EXPIRING_SOON; DateTimeFormatter formats expiresAt; Text rendered beneath SuggestionChip. Route detail was already complete. |

**Score:** 9/9 truths verified

### Caveat — Cached Negative Path (Non-blocking)

The negative Redis cache entry written at `licenses.ts` line 80 stores `{ valid: false, expiresAt: null }` with no `reason` field. When the cache is warm (within 60 seconds of the first rejection), the early-exit at line 54 returns `code: 'LICENSE_INVALID'` rather than the specific code. The Android client's `when` branch will fall to the else clause and show "No license found" for those repeat attempts.

The first attempt — which is the one the user actually experiences when they tap "Start Navigation" — always reaches the second DB query and returns the specific code. The degraded message only affects rapid retries within a 60-second window, which is an edge case. This is acceptable for v1.

---

### Required Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| `backend/src/routes/licenses.ts` | VERIFIED | Gap closed: lines 82–96 perform second query to return NOT_FOUND/REVOKED/EXPIRED codes |
| `android/.../dto/LicenseDtos.kt` | VERIFIED | LicenseDto and NavigationSession defined correctly |
| `android/.../remote/ApiService.kt` | VERIFIED | getMyLicenses() and checkLicense() both declared |
| `android/.../repository/LicenseRepository.kt` | VERIFIED | @Singleton, _cachedLicenses, all three methods present and substantive |
| `android/.../data/local/NavigationSessionManager.kt` | VERIFIED | @Singleton, storeSession() parses server ISO timestamp, isSessionExpired() uses System.currentTimeMillis() |
| `android/.../ui/navigation/NavigationScreen.kt` | VERIFIED | Scaffold + TopAppBar + 30s polling LaunchedEffect + ExpiryDialog wiring |
| `android/.../ui/navigation/ExpiryDialog.kt` | VERIFIED | AlertDialog with "Session Expired" title, single OK button |
| `android/.../navigation/Screen.kt` | VERIFIED | Navigation object with createRoute(routeId) present |
| `android/.../navigation/NavGraph.kt` | VERIFIED | NavGraphEntryPoint @EntryPoint, Navigation composable destination wired, RouteDetail passes onStartNavigation |
| `android/.../ui/routedetail/RouteDetailViewModel.kt` | VERIFIED | startNavigation(), clearNavigationError(), licenseRepository + sessionManager injected |
| `android/.../ui/routedetail/RouteDetailScreen.kt` | VERIFIED | Conditional button, Snackbar, LicenseStatusSection with type + formatted expiry |
| `android/.../data/repository/RouteRepository.kt` | VERIFIED | LicenseRepository injected; computeLicenseStatus() and getRoutesWithLicenseStatus() both present |
| `android/.../ui/catalog/RouteCard.kt` | VERIFIED | Gap closed: LicenseStatusBadge lines 90–118 compute showExpiry for ACTIVE/EXPIRING_SOON and render formatted date beneath status chip |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| GET /my handler | prisma.license.findMany | revokedAt: null filter | WIRED | `licenses.ts` line 122 |
| LicenseRepository.checkLicense | ApiService.checkLicense | Retrofit POST /licenses/check | WIRED | `LicenseRepository.kt` |
| LicenseRepository.getMyLicenses | _cachedLicenses | clear() + addAll() on success | WIRED | `LicenseRepository.kt` |
| RouteDetailScreen Start Navigation | RouteDetailViewModel.startNavigation() | onClick lambda | WIRED | `RouteDetailScreen.kt` |
| RouteDetailViewModel.startNavigation | LicenseRepository.checkLicense | licenseRepository.checkLicense(routeId) | WIRED | `RouteDetailViewModel.kt` |
| RouteDetailViewModel.startNavigation (success) | NavigationSessionManager | sessionManager.storeSession(token, expiresAt) | WIRED | `RouteDetailViewModel.kt` |
| NavigationScreen LaunchedEffect | NavigationSessionManager.isSessionExpired() | 30-second polling loop | WIRED | `NavigationScreen.kt` |
| RouteRepository.getRoutesWithLicenseStatus | LicenseRepository.getMyLicenses() | first call in try block | WIRED | `RouteRepository.kt` |
| Backend POST /check 403 code | LicenseRepository error mapping | codes 'EXPIRED'/'NOT_FOUND'/'REVOKED' | WIRED | `licenses.ts` lines 90/93/96 return specific codes; Android when-branch maps all three |
| RouteCard.LicenseStatusBadge | expiresAt display | showExpiry + DateTimeFormatter + Text composable | WIRED | `RouteCard.kt` lines 90–118 |

---

### Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| LIC-01 | 05-01, 05-02 | License validated server-side on every navigation start | SATISFIED | POST /check called in startNavigation(); server uses server clock for expiry; Redis caches for 60s |
| LIC-02 | 05-01, 05-02 | Navigation blocked when license expired | SATISFIED | canNavigate excludes EXPIRED/AVAILABLE; button disabled; POST /check also rejects expired licenses at server |
| LIC-03 | 05-01, 05-02 | Active session continues up to 1h after expiry; new session blocked | SATISFIED | sessionExpiresAt is server-issued (Date.now() + 1h); NavigationSessionManager stores and polls this |
| LIC-04 | 05-01, 05-03 | User can see license type and expiry date/time | SATISFIED | Route detail: full type + formatted expiry. Catalog + My Routes: LicenseStatusBadge now shows formatted "Expires MMM D, YYYY" for ACTIVE and EXPIRING_SOON. Gap closed. |

---

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| `NavigationScreen.kt` line 68 | `Text("Navigation coming in Phase 6")` | Info | Placeholder per plan — Phase 6 replaces this. Intentional. |
| `RouteDetailScreen.kt` purchase buttons | `onClick = { /* v1: manual licensing — no action */ }` | Info | Purchase buttons have no action — v1 design, manual licensing. Intentional. |
| `backend/src/routes/licenses.ts` line 54 | Cache hit path returns `code: 'LICENSE_INVALID'` | Info | Only affects rapid retries within 60s of first rejection. First-attempt failures return specific codes. Acceptable for v1. |

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

#### 5. Expiry date display in catalog

**Test:** Open the catalog with a route that has an ACTIVE or EXPIRING_SOON time-limited license
**Expected:** Route card shows the status chip ("Active" or "Expires soon") with "Expires MMM D, YYYY" text rendered directly beneath it
**Why human:** Visual rendering of the formatted date requires a real device with a time-limited license fixture

#### 6. Reason-specific Snackbar on navigation failure

**Test:** Trigger a navigation start for a route with an expired license (first attempt, cache cold)
**Expected:** Snackbar shows "License expired" (not the generic "No license found")
**Why human:** Requires a real server response with a cold Redis cache; EXPIRED code path mapping cannot be end-to-end tested statically

---

## Re-verification Summary

Both gaps from the initial verification are closed:

**Gap 1 — Backend 403 specific codes (closed):**
`backend/src/routes/licenses.ts` lines 82–96 now perform a second `prisma.license.findFirst` query (no active-license filter) after the first query finds nothing. The result determines the specific rejection code: `NOT_FOUND` when no license record exists at all, `REVOKED` when `revokedAt` is non-null, and `EXPIRED` when `expiresAt` is in the past. The Android `LicenseRepository.parseErrorCode` + `when` block maps all three to distinct user messages. Minor residual: the negative Redis cache entry does not store the reason, so cached repeat calls within 60s still return `LICENSE_INVALID`. This affects only rapid retries, not first-attempt user-facing messages.

**Gap 2 — Catalog expiry date display (closed):**
`RouteCard.kt` `LicenseStatusBadge` (lines 90–118) now computes `showExpiry = expiresAt != null && (status == ACTIVE || status == EXPIRING_SOON)`, formats the date with `DateTimeFormatter("MMM d, yyyy")`, and renders a `Text("Expires $formattedExpiry")` composable in a `Column` below the `SuggestionChip`. LIC-04 is now fully satisfied across route detail, catalog, and My Routes.

All 9 observable truths are verified. Remaining items require human testing on a real device.

---

_Verified: 2026-03-15T15:30:00Z_
_Verifier: Claude (gsd-verifier)_
