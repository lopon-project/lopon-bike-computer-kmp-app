package ru.lopon.domain.map

actual class OfflineMapManager {
    actual suspend fun listRegions(): List<ru.lopon.domain.map.OfflineRegionInfo> {
        TODO("Not yet implemented")
    }

    actual fun downloadRegion(
        name: String,
        bounds: ru.lopon.domain.map.CommonBounds,
        minZoom: Double,
        maxZoom: Double
    ): kotlinx.coroutines.flow.Flow<ru.lopon.domain.map.DownloadProgress> {
        TODO("Not yet implemented")
    }

    actual suspend fun deleteRegion(id: Long): kotlin.Result<Unit> {
        TODO("Not yet implemented")
    }

    actual fun estimateRegionSizeBytes(
        bounds: ru.lopon.domain.map.CommonBounds,
        minZoom: Double,
        maxZoom: Double
    ): Long {
        TODO("Not yet implemented")
    }
}