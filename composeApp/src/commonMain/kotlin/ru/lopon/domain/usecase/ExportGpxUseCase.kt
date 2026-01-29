package ru.lopon.domain.usecase

import ru.lopon.core.gpx.GpxSerializer
import ru.lopon.domain.model.TrackPoint
import ru.lopon.domain.model.Trip
import ru.lopon.domain.repository.TripRepository
import ru.lopon.platform.FileStorage

class ExportGpxUseCase(
    private val fileStorage: FileStorage,
    private val gpxSerializer: GpxSerializer,
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(
        tripId: String,
        trackPoints: List<TrackPoint>,
        outputPath: String
    ): Result<String> {
        val trip = getTrip(tripId).getOrElse { return Result.failure(it) }
        val gpxContent = gpxSerializer.serializeTrip(trip, trackPoints)
        return fileStorage.writeText(outputPath, gpxContent).map { outputPath }
    }

    suspend fun toContent(
        tripId: String,
        trackPoints: List<TrackPoint>,
        name: String? = null
    ): Result<String> {
        val trip = getTrip(tripId).getOrElse { return Result.failure(it) }
        val trackName = name ?: "Trip ${trip.id}"
        val gpxContent = gpxSerializer.serializeTrip(trip, trackPoints, trackName)
        return Result.success(gpxContent)
    }

    private suspend fun getTrip(tripId: String): Result<Trip> {
        val trip = tripRepository.getTrip(tripId)
            ?: return Result.failure(IllegalArgumentException("Trip not found: $tripId"))
        return Result.success(trip)
    }
}
