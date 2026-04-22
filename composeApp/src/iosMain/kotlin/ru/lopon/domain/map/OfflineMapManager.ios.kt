package ru.lopon.domain.map

import kotlinx.coroutines.flow.Flow

actual class OfflineMapManager {
    actual suspend fun listRegions(): List<OfflineRegionInfo> {
        TODO("Not yet implemented")
    }

    actual fun downloadRegion(
        name: String,
        bounds: CommonBounds,
        minZoom: Double,
        maxZoom: Double
    ): Flow<DownloadProgress> {
        TODO("Not yet implemented")
    }

    actual suspend fun deleteRegion(id: Long): Result<Unit> {
        TODO("Not yet implemented")
    }

    actual fun estimateRegionSizeBytes(
        bounds: CommonBounds,
        minZoom: Double,
        maxZoom: Double
    ): Long {
        TODO("Not yet implemented")
    }
}