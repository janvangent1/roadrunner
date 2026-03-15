package com.roadrunner.app.data.remote.dto

/** A single license record as returned by GET /api/v1/licenses/my */
data class LicenseDto(
    val id: String,
    val routeId: String,
    val type: LicenseType,      // reuses enum from RouteDtos.kt
    val expiresAt: String?,     // ISO 8601 or null (permanent)
    val revokedAt: String?,     // non-null means revoked
    val createdAt: String,
)

/** In-memory representation of a resolved navigation session */
data class NavigationSession(
    val sessionToken: String,
    val sessionExpiresAt: String,  // ISO 8601 — server-issued, 1-hour window
    val licenseType: LicenseType,
)
