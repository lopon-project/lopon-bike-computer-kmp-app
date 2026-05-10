package ru.lopon.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.lopon.data.routing.FallbackRoutingService
import ru.lopon.data.routing.OrsRoutingService
import ru.lopon.data.routing.OsrmRoutingService
import ru.lopon.domain.routing.RoutingService

object IosRoutingServiceFactory {

    fun createHttpClient(): HttpClient = HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    fun createRoutingService(httpClient: HttpClient): RoutingService {
        val primary = OsrmRoutingService(httpClient = httpClient)
        val fallback = OrsRoutingService(httpClient = httpClient)
        return FallbackRoutingService(primary = primary, fallback = fallback)
    }
}
