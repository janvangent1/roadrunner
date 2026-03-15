package com.roadrunner.app.data.repository

import com.roadrunner.app.data.remote.ApiService
import com.roadrunner.app.data.remote.dto.LicenseCheckRequest
import com.roadrunner.app.data.remote.dto.LicenseStatus
import com.roadrunner.app.data.remote.dto.LicenseType
import com.roadrunner.app.data.remote.dto.RouteDto
import com.roadrunner.app.data.remote.dto.RouteWithLicense
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(private val apiService: ApiService) {

    suspend fun getRoutesWithLicenseStatus(): Result<List<RouteWithLicense>> {
        return try {
            val routesResponse = apiService.getRoutes()
            if (!routesResponse.isSuccessful) {
                return Result.failure(Exception("Failed to load routes: ${routesResponse.code()}"))
            }
            val routes = routesResponse.body() ?: emptyList()
            val result = routes.map { route ->
                val licenseResult = checkLicenseStatus(route.id)
                RouteWithLicense(
                    route = route,
                    licenseStatus = licenseResult.first,
                    expiresAt = licenseResult.second,
                    licenseType = licenseResult.third,
                )
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRoute(id: String): Result<RouteDto> {
        return try {
            val response = apiService.getRoute(id)
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception("Route not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Returns Triple(LicenseStatus, expiresAt, LicenseType?) */
    suspend fun checkLicenseStatus(routeId: String): Triple<LicenseStatus, String?, LicenseType?> {
        return try {
            val response = apiService.checkLicense(LicenseCheckRequest(routeId))
            if (response.isSuccessful) {
                val body = response.body()!!
                val status = when {
                    body.licenseType == LicenseType.PERMANENT -> LicenseStatus.OWNED
                    body.expiresAt == null -> LicenseStatus.OWNED
                    else -> {
                        val expiryMillis = Instant.parse(body.expiresAt).toEpochMilli()
                        val nowMillis = System.currentTimeMillis()
                        val daysLeft = (expiryMillis - nowMillis) / (1000 * 60 * 60 * 24)
                        when {
                            daysLeft < 0 -> LicenseStatus.EXPIRED
                            daysLeft <= 3 -> LicenseStatus.EXPIRING_SOON
                            else -> LicenseStatus.ACTIVE
                        }
                    }
                }
                Triple(status, body.expiresAt, body.licenseType)
            } else {
                // 403 = no license; treat as Available
                Triple(LicenseStatus.AVAILABLE, null, null)
            }
        } catch (e: Exception) {
            Triple(LicenseStatus.AVAILABLE, null, null)
        }
    }
}
