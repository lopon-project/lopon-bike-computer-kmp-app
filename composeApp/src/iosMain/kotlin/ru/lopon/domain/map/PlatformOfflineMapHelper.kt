package ru.lopon.domain.map

import kotlinx.coroutines.flow.Flow

interface PlatformOfflineMapHelper {

    suspend fun listRegions(): List<OfflineRegionInfo>

    fun downloadRegion(
        name: String,
        bounds: CommonBounds,
        minZoom: Double,
        maxZoom: Double
    ): Flow<DownloadProgress>

    suspend fun deleteRegion(id: Long): Result<Unit>

    fun estimateRegionSizeBytes(
        bounds: CommonBounds,
        minZoom: Double,
        maxZoom: Double
    ): Long
}

internal class NoOpOfflineMapHelper : PlatformOfflineMapHelper {

    override suspend fun listRegions(): List<OfflineRegionInfo> = emptyList()

    override fun downloadRegion(
        name: String,
        bounds: CommonBounds,
        minZoom: Double,
        maxZoom: Double
    ): Flow<DownloadProgress> = kotlinx.coroutines.flow.flowOf(
        DownloadProgress(0, 0, 0, 0f)
    )

    override suspend fun deleteRegion(id: Long): Result<Unit> =
        Result.failure(IllegalStateException("Offline map helper is not attached"))

    override fun estimateRegionSizeBytes(
        bounds: CommonBounds,
        minZoom: Double,
        maxZoom: Double
    ): Long = 0L
}
