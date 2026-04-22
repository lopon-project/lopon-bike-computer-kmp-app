package ru.lopon.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.model.Route
import ru.lopon.domain.repository.RouteRepository
import ru.lopon.domain.routing.RoutingProfile
import ru.lopon.domain.usecase.CreateRouteUseCase

class RoutesViewModel(
    private val routeRepository: RouteRepository,
    private val createRouteUseCase: CreateRouteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState: StateFlow<RoutesUiState> = _uiState.asStateFlow()

    init {
        observeRoutes()
    }

    private fun observeRoutes() {
        viewModelScope.launch {
            routeRepository.observeRoutes().collect { routes ->
                _uiState.update { state ->
                    val selectedRoute = state.selectedRouteId?.let { selectedId ->
                        routes.firstOrNull { it.id == selectedId }?.id
                    }
                    state.copy(
                        routes = routes,
                        isLoading = false,
                        selectedRouteId = selectedRoute
                    )
                }
            }
        }
    }

    fun createQuickRoute() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorMessage = null) }
            val result = createRouteUseCase(
                waypoints = listOf(
                    GeoCoordinate(55.7558, 37.6173),
                    GeoCoordinate(55.7648, 37.6173)
                ),
                name = "Маршрут ${Clock.System.now().toEpochMilliseconds()}",
                profile = RoutingProfile.BIKE,
                saveToRepository = true
            )

            result.onSuccess {
                _uiState.update { it.copy(isCreating = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isCreating = false, errorMessage = "Не удалось создать маршрут: ${error.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun openRouteDetails(routeId: String) {
        _uiState.update { it.copy(selectedRouteId = routeId, errorMessage = null) }
    }

    fun closeRouteDetails() {
        _uiState.update { it.copy(selectedRouteId = null) }
    }

    fun requestDeleteRoute(routeId: String) {
        _uiState.update { it.copy(pendingDeleteRouteId = routeId) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(pendingDeleteRouteId = null) }
    }

    fun confirmDeleteRoute() {
        val routeId = _uiState.value.pendingDeleteRouteId ?: return
        viewModelScope.launch {
            val result = routeRepository.deleteRoute(routeId)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        pendingDeleteRouteId = null,
                        selectedRouteId = if (it.selectedRouteId == routeId) null else it.selectedRouteId
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        pendingDeleteRouteId = null,
                        errorMessage = "Не удалось удалить маршрут: ${error.message}"
                    )
                }
            }
        }
    }
}

data class RoutesUiState(
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val routes: List<Route> = emptyList(),
    val selectedRouteId: String? = null,
    val pendingDeleteRouteId: String? = null,
    val errorMessage: String? = null
)
