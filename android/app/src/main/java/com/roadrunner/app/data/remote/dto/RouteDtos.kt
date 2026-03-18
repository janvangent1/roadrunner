package com.roadrunner.app.data.remote.dto

enum class Difficulty { EASY, MODERATE, HARD, EXPERT }
enum class LicenseType { DAY_PASS, MULTI_DAY, PERMANENT }

data class WaypointDto(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val sortOrder: Int,
)

data class RouteDto(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: Difficulty,
    val terrainType: String,
    val region: String,
    val estimatedDurationMinutes: Int,
    val distanceKm: Double,
    val published: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val waypoints: List<WaypointDto> = emptyList(),
    val centerLat: Double? = null,
    val centerLng: Double? = null,
)

data class LicenseCheckRequest(
    val routeId: String,
    val deviceId: String = "android",  // Reserved for v2 Play Integrity
)

data class LicenseCheckResponse(
    val sessionToken: String,
    val expiresAt: String?,
    val licenseType: LicenseType,
    val sessionExpiresAt: String? = null,  // Server-issued 1-hour session window expiry
)

/** Derived UI model for license status badge display */
enum class LicenseStatus {
    OWNED,         // permanent license
    ACTIVE,        // time-based, not yet expired
    EXPIRING_SOON, // expires within 3 days
    EXPIRED,       // was licensed, now expired
    AVAILABLE,     // no license
}

data class RouteWithLicense(
    val route: RouteDto,
    val licenseStatus: LicenseStatus,
    val expiresAt: String?,       // ISO 8601 string or null
    val licenseType: LicenseType?, // null if no license
)
