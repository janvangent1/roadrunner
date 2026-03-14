# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-14)

**Core value:** Riders can discover and legally ride premium curated offroad routes without ever being able to extract or share the underlying GPX data.
**Current focus:** Phase 1 — Backend Foundation

## Current Position

Phase: 1 of 7 (Backend Foundation)
Plan: 0 of ? in current phase
Status: Ready to plan
Last activity: 2026-03-14 — Roadmap created; 7 phases derived from 34 v1 requirements

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**
- Total plans completed: 0
- Average duration: —
- Total execution time: 0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| - | - | - | - |

**Recent Trend:**
- Last 5 plans: —
- Trend: —

*Updated after each plan completion*

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- v1 payments: No in-app billing — licenses granted manually by admin via dashboard; Google Play Billing deferred to v2
- Map library: OSMDroid 6.1.20 (archived Nov 2024) — deliberate project constraint; tile cache must be set to `getCacheDir()` before any map code, not external storage
- Encryption: Google Tink 1.20.0 + Android Keystore (AES-256-GCM); `androidx.security:security-crypto` is deprecated Jul 2025 and must not be used
- License validation: Server-side only; device clock must never be trusted for expiry; StrongBox used only to wrap/unwrap session key, not for bulk decryption
- DB schema: `linked_purchase_token` and `revoked_at` columns required from day one to prevent zombie tokens

### Pending Todos

None yet.

### Blockers/Concerns

- **Phase 3 research flag:** OSMDroid GPX overlay performance at high point counts — Douglas-Peucker simplification needed; assess during Phase 3 planning
- **Phase 5 research flag:** Play Console internal test track setup and RTDN Pub/Sub configuration — assess during Phase 5 planning
- **Open question:** `minSdkVersion = 24` (required by Tink 1.20.0) — confirm with project owner before committing; affects <0.5% of devices

## Session Continuity

Last session: 2026-03-14
Stopped at: Roadmap created — ROADMAP.md, STATE.md written; REQUIREMENTS.md traceability updated
Resume file: None
