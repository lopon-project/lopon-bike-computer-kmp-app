package ru.lopon.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.lopon.domain.model.TrackPoint

interface LocationRepository {
    fun observeLocation(): Flow<TrackPoint>

    fun observeGpsAvailability(): Flow<Boolean>

    suspend fun startTracking(): Result<Unit>

    suspend fun stopTracking()

    suspend fun getLastKnownLocation(): TrackPoint?

    suspend fun isTracking(): Boolean
}

