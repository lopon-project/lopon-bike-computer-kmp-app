package ru.lopon.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.lopon.domain.model.Route

interface RouteRepository {
    suspend fun saveRoute(route: Route): Result<Unit>

    suspend fun getRoute(id: String): Route?

    suspend fun deleteRoute(id: String): Result<Unit>

    fun observeRoutes(): Flow<List<Route>>

    suspend fun listRoutes(): List<Route>
}

