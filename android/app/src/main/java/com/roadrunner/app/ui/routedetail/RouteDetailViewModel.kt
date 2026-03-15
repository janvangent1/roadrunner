package com.roadrunner.app.ui.routedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadrunner.app.data.remote.dto.LicenseStatus
import com.roadrunner.app.data.remote.dto.LicenseType
import com.roadrunner.app.data.remote.dto.RouteDto
import com.roadrunner.app.data.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteDetailUiState(
    val route: RouteDto? = null,
    val licenseStatus: LicenseStatus = LicenseStatus.AVAILABLE,
    val expiresAt: String? = null,
    val licenseType: LicenseType? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val routeId: String = checkNotNull(savedStateHandle["routeId"])
    private val _uiState = MutableStateFlow(RouteDetailUiState())
    val uiState: StateFlow<RouteDetailUiState> = _uiState.asStateFlow()

    init { loadRoute() }

    fun loadRoute() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val routeResult = routeRepository.getRoute(routeId)
            val licenseResult = routeRepository.checkLicenseStatus(routeId)
            routeResult
                .onSuccess { route ->
                    _uiState.update {
                        it.copy(
                            route = route,
                            licenseStatus = licenseResult.first,
                            expiresAt = licenseResult.second,
                            licenseType = licenseResult.third,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(error = err.message, isLoading = false) }
                }
        }
    }
}
