package ru.lopon.domain.routing

import kotlinx.serialization.Serializable
import ru.lopon.domain.model.GeoCoordinate

interface RoutingService {

    suspend fun calculateRoute(
        waypoints: List<GeoCoordinate>,
        profile: RoutingProfile = RoutingProfile.BIKE
    ): Result<RoutingResult>

    suspend fun isAvailable(): Boolean

    fun getServiceType(): RoutingServiceType
}

enum class RoutingServiceType {
    ONLINE,

    OFFLINE,

    HYBRID
}

@Serializable
enum class RoutingProfile {
    BIKE,

    ROAD_BIKE,

    MTB,

    CITY_BIKE
}
@Serializable
data class RoutingResult(
    val points: List<GeoCoordinate>,

    val distanceMeters: Double,

    val durationSeconds: Long,

    val elevationGainMeters: Double? = null,

    val elevationLossMeters: Double? = null,

    val instructions: List<RoutingInstruction> = emptyList(),

    val elevationProfile: List<ElevationPoint> = emptyList(),

    val source: RoutingServiceType = RoutingServiceType.ONLINE
)

@Serializable
data class RoutingInstruction(
    val type: InstructionType,

    val text: String,

    val distanceMeters: Double,

    val pointIndex: Int,

    val streetName: String? = null
)

@Serializable
enum class InstructionType {
    CONTINUE,
    TURN_LEFT,
    TURN_RIGHT,
    TURN_SHARP_LEFT,
    TURN_SHARP_RIGHT,
    TURN_SLIGHT_LEFT,
    TURN_SLIGHT_RIGHT,
    U_TURN,
    KEEP_LEFT,
    KEEP_RIGHT,
    ROUNDABOUT,
    DESTINATION,
    WAYPOINT,
    UNKNOWN
}

@Serializable
data class ElevationPoint(
    val distanceMeters: Double,

    val elevationMeters: Double
)

sealed class RoutingException(message: String) : Exception(message) {
    class PointNotFound(message: String) : RoutingException(message)

    class RouteNotFound(message: String) : RoutingException(message)

    class ServiceUnavailable(message: String) : RoutingException(message)

    class NetworkError(message: String) : RoutingException(message)

    class OfflineDataMissing(message: String) : RoutingException(message)
}
