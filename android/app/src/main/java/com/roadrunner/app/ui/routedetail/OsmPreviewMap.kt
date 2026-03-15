package com.roadrunner.app.ui.routedetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

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
    modifier: Modifier = Modifier,
) {
    val center = REGION_COORDS[region.lowercase()] ?: GeoPoint(50.0, 4.0) // Europe center fallback

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
        modifier = modifier,
    )
}
