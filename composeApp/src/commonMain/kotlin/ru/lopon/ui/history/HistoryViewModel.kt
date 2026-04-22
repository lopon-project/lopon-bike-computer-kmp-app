package ru.lopon.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.lopon.domain.model.Trip
import ru.lopon.domain.repository.TripRepository

class HistoryViewModel(
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        observeTrips()
    }

    private fun observeTrips() {
        viewModelScope.launch {
            tripRepository.observeTrips().collect { trips ->
                _uiState.update { state ->
                    val selectedTrip = state.selectedTripId?.let { selectedId ->
                        trips.firstOrNull { it.id == selectedId }?.id
                    }
                    state.copy(
                        trips = trips,
                        isLoading = false,
                        selectedTripId = selectedTrip
                    )
                }
            }
        }
    }

    fun openTripDetails(tripId: String) {
        _uiState.update { it.copy(selectedTripId = tripId, errorMessage = null) }
    }

    fun closeTripDetails() {
        _uiState.update { it.copy(selectedTripId = null) }
    }

    fun requestDeleteTrip(tripId: String) {
        _uiState.update { it.copy(pendingDeleteTripId = tripId) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(pendingDeleteTripId = null) }
    }

    fun confirmDeleteTrip() {
        val tripId = _uiState.value.pendingDeleteTripId ?: return
        viewModelScope.launch {
            val result = tripRepository.deleteTrip(tripId)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        pendingDeleteTripId = null,
                        selectedTripId = if (it.selectedTripId == tripId) null else it.selectedTripId
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        pendingDeleteTripId = null,
                        errorMessage = "Не удалось удалить поездку: ${error.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class HistoryUiState(
    val isLoading: Boolean = true,
    val trips: List<Trip> = emptyList(),
    val selectedTripId: String? = null,
    val pendingDeleteTripId: String? = null,
    val errorMessage: String? = null
)
