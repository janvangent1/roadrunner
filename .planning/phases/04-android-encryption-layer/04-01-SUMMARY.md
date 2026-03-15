---
phase: 04-android-encryption-layer
plan: "01"
subsystem: crypto
tags: [tink, streamingaead, android-keystore, gpx, encryption, hilt]

# Dependency graph
requires:
  - phase: 01-backend-foundation
    provides: Server encrypts GPX with Tink StreamingAead using routeId as AAD; TINK_KEYSET_JSON is base64-encoded binary keyset
  - phase: 03-android-catalog-and-auth
    provides: Hilt DI setup, build.gradle.kts with Tink 1.15.0 dependency
provides:
  - GpxCryptoManager singleton with decryptToByteArray(routeId, encFile) and encryptedFileExists(routeId, filesDir)
  - android-gpx-parser:2.3.0 dependency available to compile against
  - TINK_KEYSET_B64 debug buildConfigField for developer to populate with server keyset
affects:
  - 04-android-encryption-layer (remaining plans using GpxCryptoManager for GPX decryption and route display)

# Tech tracking
tech-stack:
  added:
    - io.ticofab.android-gpx-parser:android-gpx-parser:2.3.0
    - com.google.crypto.tink.StreamingAead (API surface, library already present)
    - com.google.crypto.tink.streamingaead.StreamingAeadConfig
    - com.google.crypto.tink.BinaryKeysetReader
    - com.google.crypto.tink.CleartextKeysetHandle
    - com.google.crypto.tink.JsonKeysetWriter (for seeding serialization)
  patterns:
    - Server keyset seeded into SharedPrefs via BinaryKeysetReader on first launch, then wrapped by AndroidKeysetManager with hardware-backed master key
    - StreamingAeadConfig.register() called before keyset access (not AeadConfig)
    - routeId.toByteArray() as AAD matches server Buffer.from(routeId) encoding
    - In-memory decryption via newDecryptingStream().use { it.readBytes() } — no plaintext written to disk

key-files:
  created:
    - android/app/src/main/java/com/roadrunner/app/crypto/GpxCryptoManager.kt
  modified:
    - android/app/build.gradle.kts

key-decisions:
  - "Keyset seeding: deserialize server binary keyset with BinaryKeysetReader + CleartextKeysetHandle, serialize to JSON via JsonKeysetWriter, store in SharedPrefs before AndroidKeysetManager loads — enables hardware wrapping of server keyset on first launch"
  - "TINK_KEYSET_B64 is debug-only buildConfigField; release key delivery deferred to Phase 7 (v2); developer must replace placeholder with base64-encoded binary keyset from server TINK_KEYSET_JSON env var"
  - "StreamingAeadConfig.register() used (not AeadConfig.register()) because StreamingAead is a distinct primitive family in Tink"

patterns-established:
  - "Crypto package pattern: com.roadrunner.app.crypto contains all Tink-based crypto managers as Hilt singletons"
  - "GPX file convention: encrypted files stored at filesDir/gpx/{routeId}.enc"
  - "AAD convention: routeId.toByteArray() (UTF-8) matches server-side Buffer.from(routeId)"

requirements-completed: [PROT-01, PROT-02]

# Metrics
duration: 2min
completed: 2026-03-15
---

# Phase 4 Plan 01: Android Encryption Layer — Crypto Foundation Summary

**Tink StreamingAead GpxCryptoManager singleton seeding server keyset from BuildConfig via BinaryKeysetReader, wrapped by Android Keystore master key, exposing in-memory GPX decryption with routeId as AAD**

## Performance

- **Duration:** ~2 min
- **Started:** 2026-03-15T07:35:08Z
- **Completed:** 2026-03-15T07:36:35Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Added `android-gpx-parser:2.3.0` dependency to build.gradle.kts for GPX parsing after in-memory decryption
- Added `TINK_KEYSET_B64` debug-only buildConfigField (placeholder) connecting server keyset to the Android build
- Created `GpxCryptoManager.kt` as a Hilt `@Singleton` in new `crypto/` package — seeds the Tink StreamingAead keyset from `BuildConfig.TINK_KEYSET_B64` on first launch using `BinaryKeysetReader`, wraps it with Android Keystore master key via `AndroidKeysetManager`, and exposes `decryptToByteArray()` and `encryptedFileExists()`

## Task Commits

Each task was committed atomically:

1. **Task 1: Add android-gpx-parser dependency and TINK_KEYSET_B64 buildConfigField** - `80756d9` (feat)
2. **Task 2: Create GpxCryptoManager with StreamingAead keyset seeding** - `0237750` (feat)

## Files Created/Modified

- `android/app/build.gradle.kts` - Added android-gpx-parser:2.3.0 dependency; added TINK_KEYSET_B64 buildConfigField in debug buildType only
- `android/app/src/main/java/com/roadrunner/app/crypto/GpxCryptoManager.kt` - New Hilt singleton; seeds Tink StreamingAead keyset from BuildConfig on first launch; exposes `decryptToByteArray(routeId, encFile)` and `encryptedFileExists(routeId, filesDir)`

## Decisions Made

- **Keyset seeding approach:** Since `AndroidKeysetManager` does not expose a direct import-existing-keyset API, the server keyset is decoded from `BuildConfig.TINK_KEYSET_B64` using `BinaryKeysetReader` + `CleartextKeysetHandle.read()`, then serialized to JSON via `JsonKeysetWriter` and stored into the SharedPrefs entry that `AndroidKeysetManager` will subsequently load and wrap with the hardware-backed Keystore master key.
- **TINK_KEYSET_B64 is debug-only:** The release path for key delivery is deferred to Phase 7 (v2 key delivery). Developer must replace the placeholder value with the base64-encoded binary keyset exported from the server's `TINK_KEYSET_JSON` environment variable.
- **StreamingAeadConfig vs AeadConfig:** `StreamingAeadConfig.register()` is required because StreamingAead primitives belong to a distinct Tink key type family; using `AeadConfig` would not register `AES256_GCM_HKDF_4KB`.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

**Developer must replace the TINK_KEYSET_B64 placeholder before the app can decrypt GPX files.**

1. On the server, export the keyset: the `TINK_KEYSET_JSON` env var contains a base64-encoded binary Tink keyset.
2. Copy that base64 string into `android/app/build.gradle.kts` debug buildConfigField:
   ```kotlin
   buildConfigField("String", "TINK_KEYSET_B64", "\"<paste-base64-keyset-here>\"")
   ```
3. Rebuild the debug APK — `GpxCryptoManager.init()` will seed the keyset on first launch.

## Next Phase Readiness

- `GpxCryptoManager` is ready for injection into any ViewModel or Repository that needs GPX decryption
- `android-gpx-parser:2.3.0` is available as a compile dependency for parsing decrypted GPX bytes
- The `crypto/` package is established as the home for Tink-based crypto managers
- Remaining Phase 4 plans (GPX download, overlay rendering) can inject `GpxCryptoManager` directly

---
*Phase: 04-android-encryption-layer*
*Completed: 2026-03-15*
