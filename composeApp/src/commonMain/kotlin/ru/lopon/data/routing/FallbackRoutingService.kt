package ru.lopon.data.routing

import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.routing.RoutingProfile
import ru.lopon.domain.routing.RoutingResult
import ru.lopon.domain.routing.RoutingService
import ru.lopon.domain.routing.RoutingServiceType

class FallbackRoutingService(
    private val primary: RoutingService,
    private val fallback: RoutingService,
    private val fallbackProfile: RoutingProfile = RoutingProfile.FOOT_WALKING
) : RoutingService {

    override suspend fun calculateRoute(
        waypoints: List<GeoCoordinate>,
        profile: RoutingProfile
    ): Result<RoutingResult> {
        val primaryResult = primary.calculateRoute(waypoints, profile)
        if (primaryResult.isSuccess) return primaryResult

        return fallback.calculateRoute(waypoints, fallbackProfile)
    }

    override suspend fun isAvailable(): Boolean =
        primary.isAvailable() || fallback.isAvailable()

    override fun getServiceType(): RoutingServiceType =
        primary.getServiceType()
}
