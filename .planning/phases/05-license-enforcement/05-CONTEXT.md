---
phase: 5
title: License Enforcement
status: ready
---

# Phase 5 Context: License Enforcement

## Decisions

### License check flow (locked from research + Phase 1 backend)
- App calls `POST /api/v1/licenses/check` with `{ routeId }` before launching navigation
- Backend responds: `{ valid: true, sessionToken: "<JWT>", expiresAt: "<ISO>" }` or `{ valid: false, reason: "EXPIRED" | "NOT_FOUND" | "REVOKED" }`
- The `sessionToken` is a short-lived JWT (1-hour window) issued by the server using server clock — never trust device clock
- App stores the `sessionToken` in memory (not persisted to disk) for the current navigation session
- On expiry: session continues up to 1 hour after `expiresAt` (grace period); new navigation sessions blocked

### Grace period implementation
- At navigation start: call `/licenses/check` → receive `expiresAt` from server
- Store `expiresAt` (ISO string) in `NavigationSessionManager` (in-memory singleton)
- During navigation: periodically check `System.currentTimeMillis()` vs `expiresAt + 1 hour`
- When grace period ends: show expiry dialog → stop navigation → navigate back to route detail
- A new navigation session after expiry: call `/licenses/check` again → server returns `valid: false` → show "License expired" screen

### Anti-tampering
- Never trust `System.currentTimeMillis()` for license validity — the server decides via `/licenses/check`
- Device clock can be set backwards; the server-issued `expiresAt` + server-side validation is the source of truth
- The 1-hour grace is counted from server-issued `expiresAt`, stored in memory — not from device-local time

### License status display
- `LicenseRepository` replaces/extends the existing `RouteRepository` license logic from Phase 3
- On catalog load: for each route, fetch user's license from a new backend endpoint `GET /api/v1/licenses/my` (returns all licenses for the authenticated user)
- Cache license list in `LicenseRepository` (in-memory, refreshed on catalog load and after a navigation session ends)
- License status chip shows: Available / Owned (permanent) / Expires in X days / Expires today / Expired

### New backend endpoint needed
`GET /api/v1/licenses/my` — returns all licenses for the currently authenticated user (not admin-only). This endpoint was not built in Phase 1 (only admin license management was built). Add it as part of Phase 5's first plan.

### "Start Navigation" button
- Currently disabled (Phase 3 left it disabled)
- Phase 5: enable it when `licenseStatus == OWNED || licenseStatus == ACTIVE || licenseStatus == EXPIRING_SOON`
- On tap: call `/licenses/check` → if valid, store session token + navigate to NavigationScreen placeholder
- NavigationScreen is a placeholder in Phase 5 — full implementation is Phase 6

### UI changes this phase
- RouteDetailScreen: enable "Start Navigation" button conditionally
- RouteDetailScreen: show license expiry date/time (DETL-04 was in Phase 3 — verify it's shown, fix if missing)
- NavigationScreen: create placeholder with just a "Back" button (actual nav in Phase 6)
- ExpiryDialog: shown mid-navigation when grace period ends

## Deferred
- Actual navigation UI (Phase 6)
- Offline grace period handling (covered: 1-hour in-memory session)
- Push notifications for expiry (v2)
