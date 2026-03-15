package com.roadrunner.app.data.repository

import android.content.Context
import com.roadrunner.app.crypto.GpxCryptoManager
import com.roadrunner.app.data.remote.ApiService
import com.roadrunner.app.data.remote.dto.LicenseCheckRequest
import com.roadrunner.app.data.remote.dto.LicenseStatus
import com.roadrunner.app.data.remote.dto.LicenseType
import com.roadrunner.app.data.remote.dto.RouteDto
import com.roadrunner.app.data.remote.dto.RouteWithLicense
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(
    private val apiService: ApiService,
    private val gpxCryptoManager: GpxCryptoManager,
    @ApplicationContext private val context: Context,
) {

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

    /**
     * Downloads the encrypted GPX blob from the server and writes it directly to
     * filesDir/gpx/{routeId}.enc. The raw (already-encrypted) server response bytes
     * are written to disk — no plaintext GPX ever touches the filesystem.
     */
    suspend fun downloadAndStoreGpx(routeId: String): Result<Unit> {
        return try {
            val gpxDir = File(context.filesDir, "gpx")
            gpxDir.mkdirs()

            val response = apiService.getRouteGpx(routeId)
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to download GPX: ${response.code()}"))
            }

            val encFile = File(gpxDir, "$routeId.enc")
            response.body()!!.byteStream().use { input ->
                FileOutputStream(encFile).use { output ->
                    input.copyTo(output)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns the decrypted GPX bytes for the given route, entirely in memory.
     * If the .enc file does not exist locally, triggers a download first.
     * The plaintext bytes are never written to disk.
     */
    suspend fun getDecryptedGpx(routeId: String): Result<ByteArray> {
        return try {
            if (!gpxCryptoManager.encryptedFileExists(routeId, context.filesDir)) {
                val downloadResult = downloadAndStoreGpx(routeId)
                if (downloadResult.isFailure) {
                    return Result.failure(downloadResult.exceptionOrNull()!!)
                }
            }
            val encFile = File(context.filesDir, "gpx/$routeId.enc")
            val bytes = gpxCryptoManager.decryptToByteArray(routeId, encFile)
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
