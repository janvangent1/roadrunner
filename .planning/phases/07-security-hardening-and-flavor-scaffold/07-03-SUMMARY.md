---
phase: 07-security-hardening-and-flavor-scaffold
plan: 03
subsystem: infra
tags: [okhttp, certificate-pinning, ssl, network-security, cloudflare, android]

# Dependency graph
requires:
  - phase: 07-security-hardening-and-flavor-scaffold
    provides: BuildConfig.CERT_PIN_SHA256 buildConfigField (set in plan 01 build.gradle.kts)
provides:
  - CertificatePinner attached to both OkHttp clients (auth + main) in NetworkModule, active only in release builds
  - DEBUG guard ensuring no pinning during development/emulator use
affects: [network-requests, ssl-pinning, api-security, mitm-protection]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Certificate pinning via private helper buildCertificatePinner() returning null in DEBUG — single point of control"
    - "OkHttpClient.Builder pattern: build pinner separately, apply via certificatePinner() before .build()"

key-files:
  created: []
  modified:
    - android/app/src/main/java/com/roadrunner/app/di/NetworkModule.kt

key-decisions:
  - "buildCertificatePinner() returns null in DEBUG and a CertificatePinner in release — single control point avoids duplication"
  - "Pin applied to both provideAuthApiService (pre-auth) and provideOkHttpClient (authenticated) clients for complete coverage"
  - "TileCacheWorker OkHttp client intentionally excluded — OSM tile servers must not be pinned"
  - "Wildcard hostname *.trycloudflare.com used as placeholder — operator must replace with stable tunnel hostname before release"

patterns-established:
  - "Certificate pinning pattern: private helper with DEBUG guard + nullable return, applied at builder stage in each provider"

requirements-completed:
  - ARCH-PINNING-01

# Metrics
duration: 3min
completed: 2026-03-15
---

# Phase 7 Plan 03: Certificate Pinning Summary

**OkHttp CertificatePinner added to both clients (auth + main) in NetworkModule, active only in release builds via BuildConfig.DEBUG guard using BuildConfig.CERT_PIN_SHA256 pin source**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-15T13:17:00Z
- **Completed:** 2026-03-15T13:20:00Z
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments
- Added `buildCertificatePinner()` private helper that returns null in DEBUG, CertificatePinner in release
- Applied pinner to `provideAuthApiService` (pre-auth OkHttpClient) — covers login/register traffic
- Applied pinner to `provideOkHttpClient` (authenticated OkHttpClient) — covers all API traffic post-login
- TileCacheWorker's OkHttp client unchanged — OSM tile downloads unaffected
- Pin source is `BuildConfig.CERT_PIN_SHA256` — no hardcoded string in source

## Task Commits

Each task was committed atomically:

1. **Task 1: Add CertificatePinner to both OkHttp clients in NetworkModule** - `d6bb658` (feat)

**Plan metadata:** (docs commit follows)

## Files Created/Modified
- `android/app/src/main/java/com/roadrunner/app/di/NetworkModule.kt` - Added CertificatePinner import, buildCertificatePinner() helper, and pinner applied to both OkHttp client providers

## Decisions Made
- `buildCertificatePinner()` as a private helper with nullable return type keeps both client providers DRY and the DEBUG guard in one place
- provideAuthApiService converted from expression body to block body to accommodate the builder pattern
- TileCacheWorker intentionally excluded from pinning per plan spec — tile servers are not the Cloudflare Tunnel

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

Before release, the operator must:
1. Replace `*.trycloudflare.com` hostname in `buildCertificatePinner()` with the actual stable Cloudflare Tunnel hostname
2. Ensure `CERT_PIN_SHA256` in `build.gradle.kts` release block contains the correct SHA-256 certificate pin for that hostname

## Next Phase Readiness
- Certificate pinning complete; release builds will reject MITM proxies that lack the pinned certificate
- Debug builds unaffected — emulator and local dev traffic pass through normally
- Proceed to Plan 04 (next plan in phase 07)

---
*Phase: 07-security-hardening-and-flavor-scaffold*
*Completed: 2026-03-15*
