package ru.lopon.domain.map

interface PlatformOfflineMapHelper {

    suspend fun listRegions(): List<OfflineRegionInfo>

    fun startDownloadRegion(
        name: String,
        bounds: CommonBounds,
        minZoom: Double,
        maxZoom: Double,
        onProgress: (DownloadProgress) -> Unit,
        onComplete: (String?) -> Unit
    )

    suspend fun deleteRegion(id: Long): Result<Unit>

    fun estimateRegionSizeBytes(
        bounds: CommonBounds,
        minZoom: Double,
        maxZoom: Double
    ): Long
}

internal class NoOpOfflineMapHelper : PlatformOfflineMapHelper {

    override suspend fun listRegions(): List<OfflineRegionInfo> = emptyList()

    override fun startDownloadRegion(
        name: String,
        bounds: CommonBounds,
        minZoom: Double,
        maxZoom: Double,
        onProgress: (DownloadProgress) -> Unit,
        onComplete: (String?) -> Unit
    ) {
        onComplete("Offline map helper is not attached")
    }

    override suspend fun deleteRegion(id: Long): Result<Unit> =
        Result.failure(IllegalStateException("Offline map helper is not attached"))

    override fun estimateRegionSizeBytes(
        bounds: CommonBounds,
        minZoom: Double,
        maxZoom: Double
    ): Long = 0L
}
