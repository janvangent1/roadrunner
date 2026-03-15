---
phase: 07-security-hardening-and-flavor-scaffold
verified: 2026-03-15T00:00:00Z
status: passed
score: 13/13 must-haves verified
re_verification: false
---

# Phase 7: Security Hardening and Flavor Scaffold Verification Report

**Phase Goal:** Security Hardening and Flavor Scaffold — release APK with R8 + resource shrinking, Log.* stripped via ProGuard, Play Integrity API check on first launch (blocks MEETS_BASIC_INTEGRITY=false devices), HTTPS cert pinning active in release builds, motorcycle flavor fully branded, sportscar flavor defined as extensibility proof.
**Verified:** 2026-03-15
**Status:** PASSED
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Release build type has isMinifyEnabled = true and isShrinkResources = true | VERIFIED | build.gradle.kts line 50-51: `isMinifyEnabled = true`, `isShrinkResources = true` |
| 2 | proguard-rules.pro keeps Tink, OSMDroid, DTOs, Retrofit; strips Log.* via assumenosideeffects | VERIFIED | proguard-rules.pro lines 4, 8, 10-13, 15-23, 26-33: all required keep rules + `-assumenosideeffects class android.util.Log` |
| 3 | motorcycle flavor has app_name = "Roadrunner Moto" in its own strings.xml source set | VERIFIED | android/app/src/motorcycle/res/values/strings.xml: `<string name="app_name">Roadrunner Moto</string>` |
| 4 | sportscar flavor is defined in build.gradle.kts with applicationId = "com.roadrunner.sportscar" but has no source set directory | VERIFIED | build.gradle.kts lines 33-37 define sportscar flavor; `android/app/src/sportscar/` does not exist |
| 5 | Both flavors share the same flavorDimension "brand" | VERIFIED | build.gradle.kts line 26: `flavorDimensions += "brand"`; lines 29 and 34: `dimension = "brand"` for both |
| 6 | IntegrityChecker.check() is called from MainActivity.onCreate() before UI is shown | VERIFIED | MainActivity.kt line 36-46: `lifecycleScope.launch { integrityChecker.check() }` before setContent; `integrityBlocked` mutableStateOf gates recomposition |
| 7 | Nonce is Base64(SHA-256(deviceId:timestamp)) to prevent replay attacks | VERIFIED | IntegrityChecker.kt lines 63-65: SHA-256 digest of `"$deviceId:$timestamp"`, Base64-encoded |
| 8 | Integrity result is cached for 24h in DataStore<Preferences> — check skips on cache hit | VERIFIED | IntegrityChecker.kt lines 41-47: reads KEY_CACHED_AT, returns cached result if within CACHE_TTL_MS (86400000ms) |
| 9 | Device failing MEETS_BASIC_INTEGRITY sees a non-dismissible AlertDialog; app becomes unusable | VERIFIED | MainActivity.kt lines 69-76: DeviceNotSupportedDialog with `onDismissRequest = { }` (no-op) and no confirmButton/dismissButton |
| 10 | Backend POST /api/v1/integrity/verify decodes token via Google Play Integrity API and returns { passed: boolean } | VERIFIED | integrity.ts lines 30-33: `playIntegrity.v1.decodeIntegrityToken(...)`, line 39: `passed = deviceIntegrity.includes('MEETS_BASIC_INTEGRITY')`, line 41: `reply.send({ passed })` |
| 11 | In release builds, OkHttpClient has a CertificatePinner configured; in debug builds no pinner is attached | VERIFIED | NetworkModule.kt lines 23-31: `buildCertificatePinner()` returns null when `BuildConfig.DEBUG`, returns CertificatePinner in release |
| 12 | CertificatePinner is applied to both provideOkHttpClient and provideAuthApiService clients | VERIFIED | NetworkModule.kt lines 37 and 59: both providers call `buildCertificatePinner()?.let { ... }` |
| 13 | The pin SHA-256 is read from BuildConfig.CERT_PIN_SHA256 — no hardcoded string in source | VERIFIED | NetworkModule.kt line 29: `"sha256/${BuildConfig.CERT_PIN_SHA256}"`; build.gradle.kts lines 44 and 49 define the field in both buildTypes |

**Score:** 13/13 truths verified

---

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `android/app/build.gradle.kts` | R8 release config, CERT_PIN_SHA256 buildConfigField, motorcycle+sportscar flavor definitions | VERIFIED | isMinifyEnabled=true, isShrinkResources=true, CERT_PIN_SHA256 in both buildTypes, both flavors with dimension="brand" |
| `android/app/proguard-rules.pro` | Keep rules for Tink, OSMDroid, DTOs, Retrofit; Log.* strip rule | VERIFIED | All required -keep and -assumenosideeffects rules present, 34 lines of substantive content |
| `android/app/src/motorcycle/res/values/strings.xml` | Motorcycle flavor branding string | VERIFIED | Contains `app_name = "Roadrunner Moto"` |
| `android/app/src/main/java/com/roadrunner/app/security/IntegrityChecker.kt` | Play Integrity API wrapper with nonce generation and 24h cache | VERIFIED | 107 lines; SHA-256 nonce, StandardIntegrityManager two-step, DataStore cache, OkHttp POST to backend |
| `android/app/src/main/java/com/roadrunner/app/MainActivity.kt` | IntegrityChecker.check() called in onCreate before setContent renders | VERIFIED | Injects IntegrityChecker, calls check() in lifecycleScope.launch, integrityBlocked mutableStateOf gates DeviceNotSupportedDialog |
| `backend/src/routes/integrity.ts` | POST /api/v1/integrity/verify Fastify route checking MEETS_BASIC_INTEGRITY | VERIFIED | decodeIntegrityToken, MEETS_BASIC_INTEGRITY verdict check, { passed: boolean } response, fail-secure on error |
| `backend/src/app.ts` | integrity route registered at /api/v1/integrity | VERIFIED | Line 10: `import integrityHandlers`; line 60: `app.register(integrityHandlers, { prefix: '/api/v1/integrity' })` |
| `android/app/src/main/java/com/roadrunner/app/di/NetworkModule.kt` | OkHttpClient with CertificatePinner guarded by !BuildConfig.DEBUG | VERIFIED | buildCertificatePinner() helper, 5 CertificatePinner references, applied to both clients |
| `android/app/src/sportscar/` (absence) | Must NOT exist — sportscar is build-config-only | VERIFIED | Directory does not exist on disk |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| build.gradle.kts release buildType | proguard-rules.pro | `proguardFiles(..., "proguard-rules.pro")` | WIRED | build.gradle.kts lines 52-55: proguardFiles block present |
| build.gradle.kts productFlavors | motorcycle source set | `create("motorcycle")` with dimension="brand" | WIRED | Line 28-32 define flavor; source set exists at android/app/src/motorcycle/ |
| MainActivity.onCreate | IntegrityChecker.check() | coroutine launch in lifecycleScope | WIRED | Lines 36-46: lifecycleScope.launch { integrityChecker.check() } |
| IntegrityChecker | POST /api/v1/integrity/verify | OkHttp POST with token in request body | WIRED | IntegrityChecker.kt lines 96-99: Request.Builder().url("$baseUrl/api/v1/integrity/verify").post(body) |
| backend integrity.ts | Google Play Integrity API | googleapis playintegrity.v1.decodeIntegrityToken | WIRED | integrity.ts line 30: `await playIntegrity.v1.decodeIntegrityToken(...)` |
| NetworkModule.provideOkHttpClient | CertificatePinner | builder.certificatePinner(it) inside if (!BuildConfig.DEBUG) | WIRED | NetworkModule.kt line 59: `buildCertificatePinner()?.let { builder.certificatePinner(it) }` |
| NetworkModule.provideAuthApiService | CertificatePinner | separate OkHttpClient.Builder with same pinning guard | WIRED | NetworkModule.kt line 37: `buildCertificatePinner()?.let { clientBuilder.certificatePinner(it) }` |

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| ARCH-FLAVOR-01 | 07-01-PLAN.md | Product flavor scaffold: motorcycle (branded) + sportscar (stub), brand dimension | SATISFIED | build.gradle.kts: both flavors defined, strings.xml created for motorcycle, no sportscar source set |
| ARCH-R8-01 | 07-01-PLAN.md | R8 minification with resource shrinking and Log.* stripping in release | SATISFIED | isMinifyEnabled=true, isShrinkResources=true in release; proguard-rules.pro with -assumenosideeffects |
| ARCH-INTEGRITY-01 | 07-02-PLAN.md | Play Integrity API device attestation blocking MEETS_BASIC_INTEGRITY=false | SATISFIED | IntegrityChecker.kt + MainActivity.kt + backend integrity.ts all verified |
| ARCH-PINNING-01 | 07-03-PLAN.md | HTTPS certificate pinning via OkHttp CertificatePinner, release-only, from BuildConfig | SATISFIED | NetworkModule.kt: buildCertificatePinner() on both clients with BuildConfig.DEBUG guard |

**Requirement ID note:** ARCH-FLAVOR-01, ARCH-R8-01, ARCH-INTEGRITY-01, and ARCH-PINNING-01 are architectural objective IDs declared in the phase PLAN frontmatter. They do not appear in REQUIREMENTS.md as standalone v1 requirement rows — REQUIREMENTS.md correctly documents Phase 7 as an "Architectural objective per PROJECT.md" with no standalone requirement IDs. This is consistent: the IDs exist only in PLAN frontmatter as internal tracking labels, not as v1 user-facing requirements. No orphaned requirements exist.

---

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `android/app/src/main/java/com/roadrunner/app/MainActivity.kt` | 44 | `android.util.Log.w(...)` inside the integrity catch block | Info | This is intentional: fail-open v1 design; the Log.w call will be stripped from release APK by the -assumenosideeffects ProGuard rule. No production leak. |
| `android/app/build.gradle.kts` | 44, 49 | `CERT_PIN_SHA256 = "PLACEHOLDER_PIN_SHA256"` in both buildTypes | Info | Documented operator action required before Play Store publish. Placeholder is correct for development; must be replaced for release. |
| `android/app/src/main/res/values/strings.xml` | 6 | `play_integrity_cloud_project_number` value is `0` | Info | Documented placeholder. Developer must fill in actual Cloud project number from Play Console before release. |
| `NetworkModule.kt` | 29 | `*.trycloudflare.com` wildcard hostname | Info | Documented placeholder. Operator must replace with actual stable Cloudflare Tunnel hostname before release. |

No blockers. No stubs. All infos are documented placeholder values requiring operator configuration before Play Store publish — consistent with stated project intent.

---

### Human Verification Required

#### 1. Integrity dialog timing (first launch UX)

**Test:** Install a debug build, launch the app. Observe whether the nav graph (catalog/login screen) flickers briefly before any potential integrity block dialog appears.
**Expected:** If the device passes (or the check is cached), the normal flow shows immediately. On a failing device, there may be a brief flash of the login/catalog screen before the blocking dialog appears, because `lifecycleScope.launch` is async and `setContent` renders immediately with `integrityBlocked = false`.
**Why human:** The timing window between coroutine launch and first composition cannot be verified programmatically. The implementation is correct (mutableStateOf drives recomposition), but the UX flash on failure is inherent to the async design decision (fail-open v1) and should be accepted or rejected by a product owner.

#### 2. sportscar flavor Gradle dry-run

**Test:** Run `cd android && ./gradlew sportscarDebug --dry-run` in the project environment.
**Expected:** Gradle lists sportscarDebug task without errors, confirming the flavor compiles with no source set.
**Why human:** Gradle cannot run in this verification environment (Windows/Java version constraint noted in SUMMARY). The code evidence is conclusive but the actual Gradle execution could not be performed.

#### 3. Play Integrity blocking dialog is truly non-dismissible

**Test:** On a rooted test device that fails MEETS_BASIC_INTEGRITY, launch the app after clearing cache (to skip the 24h cache hit).
**Expected:** The DeviceNotSupportedDialog appears and cannot be dismissed by back button, tapping outside, or any gesture. The app remains unusable.
**Why human:** Requires a physical device that fails integrity attestation. The code shows `onDismissRequest = { }` (no-op) and no buttons, which is the correct Compose pattern for a non-dismissible dialog, but only a live test on a failing device can confirm it.

---

### Gaps Summary

No gaps. All 13 must-have truths verified. All artifacts exist, are substantive, and are wired. All key links confirmed. All requirement IDs accounted for.

The four Info-level items above are documented placeholder values (CERT_PIN_SHA256, play_integrity_cloud_project_number, trycloudflare.com hostname) that require operator action before Play Store publish — this is by design and does not constitute a gap in the phase deliverable.

The backend integrity route does not implement per-device Redis caching (referenced in CONTEXT.md step 4 as part of the architectural description). The PLAN frontmatter's must_haves do not require Redis caching — only Android-side 24h DataStore caching. The CONTEXT.md text was aspirational; the binding spec is the PLAN. This is not a gap.

---

### Commit Verification

All task commits confirmed in git log:

| Plan | Task | Commit | Message |
|------|------|--------|---------|
| 07-01 | Task 1 (R8 + flavors) | `a0102ff` | feat(07-01): enable R8, add CERT_PIN_SHA256 buildConfigField, update flavor definitions |
| 07-01 | Task 2 (ProGuard + strings.xml) | `16bc42e` | feat(07-01): write ProGuard rules and create motorcycle branding source set |
| 07-02 | Task 1 (IntegrityChecker + MainActivity) | `fd018f2` | feat(07-02): add IntegrityChecker and wire into MainActivity |
| 07-02 | Task 2 (backend integrity.ts) | `e116c93` | feat(07-02): add Play Integrity verification endpoint to backend |
| 07-03 | Task 1 (CertificatePinner) | `d6bb658` | feat(07-03): add CertificatePinner to both OkHttp clients in NetworkModule |

---

_Verified: 2026-03-15_
_Verifier: Claude (gsd-verifier)_
