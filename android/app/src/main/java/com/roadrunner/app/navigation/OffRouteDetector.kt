package com.roadrunner.app.navigation

import org.osmdroid.util.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object OffRouteDetector {

    private const val EARTH_RADIUS_METRES = 6_371_000.0

    /**
     * Returns the minimum distance in metres from [point] to the nearest point on [polyline].
     * Returns [Double.MAX_VALUE] if [polyline] is empty (safe sentinel — never triggers off-route).
     */
    fun minDistanceToPolyline(point: GeoPoint, polyline: List<GeoPoint>): Double {
        if (polyline.isEmpty()) return Double.MAX_VALUE
        if (polyline.size == 1) return haversineMetres(point, polyline[0])
        return polyline.zipWithNext().minOf { (a, b) -> distanceToSegment(point, a, b) }
    }

    /**
     * Distance in metres from point [p] to segment [a]-[b].
     * Projects p onto the segment using dot-product parametric formula in lat/lon space
     * (approximation valid for short segments <50 km). Clamps t to [0, 1].
     */
    private fun distanceToSegment(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        val ax = a.longitude
        val ay = a.latitude
        val bx = b.longitude
        val by = b.latitude
        val px = p.longitude
        val py = p.latitude

        val abx = bx - ax
        val aby = by - ay
        val apx = px - ax
        val apy = py - ay

        val abLenSq = abx * abx + aby * aby
        val t = if (abLenSq == 0.0) 0.0 else ((apx * abx + apy * aby) / abLenSq).coerceIn(0.0, 1.0)

        val nearestLon = ax + t * abx
        val nearestLat = ay + t * aby
        return haversineMetres(p, GeoPoint(nearestLat, nearestLon))
    }

    /** Standard haversine formula; returns distance in metres. */
    private fun haversineMetres(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)

        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2.0 * EARTH_RADIUS_METRES * atan2(sqrt(h), sqrt(1.0 - h))
    }
}
