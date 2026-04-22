package ru.lopon.domain.map

import kotlinx.coroutines.flow.Flow

data class CommonLatLng(val lat: Double, val lng: Double)

data class CommonBounds(
    val southWest: CommonLatLng,
    val northEast: CommonLatLng
)

data class OfflineRegionInfo(
    val id: Long,
    val name: String,
    val completedResources: Long,
    val requiredResources: Long,
    val completedSize: Long,
    val isComplete: Boolean
)

data class DownloadProgress(
    val completedResources: Long,
    val requiredResources: Long,
    val completedSize: Long,
    val percentage: Float
)

expect class OfflineMapManager {
    suspend fun listRegions(): List<OfflineRegionInfo>

    fun downloadRegion(
        name: String,
        bounds: CommonBounds,
        minZoom: Double = 8.0,
        maxZoom: Double = 16.0
    ): Flow<DownloadProgress>

    suspend fun deleteRegion(id: Long): Result<Unit>

    fun estimateRegionSizeBytes(
        bounds: CommonBounds,
        minZoom: Double = 8.0,
        maxZoom: Double = 16.0
    ): Long
}
