package ru.lopon.domain.map

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual class OfflineMapManager(
    private val helper: PlatformOfflineMapHelper
) {
    actual suspend fun listRegions(): List<OfflineRegionInfo> = helper.listRegions()

    actual fun downloadRegion(
        name: String,
        bounds: CommonBounds,
        minZoom: Double,
        maxZoom: Double
    ): Flow<DownloadProgress> = callbackFlow {
        helper.startDownloadRegion(
            name = name,
            bounds = bounds,
            minZoom = minZoom,
            maxZoom = maxZoom,
            onProgress = { progress -> trySend(progress) },
            onComplete = { errorMessage ->
                if (errorMessage != null) {
                    close(Exception(errorMessage))
                } else {
                    close()
                }
            }
        )
        awaitClose { }
    }

    actual suspend fun deleteRegion(id: Long): Result<Unit> = helper.deleteRegion(id)

    actual fun estimateRegionSizeBytes(
        bounds: CommonBounds,
        minZoom: Double,
        maxZoom: Double
    ): Long = helper.estimateRegionSizeBytes(bounds, minZoom, maxZoom)
}
