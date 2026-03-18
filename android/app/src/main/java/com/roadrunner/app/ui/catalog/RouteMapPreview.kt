package com.roadrunner.app.ui.catalog

import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

/**
 * A small, non-interactive OSMDroid map preview centered on [lat]/[lng].
 * Scrolling and zooming are disabled — purely decorative.
 */
@Composable
fun RouteMapPreview(
    lat: Double,
    lng: Double,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            // Non-interactive
            setMultiTouchControls(false)
            isFocusable = false
            isClickable = false
            // Disable all gestures
            overlayManager.clear()
            setOnTouchListener { _, event ->
                event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE
            }
        }
    }

    DisposableEffect(Unit) {
        Configuration.getInstance().osmdroidTileCache = File(context.filesDir, "osmdroid")
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.controller.setZoom(12.0)
        mapView.controller.setCenter(GeoPoint(lat, lng))

        // Center marker
        val marker = Marker(mapView).apply {
            position = GeoPoint(lat, lng)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = null
            icon = null
        }
        mapView.overlays.add(marker)
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
