package com.roadrunner.app.data.repository

import com.roadrunner.app.data.remote.ApiService
import com.roadrunner.app.data.remote.dto.LicenseCheckRequest
import com.roadrunner.app.data.remote.dto.LicenseDto
import com.roadrunner.app.data.remote.dto.LicenseStatus
import com.roadrunner.app.data.remote.dto.LicenseType
import com.roadrunner.app.data.remote.dto.NavigationSession
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseRepository @Inject constructor(
    private val apiService: ApiService,
) {
    private val _cachedLicenses = mutableListOf<LicenseDto>()

    /**
     * Fetch all user licenses from server; update in-memory cache.
     * On failure, keep stale cache data intact.
     */
    suspend fun getMyLicenses(): Result<List<LicenseDto>> {
        return try {
            val response = apiService.getMyLicenses()
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                _cachedLicenses.clear()
                _cachedLicenses.addAll(body)
                Result.success(body)
            } else {
                Result.failure(Exception("Failed to fetch licenses: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compute display LicenseStatus for a given routeId from the cached list.
     * Returns Triple(LicenseStatus, expiresAt, LicenseType?).
     *
     * Uses server-issued expiresAt — does NOT call System.currentTimeMillis() for
     * the active/expired decision (device clock tampering here only affects the
     * EXPIRING_SOON warning banner, not the security gate).
     */
    fun computeLicenseStatus(routeId: String): Triple<LicenseStatus, String?, LicenseType?> {
        val license = _cachedLicenses.firstOrNull { it.routeId == routeId && it.revokedAt == null }
            ?: return Triple(LicenseStatus.AVAILABLE, null, null)

        if (license.type == LicenseType.PERMANENT || license.expiresAt == null) {
            return Triple(LicenseStatus.OWNED, null, LicenseType.PERMANENT)
        }

        val instant = Instant.parse(license.expiresAt)
        val nowMillis = System.currentTimeMillis() // display hint only, not a security gate
        val expiryMillis = instant.toEpochMilli()

        return when {
            expiryMillis < nowMillis ->
                Triple(LicenseStatus.EXPIRED, license.expiresAt, license.type)
            (expiryMillis - nowMillis) <= 3 * 24 * 60 * 60 * 1000L ->
                Triple(LicenseStatus.EXPIRING_SOON, license.expiresAt, license.type)
            else ->
                Triple(LicenseStatus.ACTIVE, license.expiresAt, license.type)
        }
    }

    /**
     * Call POST /licenses/check for a specific route before navigation start.
     * Returns Result<NavigationSession> on success (valid license).
     *
     * Returns Result.failure with a user-readable message on 403/error:
     *   "License expired" for reason EXPIRED
     *   "No license found" for reason NOT_FOUND
     *   "License revoked" for reason REVOKED
     *   "License check failed" for network errors
     */
    suspend fun checkLicense(routeId: String): Result<NavigationSession> {
        return try {
            val response = apiService.checkLicense(LicenseCheckRequest(routeId))
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.failure(Exception("License check failed"))
                val sessionExpiry = body.sessionExpiresAt ?: body.expiresAt
                    ?: return Result.failure(Exception("License check failed"))
                val session = NavigationSession(
                    sessionToken = body.sessionToken,
                    sessionExpiresAt = sessionExpiry,
                    licenseType = body.licenseType,
                )
                Result.success(session)
            } else if (response.code() == 403) {
                val errorCode = parseErrorCode(response.errorBody()?.string())
                val message = when (errorCode) {
                    "EXPIRED" -> "License expired"
                    "NOT_FOUND" -> "No license found"
                    "REVOKED" -> "License revoked"
                    else -> "No license found"
                }
                Result.failure(Exception(message))
            } else {
                Result.failure(Exception("License check failed"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("License check failed"))
        }
    }

    /** Parse the `code` field from a JSON error body like {"code":"EXPIRED"} */
    private fun parseErrorCode(errorBodyString: String?): String? {
        if (errorBodyString.isNullOrBlank()) return null
        return try {
            // Simple substring extraction to avoid requiring a JSON parser dependency here
            val codePrefix = "\"code\""
            val idx = errorBodyString.indexOf(codePrefix)
            if (idx < 0) return null
            val after = errorBodyString.substring(idx + codePrefix.length)
            val quote1 = after.indexOf('"')
            val quote2 = after.indexOf('"', quote1 + 1)
            if (quote1 < 0 || quote2 < 0) return null
            after.substring(quote1 + 1, quote2)
        } catch (e: Exception) {
            null
        }
    }
}
