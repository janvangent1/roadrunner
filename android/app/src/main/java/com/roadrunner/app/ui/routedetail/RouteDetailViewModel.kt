package com.roadrunner.app.ui.routedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadrunner.app.data.local.NavigationSessionManager
import com.roadrunner.app.data.remote.dto.LicenseStatus
import com.roadrunner.app.data.remote.dto.LicenseType
import com.roadrunner.app.data.remote.dto.RouteDto
import com.roadrunner.app.data.repository.LicenseRepository
import com.roadrunner.app.data.repository.RouteRepository
import com.roadrunner.app.data.tilecache.TileCacheManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ticofab.androidgpxparser.parser.GPXParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import javax.inject.Inject

data class RouteDetailUiState(
    val route: RouteDto? = null,
    val licenseStatus: LicenseStatus = LicenseStatus.AVAILABLE,
    val expiresAt: String? = null,
    val licenseType: LicenseType? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isStartingNavigation: Boolean = false,
    val navigationError: String? = null,
    val isTilesCached: Boolean = false,
    val canDownloadTiles: Boolean = false,
    val isDownloadingTiles: Boolean = false,
)

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    val routeRepository: RouteRepository,
    private val licenseRepository: LicenseRepository,
    val sessionManager: NavigationSessionManager,
    private val tileCacheManager: TileCacheManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val routeId: String = checkNotNull(savedStateHandle["routeId"])
    private val _uiState = MutableStateFlow(RouteDetailUiState())
    val uiState: StateFlow<RouteDetailUiState> = _uiState.asStateFlow()

    init { loadRoute() }

    fun loadRoute() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // Hydrate license cache first — ensures computeLicenseStatus has data
            licenseRepository.getMyLicenses()
            val routeResult = routeRepository.getRoute(routeId)
            val licenseResult = routeRepository.checkLicenseStatus(routeId)
            routeResult
                .onSuccess { route ->
                    val isTilesCached = tileCacheManager.isTilesCached(routeId)
                    val licenseStatus = licenseResult.first
                    val canDownloadTiles = licenseStatus in listOf(
                        LicenseStatus.OWNED, LicenseStatus.ACTIVE, LicenseStatus.EXPIRING_SOON
                    ) && !isTilesCached
                    _uiState.update {
                        it.copy(
                            route = route,
                            licenseStatus = licenseStatus,
                            expiresAt = licenseResult.second,
                            licenseType = licenseResult.third,
                            isLoading = false,
                            isTilesCached = isTilesCached,
                            canDownloadTiles = canDownloadTiles,
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(error = err.message, isLoading = false) }
                }
        }
    }

    fun startNavigation(onNavigate: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isStartingNavigation = true, navigationError = null) }
            licenseRepository.checkLicense(routeId)
                .onSuccess { session ->
                    sessionManager.storeSession(session.sessionToken, session.sessionExpiresAt)
                    _uiState.update { it.copy(isStartingNavigation = false) }
                    onNavigate()
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isStartingNavigation = false, navigationError = err.message) }
                }
        }
    }

    fun downloadTiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingTiles = true) }
            routeRepository.getDecryptedGpx(routeId)
                .onSuccess { bytes ->
                    try {
                        val gpx = GPXParser().parse(ByteArrayInputStream(bytes))
                        var minLat = Double.MAX_VALUE
                        var maxLat = -Double.MAX_VALUE
                        var minLon = Double.MAX_VALUE
                        var maxLon = -Double.MAX_VALUE

                        gpx?.tracks?.forEach { track ->
                            track?.trackSegments?.forEach { segment ->
                                segment?.trackPoints?.forEach { point ->
                                    val lat = point?.latitude ?: return@forEach
                                    val lon = point.longitude
                                    if (lat < minLat) minLat = lat
                                    if (lat > maxLat) maxLat = lat
                                    if (lon < minLon) minLon = lon
                                    if (lon > maxLon) maxLon = lon
                                }
                            }
                        }

                        if (minLat != Double.MAX_VALUE) {
                            // Add 0.01 degree padding on all sides
                            tileCacheManager.enqueueTileDownload(
                                routeId = routeId,
                                minLat = minLat - 0.01,
                                maxLat = maxLat + 0.01,
                                minLon = minLon - 0.01,
                                maxLon = maxLon + 0.01,
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("RouteDetailViewModel", "GPX parse failed: ${e.message}")
                    }
                }
                .onFailure { err ->
                    android.util.Log.w("RouteDetailViewModel", "GPX load failed: ${err.message}")
                }
            _uiState.update { it.copy(isDownloadingTiles = false) }
        }
    }

    fun clearNavigationError() {
        _uiState.update { it.copy(navigationError = null) }
    }
}
