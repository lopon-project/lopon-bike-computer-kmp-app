package ru.lopon.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.lopon.domain.model.Trip
import ru.lopon.domain.repository.TripRepository

// мок для тестов
class InMemoryTripRepository : TripRepository {

    private val trips = mutableMapOf<String, Trip>()
    private val _tripsFlow = MutableStateFlow<List<Trip>>(emptyList())

    override suspend fun saveTrip(trip: Trip): Result<Unit> {
        return try {
            trips[trip.id] = trip
            updateFlow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTrip(id: String): Trip? = trips[id]

    override suspend fun deleteTrip(id: String): Result<Unit> {
        return try {
            trips.remove(id)
            updateFlow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeTrips(): Flow<List<Trip>> = _tripsFlow.asStateFlow()

    override suspend fun listTrips(): List<Trip> = trips.values
        .sortedByDescending { it.startTimeUtc }


    suspend fun updateTrip(trip: Trip): Result<Unit> {
        return saveTrip(trip)
    }

    fun clear() {
        trips.clear()
        _tripsFlow.value = emptyList()
    }

    private fun updateFlow() {
        _tripsFlow.value = trips.values
            .sortedByDescending { it.startTimeUtc }
    }
}
