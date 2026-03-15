package com.roadrunner.app.ui.routedetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.roadrunner.app.data.repository.RouteRepository
import io.ticofab.androidgpxparser.parser.GPXParser
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/** Approximate region centers for preview map. Add more as catalog grows. */
private val REGION_COORDS = mapOf(
    "ardennes" to GeoPoint(50.0, 5.5),
    "alps" to GeoPoint(46.5, 8.0),
    "pyrenees" to GeoPoint(42.7, 1.0),
    "black forest" to GeoPoint(48.0, 8.2),
    // fallback handled below
)

@Composable
fun OsmPreviewMap(
    region: String,
    routeId: String,
    routeRepository: RouteRepository,
    modifier: Modifier = Modifier,
) {
    val center = REGION_COORDS[region.lowercase()] ?: GeoPoint(50.0, 4.0) // Europe center fallback

    var polylinePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }

    LaunchedEffect(routeId) {
        val result = withContext(Dispatchers.IO) {
            routeRepository.getDecryptedGpx(routeId)
        }
        result
            .onSuccess { bytes ->
                val gpxParser = GPXParser()
                val gpx = gpxParser.parse(ByteArrayInputStream(bytes))
                polylinePoints = gpx.tracks
                    .flatMap { it.segments }
                    .flatMap { it.points }
                    .map { GeoPoint(it.latitude, it.longitude) }
            }
            .onFailure { e ->
                android.util.Log.w("OsmPreviewMap", "GPX unavailable: ${e.message}")
                // falls back to empty list — region marker still shown
            }
    }

    AndroidView(
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isClickable = false  // preview map — not interactive in detail view
                controller.setZoom(9.0)
                controller.setCenter(center)
                // Add a simple marker at center to indicate route region
                val marker = Marker(this)
                marker.position = center
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                overlays.add(marker)
            }
        },
        update = { mapView ->
            // Remove any existing Polyline overlays (not markers)
            mapView.overlays.removeAll { it is Polyline }
            if (polylinePoints.isNotEmpty()) {
                val polyline = Polyline()
                polylinePoints.forEach { polyline.addPoint(it) }
                mapView.overlays.add(polyline)
                // Zoom to fit the polyline bounding box
                mapView.zoomToBoundingBox(polyline.bounds, true, 48)
            }
            mapView.invalidate()
        },
        modifier = modifier,
    )
}
