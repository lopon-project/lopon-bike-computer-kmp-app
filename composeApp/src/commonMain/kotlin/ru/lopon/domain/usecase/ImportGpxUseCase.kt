package ru.lopon.domain.usecase

import ru.lopon.core.IdGenerator
import ru.lopon.core.gpx.GpxParser
import ru.lopon.domain.model.Route
import ru.lopon.domain.repository.RouteRepository
import ru.lopon.platform.FileStorage

class ImportGpxUseCase(
    private val fileStorage: FileStorage,
    private val gpxParser: GpxParser,
    private val routeRepository: RouteRepository,
    private val idGenerator: IdGenerator
) {

    suspend operator fun invoke(filePath: String): Result<Route> {
        return fileStorage.readText(filePath)
            .mapCatching { content ->
                gpxParser.parseFirst(content).getOrThrow()
            }
            .mapCatching { route -> saveRoute(route) }
    }

    suspend fun importFromContent(gpxContent: String): Result<Route> {
        return gpxParser.parseFirst(gpxContent)
            .mapCatching { route -> saveRoute(route) }
    }

    suspend fun importAll(filePath: String): Result<List<Route>> {
        return fileStorage.readText(filePath)
            .mapCatching { content ->
                gpxParser.parse(content).getOrThrow()
            }
            .mapCatching { routes -> routes.map { route -> saveRoute(route) } }
    }

    private suspend fun saveRoute(route: Route): Route {
        val routeWithId = route.copy(id = idGenerator.generateId())
        routeRepository.saveRoute(routeWithId).getOrThrow()
        return routeWithId
    }
}
