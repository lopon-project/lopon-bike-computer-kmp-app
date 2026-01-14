package ru.lopon.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.lopon.domain.model.Trip


interface TripRepository {
    suspend fun saveTrip(trip: Trip): Result<Unit>

    suspend fun getTrip(id: String): Trip?

    suspend fun deleteTrip(id: String): Result<Unit>

    fun observeTrips(): Flow<List<Trip>>

    suspend fun listTrips(): List<Trip>
}

