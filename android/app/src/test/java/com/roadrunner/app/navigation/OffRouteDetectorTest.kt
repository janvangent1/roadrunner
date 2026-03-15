package com.roadrunner.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.osmdroid.util.GeoPoint

class OffRouteDetectorTest {

    // Helper: build a GeoPoint nudged ~distanceMetres north of origin
    // 1 degree latitude ≈ 111_320 m
    private fun pointNorthOf(origin: GeoPoint, metres: Double): GeoPoint {
        val latOffset = metres / 111_320.0
        return GeoPoint(origin.latitude + latOffset, origin.longitude)
    }

    /**
     * Case 1: Point ON the polyline → returns 0.0 (within floating-point tolerance)
     */
    @Test
    fun pointOnPolyline_returnsZero() {
        val a = GeoPoint(51.0, 4.0)
        val b = GeoPoint(51.01, 4.0)
        val midpoint = GeoPoint(51.005, 4.0)   // exactly on segment

        val dist = OffRouteDetector.minDistanceToPolyline(midpoint, listOf(a, b))

        assertEquals("Expected ~0m for point on polyline", 0.0, dist, 2.0)
    }

    /**
     * Case 2: Point 100m perpendicular to a horizontal segment → returns ~100.0 (within 2m tolerance)
     */
    @Test
    fun pointPerpendicularToSegment_returns100Metres() {
        // Horizontal segment at lat 51.0, from lon 4.0 to 4.01
        val a = GeoPoint(51.0, 4.0)
        val b = GeoPoint(51.0, 4.01)
        // Point ~100m north of the midpoint of the segment
        val midLon = (a.longitude + b.longitude) / 2.0
        val midPoint = GeoPoint(51.0, midLon)
        val perp = pointNorthOf(midPoint, 100.0)

        val dist = OffRouteDetector.minDistanceToPolyline(perp, listOf(a, b))

        assertEquals("Expected ~100m perpendicular distance", 100.0, dist, 2.0)
    }

    /**
     * Case 3: Point past end of segment → distance to nearest endpoint, not perpendicular projection
     */
    @Test
    fun pointPastEndOfSegment_returnsDistanceToEndpoint() {
        val a = GeoPoint(51.0, 4.0)
        val b = GeoPoint(51.0, 4.01)
        // Place point past b, 100m further east and 50m north
        val pastEnd = GeoPoint(51.0 + (50.0 / 111_320.0), 4.015)

        val dist = OffRouteDetector.minDistanceToPolyline(pastEnd, listOf(a, b))

        // Distance to b should be roughly sqrt(50^2 + delta_east^2) — at minimum > 50m and < 200m
        // The key is it returns distance to endpoint, not projection
        val distToB = OffRouteDetector.minDistanceToPolyline(pastEnd, listOf(b))
        assertEquals("Should equal distance to nearest endpoint", distToB, dist, 1.0)
    }

    /**
     * Case 4: Empty polyline → returns Double.MAX_VALUE (safe sentinel)
     */
    @Test
    fun emptyPolyline_returnsMaxValue() {
        val point = GeoPoint(51.0, 4.0)

        val dist = OffRouteDetector.minDistanceToPolyline(point, emptyList())

        assertEquals("Empty polyline must return Double.MAX_VALUE", Double.MAX_VALUE, dist, 0.0)
    }

    /**
     * Case 5: Single-point polyline → returns haversine distance from point to that single point
     */
    @Test
    fun singlePointPolyline_returnsHaversineDistance() {
        val singlePoint = GeoPoint(51.0, 4.0)
        val query = pointNorthOf(singlePoint, 200.0)

        val dist = OffRouteDetector.minDistanceToPolyline(query, listOf(singlePoint))

        assertEquals("Expected ~200m haversine distance", 200.0, dist, 3.0)
    }
}
