package com.roadrunner.app.data.repository

import android.content.Context
import com.roadrunner.app.crypto.GpxCryptoManager
import com.roadrunner.app.data.remote.ApiService
import com.roadrunner.app.data.remote.dto.LicenseStatus
import com.roadrunner.app.data.remote.dto.LicenseType
import com.roadrunner.app.data.remote.dto.RouteDto
import com.roadrunner.app.data.remote.dto.RouteWithLicense
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(
    private val apiService: ApiService,
    private val gpxCryptoManager: GpxCryptoManager,
    private val licenseRepository: LicenseRepository,
    @ApplicationContext private val context: Context,
) {

    suspend fun getRoutesWithLicenseStatus(): Result<List<RouteWithLicense>> {
        return try {
            // Hydrate license cache first (failure is non-fatal — cache stays stale)
            licenseRepository.getMyLicenses()

            val routesResponse = apiService.getRoutes()
            if (!routesResponse.isSuccessful) {
                return Result.failure(Exception("Failed to load routes: ${routesResponse.code()}"))
            }
            val routes = routesResponse.body() ?: emptyList()
            val result = routes.map { route ->
                val (status, expiresAt, licenseType) = licenseRepository.computeLicenseStatus(route.id)
                RouteWithLicense(
                    route = route,
                    licenseStatus = status,
                    expiresAt = expiresAt,
                    licenseType = licenseType,
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

    /** Delegates to LicenseRepository.computeLicenseStatus() — reads from in-memory cache.
     *  Cache must be populated via getMyLicenses() before calling this.
     */
    suspend fun checkLicenseStatus(routeId: String): Triple<LicenseStatus, String?, LicenseType?> {
        return licenseRepository.computeLicenseStatus(routeId)
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
