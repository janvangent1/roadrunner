# Roadrunner

## What This Is

Roadrunner is an Android app for offroad motorcycle enthusiasts that sells and rents curated GPX routes on a license basis. Users purchase access (day pass, multi-day rental, or permanent) via Google Play, then navigate routes directly inside the app — the underlying GPX data is encrypted and never exportable. A companion web dashboard lets the route owner upload and manage the catalog.

## Core Value

Riders can discover and legally ride premium curated offroad routes without ever being able to extract or share the underlying GPX data.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] User can browse the route catalog and see route details (distance, difficulty, preview map)
- [ ] User can purchase a license for a route via Google Play (day pass, multi-day rental, or permanent)
- [ ] User can launch turn-by-display navigation for a purchased route (map + route overlay, OSMDroid)
- [ ] GPX route data is stored encrypted on-device and never accessible outside the app
- [ ] License is account-bound and enforced server-side (prevents sharing between accounts)
- [ ] Navigation is blocked when license has expired, with a 1-hour grace period for active sessions
- [ ] Admin web dashboard allows uploading GPX files and managing the route catalog
- [ ] App targets Android, offroad motorcycle audience (v1); sport car version is a future separate app

### Out of Scope

- Turn-by-turn voice guidance — visual map display is sufficient for v1
- iOS version — Android-only for v1
- Marketplace (multiple sellers) — single seller (admin) model only
- GPX file export or sharing between users — core protection requirement
- Sport car app variant — separate future project after v1 ships

## Context

- Platform: Android native (or cross-platform with strong Android support)
- Map library: OSMDroid (OpenStreetMap-based, free, offline-capable)
- Payment: Google Play Billing (in-app purchases)
- Route protection: GPX stored encrypted, decrypted only at render time, never written to accessible storage
- License enforcement: server-side validation required (not just on-device) to prevent tampering
- Two future app variants planned: this one (offroad motorcycle) and a sport car version — architecture should allow easy re-skinning/forking

## Constraints

- **Platform**: Android only — iOS explicitly deferred
- **Payments**: Google Play Billing only — Google takes 15-30% but simplifies v1 distribution
- **Data protection**: GPX must never be extractable — core business requirement, not optional
- **Maps**: OSMDroid (OpenStreetMap) — no Google Maps SDK dependency

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Google Play Billing for payments | Simplest path for v1, handles receipts and subscription management | — Pending |
| OSMDroid for maps | Free, offline-capable, no API key costs | — Pending |
| Server-side license validation | Prevents client-side bypass of license checks | — Pending |
| Single-seller model (admin only) | Reduces complexity for v1; marketplace can come later | — Pending |
| Web dashboard for route management | Admin tooling separate from user-facing app | — Pending |

---
*Last updated: 2026-03-14 after initialization*
