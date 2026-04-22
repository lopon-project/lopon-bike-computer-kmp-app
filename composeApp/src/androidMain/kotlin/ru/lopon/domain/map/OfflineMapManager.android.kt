package ru.lopon.domain.map

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import ru.lopon.platform.MapLibreOfflineHelper
import ru.lopon.platform.DownloadProgress as AndroidDownloadProgress
import ru.lopon.platform.OfflineRegionInfo as AndroidOfflineRegionInfo

actual class OfflineMapManager(
    private val helper: MapLibreOfflineHelper
) {
    actual suspend fun listRegions(): List<OfflineRegionInfo> =
        helper.listRegions().map { it.toCommon() }

    actual fun downloadRegion(
        name: String,
        bounds: CommonBounds,
        minZoom: Double,
        maxZoom: Double
    ): Flow<DownloadProgress> =
        helper.downloadRegion(name, bounds.toMapLibre(), minZoom, maxZoom)
            .map { it.toCommon() }

    actual suspend fun deleteRegion(id: Long): Result<Unit> = helper.deleteRegion(id)

    actual fun estimateRegionSizeBytes(
        bounds: CommonBounds,
        minZoom: Double,
        maxZoom: Double
    ): Long = helper.estimateRegionSizeBytes(bounds.toMapLibre(), minZoom, maxZoom)
}

private fun CommonBounds.toMapLibre(): LatLngBounds = LatLngBounds.Builder()
    .include(LatLng(southWest.lat, southWest.lng))
    .include(LatLng(northEast.lat, northEast.lng))
    .build()

private fun AndroidOfflineRegionInfo.toCommon(): OfflineRegionInfo = OfflineRegionInfo(
    id = id,
    name = name,
    completedResources = completedResources,
    requiredResources = requiredResources,
    completedSize = completedSize,
    isComplete = isComplete
)

private fun AndroidDownloadProgress.toCommon(): DownloadProgress = DownloadProgress(
    completedResources = completedResources,
    requiredResources = requiredResources,
    completedSize = completedSize,
    percentage = percentage
)
