package com.roadrunner.app.ui.myroutes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadrunner.app.data.remote.dto.LicenseStatus
import com.roadrunner.app.data.repository.RouteRepository
import com.roadrunner.app.ui.catalog.CatalogUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyRoutesViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        loadMyRoutes()
    }

    fun loadMyRoutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            routeRepository.getRoutesWithLicenseStatus()
                .onSuccess { all ->
                    val myRoutes = all.filter {
                        it.licenseStatus in listOf(
                            LicenseStatus.OWNED,
                            LicenseStatus.ACTIVE,
                            LicenseStatus.EXPIRING_SOON,
                        )
                    }
                    _uiState.update { it.copy(routes = myRoutes, isLoading = false) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(error = err.message, isLoading = false) }
                }
        }
    }
}
