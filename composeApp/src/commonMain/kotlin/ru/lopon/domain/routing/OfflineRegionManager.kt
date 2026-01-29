package ru.lopon.domain.routing

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

interface OfflineRegionManager {
    suspend fun getAvailableRegions(): Result<List<OfflineRegion>>

    suspend fun getDownloadedRegions(): List<OfflineRegion>

    fun downloadRegion(regionId: String): Flow<DownloadProgress>

    suspend fun cancelDownload(regionId: String)

    suspend fun deleteRegion(regionId: String): Result<Unit>

    suspend fun isPointCovered(latitude: Double, longitude: Double): Boolean

    suspend fun getTotalDownloadedSize(): Long
}


@Serializable
data class OfflineRegion(
    val id: String,

    val name: String,

    val country: String,

    val sizeBytes: Long,

    val status: RegionStatus = RegionStatus.NOT_DOWNLOADED,

    val lastUpdated: Long? = null,

    val bounds: RegionBounds? = null
)

@Serializable
data class RegionBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
) {
    fun contains(lat: Double, lon: Double): Boolean {
        return lat in minLat..maxLat && lon in minLon..maxLon
    }
}

@Serializable
enum class RegionStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    UPDATE_AVAILABLE,
    ERROR
}

@Serializable
data class DownloadProgress(
    val regionId: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val status: DownloadStatus
) {
    val progressPercent: Float
        get() = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes * 100) else 0f
}

@Serializable
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}
