package com.roadrunner.app.ui.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.roadrunner.app.data.remote.dto.LicenseStatus
import com.roadrunner.app.data.remote.dto.RouteWithLicense
import com.roadrunner.app.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

@Composable
fun MapOverviewScreen(
    routes: List<RouteWithLicense>,
    onRouteClick: (routeId: String) -> Unit,
) {
    val context = LocalContext.current
    var selectedRoute by remember { mutableStateOf<RouteWithLicense?>(null) }

    // Palette for route polylines — cycles through these colours
    val routeColors = listOf(
        OrangePrimary, Color(0xFF2196F3), Color(0xFF4CAF50),
        Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFFFF5722),
    )

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                Configuration.getInstance().osmdroidTileCache = File(ctx.filesDir, "osmdroid")
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(6.0)
                    controller.setCenter(GeoPoint(50.5, 4.5)) // Europe centre
                }
            },
            update = { mapView ->
                mapView.overlays.clear()

                val allPoints = mutableListOf<GeoPoint>()

                routes.forEachIndexed { index, routeWithLicense ->
                    val color = routeColors[index % routeColors.size]
                    val points = routeWithLicense.route.routePoints
                    val center = if (routeWithLicense.route.centerLat != null && routeWithLicense.route.centerLng != null)
                        GeoPoint(routeWithLicense.route.centerLat, routeWithLicense.route.centerLng)
                    else null

                    // Draw polyline if we have points
                    if (!points.isNullOrEmpty()) {
                        val geoPoints = points.mapNotNull { pair ->
                            if (pair.size >= 2) GeoPoint(pair[0], pair[1]) else null
                        }
                        if (geoPoints.isNotEmpty()) {
                            allPoints.addAll(geoPoints)
                            val polyline = Polyline().apply {
                                setPoints(geoPoints)
                                outlinePaint.color = android.graphics.Color.argb(
                                    200,
                                    (color.red * 255).toInt(),
                                    (color.green * 255).toInt(),
                                    (color.blue * 255).toInt(),
                                )
                                outlinePaint.strokeWidth = 5f
                                setOnClickListener { _, _, _ ->
                                    selectedRoute = routeWithLicense
                                    true
                                }
                            }
                            mapView.overlays.add(polyline)
                        }
                    }

                    // Add marker at centre
                    if (center != null) {
                        allPoints.add(center)
                        val marker = Marker(mapView).apply {
                            position = center
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = routeWithLicense.route.title
                            setOnMarkerClickListener { _, _ ->
                                selectedRoute = routeWithLicense
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }
                }

                // Zoom to fit all routes
                if (allPoints.size >= 2) {
                    val north = allPoints.maxOf { it.latitude }
                    val south = allPoints.minOf { it.latitude }
                    val east  = allPoints.maxOf { it.longitude }
                    val west  = allPoints.minOf { it.longitude }
                    mapView.zoomToBoundingBox(BoundingBox(north, east, south, west), true, 80)
                }
                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Route info card (shown when a route is tapped)
        selectedRoute?.let { rwl ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, OutlineColor),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f)) {
                            // License status chip
                            val (statusLabel, statusColor) = when (rwl.licenseStatus) {
                                LicenseStatus.OWNED         -> "Owned"     to StatusGreen
                                LicenseStatus.ACTIVE        -> "Active"    to StatusBlue
                                LicenseStatus.EXPIRING_SOON -> "Expiring"  to StatusOrange
                                LicenseStatus.EXPIRED       -> "Expired"   to StatusRed
                                LicenseStatus.AVAILABLE     -> "Available" to StatusGray
                            }
                            Text(
                                text = statusLabel,
                                color = statusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.1.sp,
                            )
                            Text(
                                text = rwl.route.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = OnBackgroundDark,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            Text(
                                text = rwl.route.region,
                                style = MaterialTheme.typography.bodySmall,
                                color = SubtleText,
                            )
                        }
                        IconButton(onClick = { selectedRoute = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = SubtleText)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Straighten,
                                contentDescription = null,
                                tint = SubtleText,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                "${"%.1f".format(rwl.route.distanceKm)} km",
                                style = MaterialTheme.typography.bodySmall,
                                color = SubtleText,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Place,
                                contentDescription = null,
                                tint = SubtleText,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                rwl.route.terrainType,
                                style = MaterialTheme.typography.bodySmall,
                                color = SubtleText,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onRouteClick(rwl.route.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangePrimary,
                            contentColor = BackgroundDark,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("View Details", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // "No routes" message
        if (routes.isEmpty()) {
            Text(
                text = "No routes available",
                color = SubtleText,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
