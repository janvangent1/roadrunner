package com.roadrunner.app.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.roadrunner.app.data.local.NavigationSessionManager
import com.roadrunner.app.data.remote.dto.WaypointDto
import kotlinx.coroutines.delay
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun NavigationScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    sessionManager: NavigationSessionManager,
    viewModel: NavigationViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Session expiry polling — preserved from Phase 5 exactly
    var showExpiryDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            if (sessionManager.isSessionExpired()) {
                showExpiryDialog = true
                break
            }
        }
    }
    if (showExpiryDialog) {
        ExpiryDialog(onDismiss = {
            sessionManager.clearSession()
            onSessionExpired()
        })
    }

    // Permission launcher — request ACCESS_FINE_LOCATION then start tracking
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startTracking()
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.startTracking()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Lifecycle cleanup
    DisposableEffect(Unit) {
        onDispose { viewModel.stopTracking() }
    }

    Box(Modifier.fillMaxSize()) {
        // 1. Full-screen map
        AndroidView(
            factory = { ctx ->
                buildMapView(ctx, uiState.routePoints, uiState.waypoints)
            },
            update = { mapView ->
                updateMapLocation(mapView, uiState)
            },
            modifier = Modifier.fillMaxSize(),
        )

        // 2. HUD overlay — top
        HudOverlay(
            state = uiState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
        )

        // 3. Off-route banner — below HUD, conditional
        if (uiState.isOffRoute) {
            OffRouteBanner(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 140.dp),
            )
        }

        // 4. Back button — top-left
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }
    }
}

private fun buildMapView(
    ctx: android.content.Context,
    routePoints: List<GeoPoint>,
    waypoints: List<WaypointDto>,
): MapView {
    val mapView = MapView(ctx)
    mapView.setTileSource(TileSourceFactory.MAPNIK)
    mapView.setMultiTouchControls(true)
    mapView.controller.setZoom(14.0)

    if (routePoints.isNotEmpty()) {
        mapView.controller.setCenter(routePoints.first())
    }

    // Route polyline
    val polyline = Polyline()
    polyline.outlinePaint.color = Color.Blue.toArgb()
    polyline.outlinePaint.strokeWidth = 8f
    polyline.setPoints(routePoints)
    mapView.overlays.add(polyline)

    // GPS / my-location overlay
    val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), mapView)
    myLocationOverlay.enableMyLocation()
    myLocationOverlay.enableFollowLocation()
    mapView.overlays.add(myLocationOverlay)

    // Compass overlay
    val compassOverlay = CompassOverlay(ctx, InternalCompassOrientationProvider(ctx), mapView)
    compassOverlay.enableCompass()
    mapView.overlays.add(compassOverlay)

    // Waypoint markers
    for (waypoint in waypoints) {
        val marker = Marker(mapView)
        marker.position = GeoPoint(waypoint.latitude, waypoint.longitude)
        marker.title = waypoint.label
        marker.snippet = waypoint.type
        marker.setIcon(null)
        marker.subDescription = when (waypoint.type.uppercase()) {
            "FUEL" -> "\u26fd"
            "WATER" -> "\ud83d\udca7"
            "CAUTION" -> "\u26a0\ufe0f"
            "INFO" -> "\u2139\ufe0f"
            else -> "\ud83d\udccd"
        }
        mapView.overlays.add(marker)
    }

    return mapView
}

@Suppress("UNUSED_PARAMETER")
private fun updateMapLocation(mapView: MapView, uiState: NavigationUiState) {
    // MyLocationNewOverlay.enableFollowLocation() handles centering automatically.
    // This function is intentionally a no-op.
}

@Composable
private fun HudOverlay(state: NavigationUiState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.65f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp)) {
                HudTile(label = "Speed", value = "%.0f km/h".format(state.speedKmh))
                HudTile(label = "Covered", value = "%.1f km".format(state.distanceCoveredKm))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp)) {
                HudTile(label = "Remaining", value = "%.1f km".format(state.distanceRemainingKm))
                HudTile(label = "Elapsed", value = formatElapsed(state.elapsedSeconds))
            }
        }
    }
}

@Composable
private fun HudTile(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
    }
}

private fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

@Composable
private fun OffRouteBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Red),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Off Route",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
