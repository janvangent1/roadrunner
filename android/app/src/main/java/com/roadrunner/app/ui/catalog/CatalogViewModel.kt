package com.roadrunner.app.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadrunner.app.data.remote.dto.RouteWithLicense
import com.roadrunner.app.data.repository.AuthRepository
import com.roadrunner.app.data.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CatalogUiState(
    val routes: List<RouteWithLicense> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        loadRoutes()
    }

    fun loadRoutes(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh, error = null) }
            routeRepository.getRoutesWithLicenseStatus()
                .onSuccess { routes ->
                    _uiState.update { it.copy(routes = routes, isLoading = false, isRefreshing = false) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(error = err.message, isLoading = false, isRefreshing = false) }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
