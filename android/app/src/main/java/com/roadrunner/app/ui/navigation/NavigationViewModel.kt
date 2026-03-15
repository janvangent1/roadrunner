package com.roadrunner.app.ui.navigation

import android.location.Location
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadrunner.app.data.location.LocationRepository
import com.roadrunner.app.data.remote.dto.WaypointDto
import com.roadrunner.app.data.repository.RouteRepository
import com.roadrunner.app.navigation.OffRouteDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ticofab.androidgpxparser.parser.GPXParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.io.ByteArrayInputStream
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class NavigationUiState(
    val location: Location? = null,
    val speedKmh: Float = 0f,
    val distanceCoveredKm: Double = 0.0,
    val distanceRemainingKm: Double = 0.0,
    val elapsedSeconds: Long = 0L,
    val isOffRoute: Boolean = false,
    val routePoints: List<GeoPoint> = emptyList(),
    val waypoints: List<WaypointDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val routeRepository: RouteRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val routeId: String = checkNotNull(savedStateHandle["routeId"])

    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState

    private var totalDistanceKm: Double = 0.0
    private var previousLocation: Location? = null
    private var distanceCoveredKm: Double = 0.0

    init {
        loadRoute()
    }

    private fun loadRoute() {
        viewModelScope.launch {
            val routeResult = routeRepository.getRoute(routeId)
            if (routeResult.isFailure) {
                _uiState.update { it.copy(isLoading = false, error = routeResult.exceptionOrNull()?.message) }
                return@launch
            }
            val route = routeResult.getOrThrow()
            totalDistanceKm = route.distanceKm

            val gpxResult = routeRepository.getDecryptedGpx(routeId)
            val routePoints = if (gpxResult.isSuccess) {
                try {
                    val gpx = GPXParser().parse(ByteArrayInputStream(gpxResult.getOrThrow()))
                    gpx.tracks
                        .firstOrNull()
                        ?.trackSegments
                        ?.firstOrNull()
                        ?.trackPoints
                        ?.map { GeoPoint(it.latitude, it.longitude) }
                        ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    routePoints = routePoints,
                    waypoints = route.waypoints,
                    distanceRemainingKm = totalDistanceKm,
                )
            }

            // Start elapsed-time ticker after route loaded
            startElapsedTicker()
        }
    }

    private fun startElapsedTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    fun startTracking() {
        locationRepository.startLocationUpdates()
        viewModelScope.launch {
            locationRepository.locationFlow.collect { location ->
                location ?: return@collect

                val speedKmh = location.speed * 3.6f

                // Accumulate distance
                val prevLoc = previousLocation
                if (prevLoc != null) {
                    distanceCoveredKm += haversineMetres(prevLoc, location) / 1000.0
                }
                previousLocation = location

                val remaining = (totalDistanceKm - distanceCoveredKm).coerceAtLeast(0.0)

                // Off-route hysteresis
                val currentPoint = GeoPoint(location.latitude, location.longitude)
                val routePoints = _uiState.value.routePoints
                val dist = OffRouteDetector.minDistanceToPolyline(currentPoint, routePoints)
                val wasOffRoute = _uiState.value.isOffRoute
                val isOffRoute = when {
                    !wasOffRoute && dist > 50.0 -> true
                    wasOffRoute && dist < 40.0 -> false
                    else -> wasOffRoute
                }

                _uiState.update {
                    it.copy(
                        location = location,
                        speedKmh = speedKmh,
                        distanceCoveredKm = distanceCoveredKm,
                        distanceRemainingKm = remaining,
                        isOffRoute = isOffRoute,
                    )
                }
            }
        }
    }

    fun stopTracking() {
        locationRepository.stopLocationUpdates()
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }

    private fun haversineMetres(a: Location, b: Location): Double {
        val r = 6_371_000.0
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2.0 * r * atan2(sqrt(h), sqrt(1.0 - h))
    }
}
