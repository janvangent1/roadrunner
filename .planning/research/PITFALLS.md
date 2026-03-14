# Pitfalls Research

**Domain:** Android navigation app with encrypted/licensed content (offroad motorcycle)
**Researched:** 2026-03-14
**Confidence:** HIGH (security pitfalls), MEDIUM (UX/navigation pitfalls), HIGH (billing pitfalls)

---

## Critical Pitfalls

### Pitfall 1: Storing Decryption Keys in App Memory Too Long (GPX Key Exposure)

**What goes wrong:**
The AES key used to decrypt GPX data is loaded once and kept in a static field or long-lived ViewModel. An attacker with a rooted device or adb root access can take a process memory dump and recover the key. The attacker then has permanent access to all route data for that key, even after license revocation.

**Why it happens:**
Developers focus on "GPX file is encrypted at rest" and consider the job done. The runtime key is treated as an implementation detail rather than a secret to protect. Keys held in long-lived Java/Kotlin objects sit in GC-managed heap and are not zeroed between uses.

**How to avoid:**
- Use the Android Keystore (hardware-backed TEE) to wrap the per-route content key. The raw key material never leaves the TEE. Use `KeyStore.getInstance("AndroidKeyStore")` and generate a `KEY_ALGORITHM_AES` key with `setIsStrongBoxBacked(true)` on supported devices.
- For decryption operations, pass ciphertext directly to a `Cipher` backed by the Keystore key — the plaintext GPX coordinates are produced only in memory, never on disk.
- Zero out byte arrays holding plaintext route data immediately after the rendering pass. Use `Arrays.fill(buffer, (byte) 0)` and avoid `String` (immutable, not clearable) for route coordinate data.
- Do not cache decrypted route data in any shared or global object. Decrypt per-render pass.

**Warning signs:**
- A static field or singleton holds the decryption key or decrypted route coordinates.
- GPX plaintext is converted to `String` at any point.
- Tests decrypt the file once and reuse results across multiple test cases without re-decrypting.

**Phase to address:**
Content encryption and key management phase (early — before any route data is loaded into the app).

---

### Pitfall 2: Client-Only License Checks Are Trivially Bypassed

**What goes wrong:**
The app checks `sharedPreferences.getBoolean("licenseValid", false)` or inspects a locally-cached token to gate navigation. An attacker patches the APK (using apktool + smali edit) or hooks the check with Frida to return `true`, and navigates any route for free permanently.

**Why it happens:**
Server-side validation feels like over-engineering for v1. Developers defer it with "we'll add it later." It is never added later.

**How to avoid:**
- Every navigation session start must hit the server license endpoint. Accept a short-lived signed JWT (30-60 minute TTL) in the response. The app checks this token's expiry, not a persisted boolean.
- The server must verify: (a) the Google Play purchase token via the Play Developer API, (b) that the token belongs to the authenticated user, (c) that the license period covers the current timestamp.
- The 1-hour grace period for active sessions (per PROJECT.md) should be implemented as: navigation can *continue* for 60 minutes without re-validation, but navigation cannot *start* without a fresh server check.
- Never put the license state in SharedPreferences as a plain boolean. If local caching is needed, store the full signed JWT and validate its signature and TTL on every check.

**Warning signs:**
- License validation runs only at app install or login, not at navigation start.
- `if (isLicensed)` checks reference any field set by local code rather than a server-verified token.
- The server endpoint is implemented but the client also has a fallback path that bypasses it.

**Phase to address:**
License enforcement and server-side validation phase — must be complete before any beta distribution.

---

### Pitfall 3: GPX Data Written to World-Readable Storage at Any Point

**What goes wrong:**
During download, the encrypted GPX blob is written to `Environment.getExternalStorageDirectory()` or the Downloads folder so "it's easier to manage." Even though it's encrypted, users with a file manager can copy the blob. If the per-route encryption key is ever derivable from a static value (e.g., package name + route ID), the blob is recoverable.

**Why it happens:**
External storage is the path of least resistance. The encryption provides a false sense of security that makes the storage location feel irrelevant.

**How to avoid:**
- Store all encrypted GPX blobs exclusively in `context.getFilesDir()` or `context.getCacheDir()` — app-private directories not accessible to other apps or users without root.
- Never request `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE` permissions. Their absence signals intent.
- Use per-route, per-device content keys (derived from a server-issued secret + device-bound Android Keystore key). A stolen ciphertext blob from one device is useless on another.

**Warning signs:**
- The app's `AndroidManifest.xml` requests any external storage permission.
- Encrypted files appear in `/sdcard/` or any path a third-party file manager can list.
- The encryption key derivation includes only static, predictable values.

**Phase to address:**
Content storage and download phase — the storage path must be locked down before route download is implemented.

---

### Pitfall 4: linkedPurchaseToken Not Invalidated on Subscription Change

**What goes wrong:**
A user purchases a day pass (token A), lets it expire, then purchases a new day pass (token B). The server only tracks token B as active. The user discovers they can still call the server with token A (because Google's API reports both as "active" simultaneously for a brief overlap window), and gains duplicate access. Over time, the purchase token table grows with zombie tokens that grant unintended access.

**Why it happens:**
The Google Play Billing `linkedPurchaseToken` field is not prominently documented in the quick-start guides. Developers implement basic purchase flow without reading the subscription lifecycle edge cases.

**How to avoid:**
- On every purchase verification server call, check if the `linkedPurchaseToken` field is set in the purchase response.
- If set, immediately mark the old token as revoked in the database and deny any future license checks using it.
- This applies to all purchase types: upgrades, downgrades, renewals after expiry, and plan changes.
- Store purchase token hash (not the raw token) plus its validity state, creation timestamp, and linked token reference in the server DB.

**Warning signs:**
- The server purchase table has no `revoked_at` or `replaced_by_token` column.
- License checks only query by `user_id` without checking token validity state.
- Integration tests do not cover the upgrade/renewal purchase flow.

**Phase to address:**
Google Play Billing integration phase — must be in the server schema from the start, not retrofitted.

---

### Pitfall 5: Offline Navigation Fails or Blocks Because of a Hard Network Requirement

**What goes wrong:**
The app requires a live server license check to start or continue navigation. A rider enters an area with no cell signal mid-session, the session JWT expires, the app attempts re-validation, fails, and terminates navigation. The rider is now lost on a trail with no map.

**Why it happens:**
The security requirement ("server-side license validation") is implemented as a synchronous gate on every navigation tick. The developer correctly wants to prevent expired licenses but does not account for the physical reality of offroad environments.

**How to avoid:**
- Validate the license before the session starts (while the user still has connectivity at the trailhead).
- Issue a session token with a generous but bounded TTL (the PROJECT.md-specified 1-hour grace period is appropriate).
- During an active session, cache the session token locally and do not re-validate until the TTL expires.
- On TTL expiry, attempt re-validation. If the network is unavailable, apply the grace period extension silently. Show a non-blocking UI indicator ("License expires in 45 min — connect to extend").
- Only block navigation start (cold start), never terminate a running navigation session silently.

**Warning signs:**
- A network call is in the critical path of the navigation rendering loop.
- No `ConnectivityManager` check exists before license re-validation.
- The app shows a blocking dialog when re-validation fails during active navigation.

**Phase to address:**
Navigation launch and offline mode phase.

---

### Pitfall 6: OSMDroid Tile Cache Stored at a Path That Breaks on Android 13+

**What goes wrong:**
OSMDroid's default tile cache path targets external storage (`/sdcard/osmdroid/`). On Android 13+ (targetSdk 33+), scoped storage enforcement means this path is either inaccessible without the deprecated `READ_MEDIA_IMAGES` permission or silently fails. The map renders as a grey grid of empty tiles. The problem only surfaces on production devices — emulators often have more permissive storage setups.

**Why it happens:**
OSMDroid predates scoped storage. Older tutorials and documentation show external storage configurations that no longer work. The library does fall back to app-private storage but requires explicit configuration that is not the default.

**How to avoid:**
- At app startup, explicitly set: `Configuration.getInstance().setOsmdroidBasePath(context.getFilesDir())` and `Configuration.getInstance().setOsmdroidTileCache(new File(context.getCacheDir(), "osmdroid_tiles"))`.
- Do not request any external storage permissions. Use only app-private storage for tiles.
- Test tile loading on a physical Android 13+ device with no external storage permissions granted, not only on an emulator.

**Warning signs:**
- The app's manifest includes `READ_MEDIA_IMAGES` or legacy storage flags.
- Tile loading works in the emulator but fails on a clean physical device.
- Map shows tiles in development but grey squares in production.

**Phase to address:**
Map and navigation setup phase — configure storage paths before any map feature work begins.

---

### Pitfall 7: APK Code Paths Reveal the Decryption or Validation Logic to Reverse Engineers

**What goes wrong:**
A motivated attacker runs JADX on the APK and finds a method called `decryptGpxFile(byte[] key, byte[] data)`. The key derivation logic is visible. They write a Frida script to hook the method, capture the plaintext coordinates as they are produced, and reconstruct the GPX file. ProGuard/R8 is disabled because "it was causing crashes."

**Why it happens:**
R8 obfuscation is enabled by default but developers disable it when troubleshooting a crash and forget to re-enable it. Crash reporting tools (e.g., Firebase Crashlytics) need mapping files anyway, so obfuscation is seen as optional.

**How to avoid:**
- R8 is a speed bump, not a security wall — treat it as a necessary layer, not sufficient.
- Move all encryption key derivation and validation logic to the server. The app client should only receive a wrapped key for the current session, not derive keys itself.
- Do not name methods or fields with semantically obvious names (`decrypt`, `licenseKey`, `routeData`) — use R8 minification at minimum.
- For higher-value protection, consider using Android NDK (C/C++) for the decryption routine. NDK code is significantly harder to reverse than Dalvik bytecode, though not impossible.
- Enable R8 in release builds unconditionally. Use `@Keep` annotations for classes that must not be renamed (e.g., serialization models).

**Warning signs:**
- `minifyEnabled false` in any release build variant.
- JADX can produce readable class and method names from the release APK.
- Encryption logic lives entirely in a Kotlin/Java class with no server-side component.

**Phase to address:**
Security hardening phase (can be a final pass before Play Store submission, but the server-side key management architecture must be in place from the start).

---

### Pitfall 8: Google Play Billing Library Not Updated to v7+ Before August 2025 Deadline

**What goes wrong:**
Google mandated that all new apps and updates must use Play Billing Library v7 or newer by August 31, 2025. Shipping with an older version results in Play Store rejection. The BillingClient API surface changed significantly between v4/v5 and v7, so a last-minute upgrade breaks purchase flows.

**Why it happens:**
The deadline is known but deferred. Integration was done with the latest version available at project start, and the update is skipped during feature development.

**How to avoid:**
- Start with Billing Library 7.x from day one. Do not implement against an older version.
- Pin to `com.android.billingclient:billing-ktx:7.x.x` in `build.gradle`.
- Follow the v7 coroutine-based API (`BillingClient.startConnection` with `BillingClientStateListener`, `queryProductDetailsAsync`, `launchBillingFlow`, `queryPurchasesAsync`).

**Warning signs:**
- `com.android.billingclient:billing` dependency is below version 7 in `build.gradle`.
- Purchase flow uses the deprecated `SkuDetails` API (v4 and below) instead of `ProductDetails` (v5+).

**Phase to address:**
Google Play Billing integration phase — the version constraint is a prerequisite, not a migration task.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Store license state as a SharedPreferences boolean | Avoids server call at app start | Any client-side patch bypasses all content protection; requires rewrite | Never |
| Use a global static encryption key for all routes | Simpler key management code | One compromised device exposes all route data for all users | Never |
| Skip `linkedPurchaseToken` invalidation in v1 | Faster billing integration | Zombie tokens accumulate; users get free access after plan changes | Never |
| Defer R8 obfuscation until "before release" | Easier debugging during development | Always gets postponed; ships disabled | Never |
| Use OSMDroid default (external) tile cache path | Zero configuration | Breaks on Android 13+ devices; map shows grey tiles | Never for Android 13+ target |
| Block navigation on any network failure | Simpler license check code | App becomes unusable in offroad environments with no signal | Never for offroad use case |
| Hard-code encryption key in BuildConfig | Easier local testing | Key visible in decompiled APK; extracted in minutes | Development environment only, never production |

---

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Google Play Billing | Trusting the `PurchaseState.PURCHASED` result from the client without server verification | Always verify the purchase token via the Play Developer API on the server before granting access |
| Google Play Billing | Not handling `PENDING` purchase state | A purchase can be `PENDING` (e.g., cash payment methods); do not grant access until state is `PURCHASED` and server-verified |
| Google Play Billing | Missing Real-Time Developer Notifications (RTDN) for subscription events | Without RTDN webhooks, the server never knows about cancellations or expirations until the user opens the app |
| Android Keystore | Calling Keystore operations on the main thread | Keystore crypto operations (especially StrongBox) can take 100ms+; always run on a background coroutine/dispatcher |
| OSMDroid | Setting map center before zoom level | Setting center resets the zoom; always call `mapView.controller.setZoom()` before `setCenter()` |
| OSMDroid | Adding a `Polyline` overlay with raw GPX points (potentially thousands) | Use Douglas-Peucker simplification (OSMBonusPack's `DouglasPeuckerReducer`) before adding the overlay; raw GPX point density tanks frame rate |
| Play Integrity API | Calling `requestIntegrityToken()` on every app launch | The API has a call limit; cache the verdict for a reasonable period (e.g., 1 hour) and revalidate at critical events only (purchase, navigation start) |
| Firebase / backend | Not implementing RTDN (Pub/Sub) for license revocation | License expirations are only detected when the user opens the app; expired licenses stay "valid" indefinitely otherwise |

---

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Rendering full-resolution GPX overlay (10k+ points) on every map frame | Frame rate drops to <20fps during navigation; map is "sticky" | Apply Douglas-Peucker point reduction before building the `Polyline` overlay; target <500 rendered points per visible viewport | Routes with more than ~500 raw waypoints |
| Decrypting GPX data on the main thread | UI freezes for 500ms-2s when a route is opened | Run `Cipher.doFinal()` on `Dispatchers.IO`; show a loading indicator | Any route file over ~50KB ciphertext |
| Loading map tiles synchronously before navigation screen is shown | Long blank screen before map appears | Pre-warm the tile cache during route detail view display; use OSMDroid's prefetch API | First open of any route on slow connections |
| StrongBox-backed key used to decrypt large GPX files directly | Decryption takes 60+ seconds (StrongBox is slow for >5MB) | Use StrongBox only to wrap/unwrap a session AES key; do bulk decryption with a software AES implementation using the unwrapped key | Any route file over ~1MB |
| Server license check called synchronously on navigation start | Cold start takes 3-5 seconds; user sees a spinner | Pre-fetch and cache the license JWT during route browsing, refresh it in the background | Any network latency >500ms (common in rural/offroad areas) |

---

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Deriving the content decryption key from `BuildConfig.API_KEY` or any constant baked into the APK | Any attacker who decompiles the APK and reads the key derivation function recovers all keys | Keys must be server-issued, bound to a session, and never derivable from APK-visible constants |
| Trusting the device clock for license expiry checks | Attacker sets device clock backward to replay an expired session token | Server-issued tokens must be validated against server time; include a server-issued `iat` claim and reject tokens issued more than N minutes ago |
| Not checking Play Integrity verdict before issuing a license | Rooted devices or emulators can spoof purchase confirmations and make fraudulent license requests | Request a Play Integrity verdict at registration and before issuing any content key; reject `MEETS_DEVICE_INTEGRITY = false` for paid content |
| Logging route coordinates or decrypted content to Logcat or Crashlytics | Content extracted from crash reports or USB-attached developer console | Ensure no coordinate data ever appears in log statements; use ProGuard rules to strip `Log.*` calls from release builds |
| Using HTTP (not HTTPS) for license or key endpoints during development | Credentials and tokens are interceptable via Charles Proxy / mitmproxy | HTTPS with certificate pinning from day one; never ship an HTTP endpoint, even for dev |
| Storing the Google Play service account JSON key in the app repository | Full Play Developer API access exposed if repository is ever public | Service account credentials live only on the server; never in the Android project or web dashboard frontend |

---

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Blocking dialog when license re-validation fails mid-ride | Rider must stop to dismiss a dialog while on a moving motorcycle | Show a non-blocking banner; only halt navigation if the grace period is fully exhausted |
| No offline map tiles — app requires internet for tiles during navigation | Map goes grey as soon as the user enters a no-signal area (which is the entire point of the app) | Pre-download tile bundles for the route bounding box when the user purchases a route; store in app-private tile archive |
| North-up map orientation only | Offroad rider must mentally rotate the map to match their direction of travel, increasing cognitive load | Implement course-up (heading-up) map rotation using the device compass/bearing; let users toggle between north-up and course-up |
| Re-centering map on user location requires explicit tap | Rider checks the map, pans around, then cannot find their current position quickly | Add a floating "re-center" button that is always visible when the viewport has drifted from the current position |
| Map zoom level too high (street navigation default) | Offroad trails at default zoom are invisible; the route line disappears into the terrain | Default zoom for offroad GPX routes should show 2-5km of route; do not inherit street navigation zoom defaults |
| No visual distinction between "on route" and "off route" | Rider cannot tell at a glance if they have made a wrong turn | Color the route polyline differently when the rider's current position deviates more than 50m from the nearest route point |
| License expiry UI only shown in the account/settings screen | Rider discovers their day pass expired only when navigation is blocked at the worst moment | Show license TTL prominently on the route detail screen and as a persistent indicator during navigation |

---

## "Looks Done But Isn't" Checklist

- [ ] **GPX Encryption:** File is encrypted at rest — verify the decryption key is NOT stored in SharedPreferences, a static field, or BuildConfig
- [ ] **License Enforcement:** License check returns "valid" — verify the check is made against the server, not a local cache, at navigation start
- [ ] **Google Play Billing:** Purchase flow completes — verify `linkedPurchaseToken` invalidation logic exists in the server handler
- [ ] **Offline Navigation:** Map renders in development — verify tiles are pre-downloaded and stored in app-private storage for offline use
- [ ] **OSMDroid Storage:** Map tiles load on the emulator — verify tile cache path is set to app-private storage, not external, and test on Android 13+ physical device
- [ ] **Grace Period:** "Grace period" is implemented — verify it extends an active session but does NOT allow starting a new navigation session
- [ ] **Play Integrity:** Security checks are implemented — verify the API is called server-side (decrypted server-side, not client-side) and SafetyNet is NOT used (deprecated May 2025)
- [ ] **Route Rendering:** Route line appears on map — verify point count has been reduced and frame rate is >30fps on a mid-range device
- [ ] **Session Token:** JWT is validated — verify server checks `iat`, `exp`, `sub` (user ID), and `jti` (replay prevention nonce)
- [ ] **Admin Dashboard:** Route upload works — verify GPX encryption happens server-side before storage, not client-side in the browser

---

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Client-only license checks shipped to production | HIGH | Server-side enforcement requires a server deployment + forced app update; all installed versions remain bypassable until users update |
| GPX stored in external/world-readable path | HIGH | Requires app update to migrate files to private storage; encrypted blobs already distributed can be re-downloaded but old copies remain on user devices |
| `linkedPurchaseToken` not implemented | MEDIUM | Add the field to the server DB schema and backfill from Play Developer API; token overlap window is finite but requires an audit of all existing subscriptions |
| OSMDroid on wrong tile cache path | LOW | One-line config change + app update; tiles re-download on next launch |
| StrongBox used for bulk decryption | MEDIUM | Architecture change required to separate key wrapping (StrongBox) from bulk decryption (software AES); data migration not required |
| Billing Library below v7 | MEDIUM | API surface change from `SkuDetails` to `ProductDetails` requires rewriting the purchase and query flows; not a drop-in upgrade |

---

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Decryption keys in app memory | Content encryption and key management (early) | Verify with Android Profiler memory snapshot — no raw AES key bytes visible in heap |
| Client-only license checks | License enforcement phase | Attempt navigation with airplane mode: start should fail; active session should continue for grace period only |
| GPX in world-readable storage | Content download phase | Check `adb shell ls /sdcard/` — no `.gpx` or encrypted route files visible |
| `linkedPurchaseToken` not invalidated | Billing integration phase | Test with a simulated plan change; old token must be rejected within seconds |
| Hard network requirement mid-ride | Navigation offline handling phase | Kill network mid-navigation — map must remain functional for the grace period without any blocking dialog |
| OSMDroid tile cache path | Map setup phase (first) | Test on physical Android 13 device with no storage permissions granted — tiles must load |
| APK reveals key derivation | Security hardening phase | Run JADX on release APK — no key derivation logic should be identifiable |
| Billing Library version | Billing integration phase (prerequisite) | `./gradlew dependencies` must show `billing-ktx:7.x.x` |

---

## Sources

- [Android Keystore system — developer.android.com](https://developer.android.com/privacy-and-security/keystore)
- [How to extract sensitive plaintext data from Android memory — Pen Test Partners](https://www.pentestpartners.com/security-blog/how-to-extract-sensitive-plaintext-data-from-android-memory/)
- [KeyDroid: Large-Scale Analysis of Secure Key Storage in Android Apps — arxiv.org](https://arxiv.org/html/2507.07927v1)
- [Implementing linkedPurchaseToken correctly to prevent duplicate subscriptions — Android Developers blog](https://medium.com/androiddevelopers/implementing-linkedpurchasetoken-correctly-to-prevent-duplicate-subscriptions-82dfbf7167da)
- [Handling edge cases in Google Play Billing — RevenueCat](https://www.revenuecat.com/blog/engineering/google-play-edge-cases/)
- [Fight fraud and abuse — Play Billing — Android Developers](https://developer.android.com/google/play/billing/security)
- [Server-side purchase validation on Google Play — Adapty](https://adapty.io/blog/android-in-app-purchases-server-side-validation/)
- [Play Integrity API overview — Android Developers](https://developer.android.com/google/play/integrity/overview)
- [The Limitations of Google Play Integrity API — Approov](https://approov.io/blog/limitations-of-google-play-integrity-api-ex-safetynet)
- [A Practical Replay Attack Against the Widevine DRM — USENIX Security 2025](https://www.usenix.org/system/files/usenixsecurity25-roudot.pdf)
- [OSMDroid Tile Caching — GitHub Wiki](https://github.com/osmdroid/osmdroid/wiki/Tile-Caching)
- [OSMDroid Offline Map Tiles — GitHub Wiki](https://github.com/osmdroid/osmdroid/wiki/Offline-Map-Tiles)
- [OSMDroid Performance Issue with large overlays — GitHub Issue #182](https://github.com/osmdroid/osmdroid/issues/182)
- [OSMBonusPack DouglasPeuckerReducer — GitHub](https://github.com/MKergall/osmbonuspack)
- [JWT Security Vulnerabilities Prevention — APIsec](https://www.apisec.ai/blog/jwt-security-vulnerabilities-prevention)
- [Adding Server-Side License Verification — Android Developers](https://developer.android.com/google/play/licensing/server-side-verification)
- [From APK to Source Code: Decompiling Explained (2025) — Medium](https://medium.com/@vaibhav.shakya786/from-apk-to-source-code-the-dark-art-of-app-decompiling-explained-2025-edition-7f28fc2dee0f)
- [Subscription lifecycle — Play Billing — Android Developers](https://developer.android.com/google/play/billing/lifecycle/subscriptions)
- [Google Play Integrity API policy as of May 2025 — XDA Forums](https://xdaforums.com/t/google-play-integrity-api-policy-as-of-may-2025-and-rooted-devices.4732970/)

---
*Pitfalls research for: Android GPX navigation app with DRM/licensing (Roadrunner)*
*Researched: 2026-03-14*
