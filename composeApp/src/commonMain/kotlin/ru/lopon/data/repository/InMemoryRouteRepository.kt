package ru.lopon.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.lopon.domain.model.Route
import ru.lopon.domain.repository.RouteRepository

class InMemoryRouteRepository : RouteRepository {

    private val routes = mutableMapOf<String, Route>()
    private val _routesFlow = MutableStateFlow<List<Route>>(emptyList())

    override suspend fun saveRoute(route: Route): Result<Unit> {
        return try {
            routes[route.id] = route
            _routesFlow.value = routes.values.toList()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRoute(id: String): Route? = routes[id]

    override suspend fun deleteRoute(id: String): Result<Unit> {
        return try {
            routes.remove(id)
            _routesFlow.value = routes.values.toList()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeRoutes(): Flow<List<Route>> = _routesFlow.asStateFlow()

    override suspend fun listRoutes(): List<Route> = routes.values.toList()

    /**
     * Очищает все маршруты. Используется для тестов.
     */
    fun clear() {
        routes.clear()
        _routesFlow.value = emptyList()
    }
}
