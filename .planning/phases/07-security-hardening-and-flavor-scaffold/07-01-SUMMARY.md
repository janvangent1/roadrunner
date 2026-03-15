---
phase: 07-security-hardening-and-flavor-scaffold
plan: 01
subsystem: infra
tags: [android, r8, proguard, flavors, security, tink, osmdroid, retrofit]

# Dependency graph
requires:
  - phase: 06-navigation-experience
    provides: Final navigation feature set; all Android source files stable before hardening
provides:
  - R8 minification enabled for release builds with resource shrinking
  - ProGuard keep rules for Tink, OSMDroid, DTOs, Retrofit; Log.* stripped from release
  - motorcycle flavor with 'brand' dimension and branding strings source set
  - sportscar flavor stub (build.gradle.kts only; no source set required)
  - CERT_PIN_SHA256 buildConfigField in debug and release buildTypes (placeholder for operator to fill)
  - Play Integrity API dependency
affects:
  - Future flavor additions (follow brand dimension pattern)
  - Release signing and Play Store publishing (operator must replace PLACEHOLDER_PIN_SHA256)

# Tech tracking
tech-stack:
  added:
    - "com.google.android.play:integrity:1.4.0 (Play Integrity API)"
  patterns:
    - "R8 minification: isMinifyEnabled=true + isShrinkResources=true in release buildType"
    - "ProGuard assumenosideeffects: strips Log.* from release APK to prevent logcat leaks"
    - "Flavor dimension named 'brand': motorcycle and sportscar share the same dimension"
    - "Flavor source sets: motorcycle has strings.xml override; sportscar has no source set (flavor defined only in build.gradle.kts)"

key-files:
  created:
    - "android/app/src/motorcycle/res/values/strings.xml"
  modified:
    - "android/app/build.gradle.kts"
    - "android/app/proguard-rules.pro"

key-decisions:
  - "flavorDimension renamed from 'variant' to 'brand' to semantically reflect multi-brand architecture"
  - "sportscar flavor defined in build.gradle.kts only, no source set — proves extensibility with zero architectural rework"
  - "CERT_PIN_SHA256 is a placeholder buildConfigField; operator must replace before Play Store publish"
  - "motorcycle applicationId set to 'com.roadrunner.app' (not suffix-based) for explicitness"

patterns-established:
  - "Flavor-specific strings override: place strings.xml under android/app/src/{flavorName}/res/values/"
  - "All ProGuard keep rules scoped under android/app/proguard-rules.pro (not default file)"

requirements-completed: [ARCH-FLAVOR-01, ARCH-R8-01]

# Metrics
duration: 2min
completed: 2026-03-15
---

# Phase 7 Plan 01: Security Hardening and Flavor Scaffold Summary

**R8 minification enabled for release with Log-stripping ProGuard rules; motorcycle+sportscar dual-brand flavor scaffold using 'brand' dimension with motorcycle branding strings source set**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-15T13:25:29Z
- **Completed:** 2026-03-15T13:27:00Z
- **Tasks:** 2
- **Files modified:** 3 (build.gradle.kts, proguard-rules.pro, strings.xml created)

## Accomplishments
- Release buildType hardened: `isMinifyEnabled = true`, `isShrinkResources = true`, proguard-rules.pro wired in
- ProGuard keep rules written for Tink, OSMDroid, Retrofit DTOs; `assumenosideeffects` strips all Log.* calls from release APK preventing logcat leaks
- `flavorDimensions` renamed from `variant` to `brand`; motorcycle flavor updated with explicit applicationId; sportscar flavor added as stub (proves dual-brand extensibility requires no source rework)
- `CERT_PIN_SHA256` buildConfigField added to debug and release — operator replaces placeholder before Play Store publish
- Play Integrity API dependency added

## Task Commits

Each task was committed atomically:

1. **Task 1: Enable R8, add CERT_PIN_SHA256 buildConfigField, update flavor definitions** - `a0102ff` (feat)
2. **Task 2: Write ProGuard rules and create motorcycle branding source set** - `16bc42e` (feat)

## Files Created/Modified
- `android/app/build.gradle.kts` - flavorDimensions='brand', motorcycle+sportscar flavors, isMinifyEnabled=true, isShrinkResources=true, CERT_PIN_SHA256 field, Play Integrity dep
- `android/app/proguard-rules.pro` - Keep rules for Tink/OSMDroid/DTOs/Retrofit; assumenosideeffects Log.* strip rule
- `android/app/src/motorcycle/res/values/strings.xml` - app_name = "Roadrunner Moto" (motorcycle flavor branding)

## Decisions Made
- `flavorDimension` renamed from `variant` to `brand` to accurately reflect the multi-brand architecture (motorcycle vs. sportscar)
- `sportscar` flavor stub defined only in build.gradle.kts with no source set — demonstrates that adding a new vehicle-type brand requires zero file structure changes beyond build config
- `CERT_PIN_SHA256` set to placeholder string in both debug and release; real SHA-256 certificate fingerprint must be substituted by operator before production Play Store submission
- `motorcycle` uses explicit `applicationId = "com.roadrunner.app"` instead of `applicationIdSuffix` for clarity

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

**Operator action required before Play Store publish:**
- Replace `PLACEHOLDER_PIN_SHA256` with the actual SHA-256 certificate fingerprint in both `debug` and `release` buildTypes in `android/app/build.gradle.kts`
- This value is used for certificate pinning (CERT_PIN_SHA256 buildConfigField); using the placeholder in production will cause all certificate pin checks to fail

## Next Phase Readiness
- R8 and ProGuard baseline established; release APK will be minified and Log-stripped on next release build
- Dual-flavor scaffold ready; any future sportscar-specific UI/strings can be added to `android/app/src/sportscar/` without touching shared code
- Play Integrity API dependency in place for Phase 7 attestation implementation

## Self-Check: PASSED

All created/modified files found on disk. All task commits verified in git log.

---
*Phase: 07-security-hardening-and-flavor-scaffold*
*Completed: 2026-03-15*
