---
phase: 03-android-catalog-and-auth
plan: "01"
subsystem: android

tags: [android, kotlin, jetpack-compose, hilt, navigation, osmdroid, retrofit, tink, gradle]

# Dependency graph
requires: []
provides:
  - Buildable Android project skeleton at android/ with all Phase 3 dependencies
  - Hilt DI wired (@HiltAndroidApp on RoadrunnerApp, @AndroidEntryPoint on MainActivity)
  - Jetpack Compose NavGraph with five screen destinations (login, register, catalog, my_routes, route_detail)
  - OSMDroid tile cache initialized to filesDir/osmdroid in Application.onCreate()
  - motorcycle product flavor with BuildConfig.BASE_URL per buildType
affects:
  - 03-android-catalog-and-auth
  - 04-gpx-navigation

# Tech tracking
tech-stack:
  added:
    - "Android Gradle Plugin 8.7.0"
    - "Kotlin 2.0.21"
    - "Kotlin Compose Compiler Plugin 2.0.21"
    - "Hilt 2.51.1 + KSP 2.0.21-1.0.28"
    - "Compose BOM 2024.09.00"
    - "Navigation Compose 2.8.3"
    - "OSMDroid 6.1.20"
    - "Retrofit 2.11.0 + OkHttp 4.12.0"
    - "Google Tink Android 1.15.0"
    - "Credentials API 1.3.0 + Google ID 1.1.1"
    - "Gradle wrapper 8.9"
  patterns:
    - "Single :app module with clean architecture layers within module"
    - "motorcycle product flavor scaffolded for Phase 7 branding"
    - "BuildConfig.BASE_URL per buildType: debug=10.0.2.2:3000, release=api.roadrunner.app"
    - "OSMDroid tile cache must be set in Application.onCreate() before any map code"
    - "Placeholder screen composables in NavGraph.kt until feature plans replace them"

key-files:
  created:
    - android/build.gradle.kts
    - android/settings.gradle.kts
    - android/gradle.properties
    - android/app/build.gradle.kts
    - android/app/src/main/AndroidManifest.xml
    - android/app/src/main/java/com/roadrunner/app/RoadrunnerApp.kt
    - android/app/src/main/java/com/roadrunner/app/MainActivity.kt
    - android/app/src/main/java/com/roadrunner/app/di/AppModule.kt
    - android/app/src/main/java/com/roadrunner/app/navigation/NavGraph.kt
    - android/app/src/main/java/com/roadrunner/app/navigation/Screen.kt
    - android/app/src/main/java/com/roadrunner/app/ui/theme/Theme.kt
    - android/app/src/main/res/values/strings.xml
    - android/app/src/main/res/values/themes.xml
  modified: []

key-decisions:
  - "Kotlin 2.0 requires org.jetbrains.kotlin.plugin.compose in addition to kotlin.android; composeOptions block removed"
  - "Theme.AppCompat.Light.NoActionBar used for Activity window theme instead of Theme.Material3.DayNight.NoActionBar (Material3 XML themes require com.google.android.material library not in deps)"
  - "Launcher icon refs removed from AndroidManifest — icon assets deferred to Phase 7 branding"
  - "androidx.preference:preference-ktx added for PreferenceManager.getDefaultSharedPreferences used by OSMDroid Configuration.load()"
  - "osmdroidTileCache set to filesDir (not cacheDir or external) per Android 13+ scoped storage requirement"

patterns-established:
  - "OSMDroid Configuration.getInstance() must be called with load() + osmdroidTileCache before any MapView is created"
  - "Screen sealed class defines all route strings; NavGraph.kt uses Screen.*.route constants throughout"
  - "AppModule starts empty; each subsequent plan adds its own @Provides bindings"

requirements-completed: [AUTH-01, AUTH-02, AUTH-03, AUTH-04]

# Metrics
duration: 14min
completed: 2026-03-15
---

# Phase 3 Plan 01: Android Project Scaffold Summary

**Buildable Android skeleton with Hilt DI, Compose NavGraph (5 destinations), OSMDroid tile cache to filesDir, and motorcycle flavor — `./gradlew assembleMotorcycleDebug` exits 0**

## Performance

- **Duration:** 14 min
- **Started:** 2026-03-15T06:40:00Z
- **Completed:** 2026-03-15T06:54:07Z
- **Tasks:** 2
- **Files modified:** 13 created, 3 modified

## Accomplishments

- Full Android Gradle project scaffold with Kotlin DSL, AGP 8.7.0, Kotlin 2.0.21, all Phase 3 deps resolved
- Hilt fully wired: @HiltAndroidApp on RoadrunnerApp, @AndroidEntryPoint on MainActivity, AppModule placeholder
- Jetpack Compose NavGraph with all 5 screen destinations and stub composables compilable out of the box
- OSMDroid tile cache set to `filesDir/osmdroid` in Application.onCreate() before any map code per Android 13+ requirement

## Task Commits

Each task was committed atomically:

1. **Task 1: Gradle project scaffold with all dependencies and motorcycle flavor** - `095cf6b` (chore)
2. **Task 2: Hilt application class, MainActivity, NavGraph, and OSMDroid init** - `f39549b` (feat)

## Files Created/Modified

- `android/build.gradle.kts` - Root plugins block: AGP 8.7.0, Kotlin 2.0.21, Compose compiler plugin, Hilt 2.51.1, KSP
- `android/settings.gradle.kts` - Single :app module, repo management
- `android/gradle.properties` - AndroidX, Jetifier, Kotlin code style
- `android/app/build.gradle.kts` - App config: namespace, compileSdk 35, minSdk 24, motorcycle flavor, BuildConfig.BASE_URL, all deps
- `android/app/src/main/AndroidManifest.xml` - INTERNET permission, RoadrunnerApp application reference, MainActivity launcher
- `android/app/src/main/java/com/roadrunner/app/RoadrunnerApp.kt` - @HiltAndroidApp; OSMDroid tile cache init
- `android/app/src/main/java/com/roadrunner/app/MainActivity.kt` - @AndroidEntryPoint; EdgeToEdge; NavHost bootstrap
- `android/app/src/main/java/com/roadrunner/app/di/AppModule.kt` - Empty @Module @InstallIn(SingletonComponent) placeholder
- `android/app/src/main/java/com/roadrunner/app/navigation/Screen.kt` - Sealed class with 5 route strings
- `android/app/src/main/java/com/roadrunner/app/navigation/NavGraph.kt` - RoadrunnerNavGraph with all 5 composable destinations
- `android/app/src/main/java/com/roadrunner/app/ui/theme/Theme.kt` - Minimal RoadrunnerTheme wrapping MaterialTheme
- `android/app/src/main/res/values/strings.xml` - app_name = "Roadrunner"
- `android/app/src/main/res/values/themes.xml` - Theme.Roadrunner parent = Theme.AppCompat.Light.NoActionBar

## Decisions Made

- Kotlin 2.0 mandates `org.jetbrains.kotlin.plugin.compose` plugin; `composeOptions.kotlinCompilerExtensionVersion` block removed (no longer needed with Compose compiler plugin)
- `Theme.AppCompat.Light.NoActionBar` used for Activity window theme — Compose handles all visual theming via `RoadrunnerTheme`; Material3 XML theme requires a separate library not needed here
- Launcher icons deferred to Phase 7 branding; references removed from manifest for clean scaffold
- `androidx.preference:preference-ktx:1.2.1` added as OSMDroid's `Configuration.load()` requires `PreferenceManager.getDefaultSharedPreferences()`

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Kotlin 2.0 Compose compiler plugin missing**
- **Found during:** Task 1 (dependency resolution)
- **Issue:** Kotlin 2.0+ requires `org.jetbrains.kotlin.plugin.compose` applied explicitly; `composeOptions` block alone no longer sufficient
- **Fix:** Added `id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"` to root and app build.gradle.kts; removed `composeOptions` block
- **Files modified:** android/build.gradle.kts, android/app/build.gradle.kts
- **Verification:** `./gradlew :app:dependencies` exits 0 after fix
- **Committed in:** 095cf6b (Task 1 commit)

**2. [Rule 2 - Missing dependency] OSMDroid PreferenceManager dependency missing**
- **Found during:** Task 2 (source file creation)
- **Issue:** `RoadrunnerApp.kt` uses `PreferenceManager.getDefaultSharedPreferences()` per OSMDroid's recommended `Configuration.load()` pattern; `androidx.preference` not in deps
- **Fix:** Added `implementation("androidx.preference:preference-ktx:1.2.1")` to app/build.gradle.kts
- **Files modified:** android/app/build.gradle.kts
- **Verification:** Build succeeds with preference import resolved
- **Committed in:** f39549b (Task 2 commit)

**3. [Rule 1 - Bug] Theme.Material3.DayNight.NoActionBar not available via compile-time XML**
- **Found during:** Task 2 (first assembleMotorcycleDebug run)
- **Issue:** AAPT error — Material3 XML theme requires `com.google.android.material` library (not in deps); Compose does not need XML material3 theme
- **Fix:** Changed `themes.xml` parent to `Theme.AppCompat.Light.NoActionBar` (included via AndroidX transitively)
- **Files modified:** android/app/src/main/res/values/themes.xml
- **Verification:** Build succeeds after fix
- **Committed in:** f39549b (Task 2 commit)

**4. [Rule 1 - Bug] Missing mipmap launcher icons caused AAPT link failure**
- **Found during:** Task 2 (second assembleMotorcycleDebug run after theme fix)
- **Issue:** AndroidManifest referenced `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round` which don't exist in scaffold
- **Fix:** Removed `android:icon` and `android:roundIcon` attributes from `<application>` tag; icons deferred to Phase 7 branding
- **Files modified:** android/app/src/main/AndroidManifest.xml
- **Verification:** Build succeeds; APK produced at app/build/outputs/apk/motorcycle/debug/
- **Committed in:** f39549b (Task 2 commit)

---

**Total deviations:** 4 auto-fixed (2 Rule 1 bugs, 1 Rule 1 AAPT bug, 1 Rule 2 missing dependency)
**Impact on plan:** All auto-fixes required for correct Kotlin 2.0 + Compose compiler integration and successful build. No scope creep.

## Issues Encountered

- OpenJDK 24.0.1 (at `C:\Users\jbouq\.jdks\openjdk-24.0.1`) required for AGP 8.7.0; default PATH Java 1.8 insufficient. Builds must use `JAVA_HOME=/c/Users/jbouq/.jdks/openjdk-24.0.1` or configure Android Studio's JDK in gradle.properties.

## User Setup Required

Developers cloning the repo need to create `android/local.properties` with:
```
sdk.dir=C\:\\Users\\<username>\\AppData\\Local\\Android\\Sdk
```

And use JDK 11+ (JDK 24 available at `C:\Users\jbouq\.jdks\openjdk-24.0.1`).

## Next Phase Readiness

- Plan 03-02: Auth screens (Login + Register) can now import `Screen`, `RoadrunnerNavGraph`, and `AppModule` bindings
- Plan 03-03: Catalog + RouteDetail screens replace placeholder composables in NavGraph.kt
- OSMDroid configured and ready for map views in Plan 03-03 (RouteDetail preview map)
- Hilt component graph ready; ViewModel injection via `hiltViewModel()` will work out of the box

---
*Phase: 03-android-catalog-and-auth*
*Completed: 2026-03-15*
