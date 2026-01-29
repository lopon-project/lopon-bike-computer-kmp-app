package ru.lopon.domain.usecase

import ru.lopon.core.IdGenerator
import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.model.Route
import ru.lopon.domain.repository.RouteRepository
import ru.lopon.domain.routing.RoutingProfile
import ru.lopon.domain.routing.RoutingService

class CreateRouteUseCase(
    private val routingService: RoutingService,
    private val routeRepository: RouteRepository,
    private val idGenerator: IdGenerator
) {

    suspend operator fun invoke(
        waypoints: List<GeoCoordinate>,
        name: String,
        profile: RoutingProfile = RoutingProfile.BIKE,
        saveToRepository: Boolean = true
    ): Result<RouteWithDetails> {
        if (waypoints.size < 2) {
            return Result.failure(
                IllegalArgumentException("At least 2 waypoints required")
            )
        }

        return routingService.calculateRoute(waypoints, profile)
            .mapCatching { routingResult ->
                val route = Route(
                    id = idGenerator.generateId(),
                    name = name,
                    points = routingResult.points
                )

                if (saveToRepository) {
                    routeRepository.saveRoute(route).getOrThrow()
                }

                RouteWithDetails(
                    route = route,
                    distanceMeters = routingResult.distanceMeters,
                    durationSeconds = routingResult.durationSeconds,
                    elevationGainMeters = routingResult.elevationGainMeters,
                    elevationLossMeters = routingResult.elevationLossMeters,
                    instructions = routingResult.instructions,
                    profile = profile
                )
            }
    }

    suspend fun isRoutingAvailable(): Boolean = routingService.isAvailable()
}

data class RouteWithDetails(
    val route: Route,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val elevationGainMeters: Double?,
    val elevationLossMeters: Double?,
    val instructions: List<ru.lopon.domain.routing.RoutingInstruction>,
    val profile: RoutingProfile
)
