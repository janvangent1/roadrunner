---
phase: 6
title: Navigation Experience
status: ready
---

# Phase 6 Context: Navigation Experience

## Decisions

### NavigationScreen replaces the Phase 5 placeholder
- Phase 5 created `NavigationScreen.kt` as a stub with only a Back button
- Phase 6 replaces it entirely with the full navigation experience

### Map setup
- OSMDroid `MapView` via `AndroidView` composable (same pattern as `OsmPreviewMap`)
- Route polyline overlay drawn from the already-decrypted GPX (reuse `RouteRepository.getDecryptedGpx()`)
- Tile source: `TileSourceFactory.MAPNIK`
- Map starts centered on the first track point; follows GPS position during navigation
- Rotation: map follows device orientation (course-up) using `CompassOverlay` — enable when moving, disable when still
- Location overlay: `MyLocationNewOverlay` with `GpsMyLocationProvider`

### GPS and location
- `LocationRepository` — wraps Android `FusedLocationProviderClient` (Google Play Services) for accurate GPS
- Updates: `LocationRequest` with `PRIORITY_HIGH_ACCURACY`, interval 2s, fastest interval 1s
- Emits `Location` objects as `StateFlow<Location?>`
- Permissions: `ACCESS_FINE_LOCATION` — already declared or to be added to AndroidManifest

### HUD (heads-up display)
Floating overlay on top of the map — always visible during navigation:
- **Speed:** from `location.speed` (m/s → km/h conversion)
- **Distance covered:** running total of haversine distance between GPS track points
- **Distance remaining:** total route distance minus distance covered (from `RouteDto.distanceKm`)
- **Elapsed time:** `System.currentTimeMillis()` minus navigation start time (in-memory, not server clock — display only, not security)

HUD layout: semi-transparent Card anchored to top of screen, 2×2 grid of metric tiles.

### Off-route indicator
- Compute minimum distance from current GPS position to any point on the route polyline
- If distance > 50 metres: show "Off Route" warning banner below HUD
- Clear when distance drops back below 40 metres (hysteresis to avoid flickering)
- No rerouting — visual indicator only

### Waypoints
- Waypoints stored in `RouteDto.waypoints` (already fetched with route detail)
- Render as `Marker` overlays on the OSMDroid map with label and type-appropriate icon
- Types: FUEL (⛽), WATER (💧), CAUTION (⚠️), INFO (ℹ️) — use text emoji as marker icon for simplicity in v1

### Offline tile pre-caching
- On purchase / first access of a route: pre-download OSMDroid tiles for the route bounding box
- Zoom levels 10–15 (covers regional overview to street-level detail)
- Use `MapTileProviderBasic` + `TileDownloader` pattern, or `OSMDroid`'s `OfflineTileProvider`
- Actually: use `MapBox` offline SDK? No — use OSMDroid's built-in `OfflineTileProvider` with a custom downloader
- **Decision:** Use `OSMDroid`'s `OfflineTileDownloader` approach — spawn a coroutine that iterates tile coordinates within bounding box and zoom levels, downloads via OkHttp to `filesDir/osmdroid/tiles/`
- Trigger: when user opens RouteDetailScreen for a route they own, check if tiles are cached; if not, show "Download for offline use" button → download in background

### Session expiry during navigation
- Already handled by `NavigationSessionManager` from Phase 5 — the `ExpiryDialog` is shown
- Navigation screen observes `NavigationSessionManager.isGracePeriodExpired()` via a `LaunchedEffect` loop
- When dialog confirmed: `navController.popBackStack()` → returns to RouteDetailScreen

### Requirements covered
- NAV-01: Launch navigation for route with valid license — already gated by Phase 5
- NAV-02: OSMDroid map + GPX polyline overlay
- NAV-03: Real-time GPS position dot
- NAV-04: HUD (speed, distance covered, distance remaining, elapsed time)
- NAV-05: Off-route indicator (visual)
- NAV-06: Offline map tile pre-caching
- WAYPT-01: Admin waypoints — already in RouteDto from backend
- WAYPT-02: Waypoint pins during navigation

## Deferred
- Voice guidance (v2, explicitly out of scope)
- Rerouting (out of scope)
- Turn-by-turn arrows (out of scope)
- Advanced tile cache management / cache size limits
