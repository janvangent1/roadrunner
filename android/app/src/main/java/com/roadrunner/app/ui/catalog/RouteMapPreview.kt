package com.roadrunner.app.ui.catalog

import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.io.File

/**
 * A small, non-interactive OSMDroid map preview showing the route polyline.
 * Falls back to centering on [lat]/[lng] if no points are available.
 */
@Composable
fun RouteMapPreview(
    lat: Double,
    lng: Double,
    routePoints: List<List<Double>>? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(false)
            isFocusable = false
            isClickable = false
            setOnTouchListener { _, event ->
                event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE
            }
        }
    }

    DisposableEffect(lat, lng, routePoints) {
        Configuration.getInstance().osmdroidTileCache = File(context.filesDir, "osmdroid")
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.overlays.clear()

        val geoPoints = routePoints
            ?.mapNotNull { pair -> if (pair.size >= 2) GeoPoint(pair[0], pair[1]) else null }
            ?.takeIf { it.isNotEmpty() }

        if (geoPoints != null) {
            // Draw the route as an orange polyline
            val polyline = Polyline().apply {
                setPoints(geoPoints)
                outlinePaint.color = android.graphics.Color.argb(220, 255, 109, 0) // OrangePrimary
                outlinePaint.strokeWidth = 6f
            }
            mapView.overlays.add(polyline)

            // Zoom to fit the polyline
            val north = geoPoints.maxOf { it.latitude }
            val south = geoPoints.minOf { it.latitude }
            val east  = geoPoints.maxOf { it.longitude }
            val west  = geoPoints.minOf { it.longitude }
            mapView.post {
                mapView.zoomToBoundingBox(BoundingBox(north, east, south, west), false, 24)
            }
        } else {
            // Fallback: just centre on the route's centroid
            mapView.controller.setZoom(12.0)
            mapView.controller.setCenter(GeoPoint(lat, lng))
        }

        mapView.invalidate()

        onDispose {
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
    )
}
