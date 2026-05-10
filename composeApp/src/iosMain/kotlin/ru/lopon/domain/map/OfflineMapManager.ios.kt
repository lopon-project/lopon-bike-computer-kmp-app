package ru.lopon.domain.map

import kotlinx.coroutines.flow.Flow

actual class OfflineMapManager(
    private val helper: PlatformOfflineMapHelper
) {
    actual suspend fun listRegions(): List<OfflineRegionInfo> = helper.listRegions()

    actual fun downloadRegion(
        name: String,
        bounds: CommonBounds,
        minZoom: Double,
        maxZoom: Double
    ): Flow<DownloadProgress> = helper.downloadRegion(name, bounds, minZoom, maxZoom)

    actual suspend fun deleteRegion(id: Long): Result<Unit> = helper.deleteRegion(id)

    actual fun estimateRegionSizeBytes(
        bounds: CommonBounds,
        minZoom: Double,
        maxZoom: Double
    ): Long = helper.estimateRegionSizeBytes(bounds, minZoom, maxZoom)
}
