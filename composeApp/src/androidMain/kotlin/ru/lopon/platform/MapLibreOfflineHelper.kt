package ru.lopon.platform

import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import org.maplibre.android.geometry.LatLngBounds
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.ln
import kotlin.math.tan

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

class MapLibreOfflineHelper(private val context: Context) {

    private val offlineManager: OfflineManager by lazy {
        OfflineManager.getInstance(context)
    }

    suspend fun listRegions(): List<OfflineRegionInfo> {
        val regions = suspendCancellableCoroutine { cont ->
            offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    cont.resume(offlineRegions ?: emptyArray())
                }

                override fun onError(error: String) {
                    cont.resumeWithException(RuntimeException(error))
                }
            })
        }

        return regions.map { region ->
            val metadata = String(region.metadata, Charsets.UTF_8)
            val status = getRegionStatus(region)
            OfflineRegionInfo(
                id = region.id,
                name = metadata.ifEmpty { "Регион ${region.id}" },
                completedResources = status.completedResourceCount,
                requiredResources = status.requiredResourceCount,
                completedSize = status.completedResourceSize,
                isComplete = status.isComplete
            )
        }
    }

    private suspend fun getRegionStatus(region: OfflineRegion): OfflineRegionStatus =
        suspendCancellableCoroutine { cont ->
            region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                override fun onStatus(status: OfflineRegionStatus?) {
                    if (status != null) {
                        cont.resume(status)
                    } else {
                        cont.resumeWithException(RuntimeException("Status is null"))
                    }
                }

                override fun onError(error: String?) {
                    cont.resumeWithException(RuntimeException(error ?: "Unknown error"))
                }
            })
        }

    fun downloadRegion(
        name: String,
        bounds: LatLngBounds,
        minZoom: Double = 8.0,
        maxZoom: Double = 16.0,
        styleUrl: String = "https://tiles.openfreemap.org/styles/liberty"
    ): Flow<DownloadProgress> = callbackFlow {
        val definition = OfflineTilePyramidRegionDefinition(
            styleUrl,
            bounds,
            minZoom,
            maxZoom,
            context.resources.displayMetrics.density
        )
        val metadata = name.toByteArray(Charsets.UTF_8)

        offlineManager.createOfflineRegion(
            definition,
            metadata,
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {
                    offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
                        override fun onStatusChanged(status: OfflineRegionStatus) {
                            val progress = DownloadProgress(
                                completedResources = status.completedResourceCount,
                                requiredResources = status.requiredResourceCount,
                                completedSize = status.completedResourceSize,
                                percentage = if (status.requiredResourceCount > 0)
                                    status.completedResourceCount.toFloat() / status.requiredResourceCount
                                else 0f
                            )
                            trySend(progress)
                            if (status.isComplete) {
                                close()
                            }
                        }

                        override fun onError(error: OfflineRegionError) {
                            close(RuntimeException(error.message))
                        }

                        override fun mapboxTileCountLimitExceeded(limit: Long) {
                            close(RuntimeException("Tile limit exceeded: $limit"))
                        }
                    })
                    offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                }

                override fun onError(error: String) {
                    close(RuntimeException(error))
                }
            }
        )

        awaitClose { }
    }

    suspend fun deleteRegion(id: Long): Result<Unit> = suspendCancellableCoroutine { cont ->
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                val region = offlineRegions?.firstOrNull { it.id == id }
                if (region == null) {
                    cont.resume(Result.failure(RuntimeException("Region not found")))
                    return
                }
                region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                    override fun onDelete() {
                        cont.resume(Result.success(Unit))
                    }

                    override fun onError(error: String) {
                        cont.resume(Result.failure(RuntimeException(error)))
                    }
                })
            }

            override fun onError(error: String) {
                cont.resume(Result.failure(RuntimeException(error)))
            }
        })
    }

    fun estimateRegionSizeBytes(
        bounds: LatLngBounds,
        minZoom: Double = 8.0,
        maxZoom: Double = 16.0
    ): Long {
        val avgTileSizeBytes = 20_000L // ~20 KB for vector tiles
        var totalTiles = 0L

        for (z in minZoom.toInt()..maxZoom.toInt()) {
            val n = 2.0.pow(z)
            val xMin = floor((bounds.longitudeWest + 180.0) / 360.0 * n).toLong()
            val xMax = floor((bounds.longitudeEast + 180.0) / 360.0 * n).toLong()
            val yMin = floor((1.0 - ln(
                tan(Math.toRadians(bounds.latitudeNorth)) +
                        1.0 / cos(Math.toRadians(bounds.latitudeNorth))
            ) / Math.PI) / 2.0 * n).toLong()
            val yMax = floor((1.0 - ln(
                tan(Math.toRadians(bounds.latitudeSouth)) +
                        1.0 / cos(Math.toRadians(bounds.latitudeSouth))
            ) / Math.PI) / 2.0 * n).toLong()
            totalTiles += (xMax - xMin + 1) * (yMax - yMin + 1)
        }

        return totalTiles * avgTileSizeBytes
    }
}
