---
phase: 4
title: Android Encryption Layer
status: ready
---

# Phase 4 Context: Android Encryption Layer

## Decisions

### Encryption approach (locked from research)
- **Library:** Google Tink `tink-android:1.15.0` (already added to build.gradle.kts in Phase 3)
- **Primitive:** `StreamingAead` with `AES256_GCM_HKDF_4KB` — chunks large GPX files, works without loading entire file into RAM
- **Key storage:** `AndroidKeysetManager` backed by Android Keystore (same pattern as TokenStorage from Phase 3)
- **Keyset name:** `roadrunner_gpx_keyset` stored in `EncryptedSharedPreferences`-equivalent managed by Tink's `AndroidKeysetManager`

### Storage
- Encrypted GPX files stored in `context.filesDir/gpx/<routeId>.enc`
- No other location — NOT external storage, NOT cache dir
- File never written as plaintext at any point

### Decrypt-at-render flow
1. `RouteRepository.downloadAndStoreGpx(routeId)` — downloads encrypted blob from `GET /routes/:id/gpx`, writes to `filesDir/gpx/<routeId>.enc` as-is (already Tink-encrypted by server)
2. At navigation render time: `GpxCryptoManager.decryptToByteArray(routeId)` — decrypts in memory, returns `ByteArray`
3. `ByteArray` is parsed by GPX parser (GPXParser library) into track points
4. Track points passed directly to OSMDroid overlay — no intermediate file

### GPX parsing library
- `io.ticofab.android-gpx-parser:android-gpx-parser:2.3.0` — Kotlin-friendly, parses `InputStream`
- Decrypt → `ByteArrayInputStream` → parse — never touches filesystem as plaintext

### Key management
- Server uses one global Tink keyset (AES-256-GCM) to encrypt all GPX files on upload
- Client needs to decrypt what the server encrypted
- **Decision:** Client holds its own Tink keyset (generated once, stored in Android Keystore). BUT the server encryption and client decryption must use the SAME keyset — which means the client keyset must be seeded from the server's keyset.
- **Practical approach for v1:** The server's Tink keyset JSON (base64) is embedded as a BuildConfig constant (`BuildConfig.TINK_KEYSET_B64`) set in `build.gradle.kts` under the `debug` buildType. The client deserializes this keyset on first launch, wraps it with Android Keystore, and stores it. This avoids a key-exchange endpoint for v1.
- For v2: replace with a proper key delivery endpoint authenticated by the license check session token.

### No export mechanism
- No `share` intent for GPX files
- No `FileProvider` registration for GPX files
- No API endpoint to retrieve decrypted GPX

### What this phase delivers
- `GpxCryptoManager` — wraps Tink keyset management + decrypt-to-ByteArray
- `RouteRepository` extension — `downloadAndStoreGpx(routeId)` + `getDecryptedGpx(routeId): ByteArray`
- `OsmPreviewMap` update — replace placeholder map with real GPX polyline overlay (Phase 3 had empty map)
- PROT-01, PROT-02, PROT-03 satisfied

## Deferred
- Per-route or per-license-session key delivery (Phase 7 / v2)
- Key rotation
- Memory wiping after decode (nice to have, complex)
