package ru.lopon.data.routing

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.routing.*

class OsrmRoutingService(
    private val httpClient: HttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL
) : RoutingService {

    companion object {
        const val DEFAULT_BASE_URL = "https://router.project-osrm.org"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun calculateRoute(
        waypoints: List<GeoCoordinate>,
        profile: RoutingProfile
    ): Result<RoutingResult> {
        if (waypoints.size < 2) {
            return Result.failure(
                RoutingException.RouteNotFound("Для построения маршрута нужно минимум 2 точки")
            )
        }

        return try {
            val osrmProfile = mapProfile(profile)

            val coordinates = waypoints.joinToString(";") { "${it.longitude},${it.latitude}" }
            val url = "$baseUrl/route/v1/$osrmProfile/$coordinates"

            val response = httpClient.get(url) {
                parameter("overview", "full")
                parameter("geometries", "geojson")
                parameter("steps", "true")
                parameter("annotations", "true")
            }

            val responseText = response.bodyAsText()
            val responseJson = json.parseToJsonElement(responseText).jsonObject

            val code = responseJson["code"]?.jsonPrimitive?.content
            if (code != "Ok") {
                val message = responseJson["message"]?.jsonPrimitive?.content
                    ?: "Маршрут не найден"
                return Result.failure(RoutingException.RouteNotFound(message))
            }

            val result = parseOsrmResponse(responseJson)
            Result.success(result)
        } catch (e: RoutingException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(
                RoutingException.NetworkError("Ошибка сети: ${e.message ?: "неизвестная ошибка"}")
            )
        }
    }

    override suspend fun isAvailable(): Boolean = true

    override fun getServiceType(): RoutingServiceType = RoutingServiceType.ONLINE

    private fun mapProfile(profile: RoutingProfile): String = when (profile) {
        RoutingProfile.BIKE -> "bicycle"
        RoutingProfile.ROAD_BIKE -> "bicycle"
        RoutingProfile.MTB -> "bicycle"
        RoutingProfile.CITY_BIKE -> "bicycle"
        RoutingProfile.FOOT_WALKING -> "foot"
    }

    private fun parseOsrmResponse(responseJson: JsonObject): RoutingResult {
        val routes = responseJson["routes"]?.jsonArray
            ?: throw RoutingException.RouteNotFound("Маршрут не найден")

        if (routes.isEmpty()) {
            throw RoutingException.RouteNotFound("Маршрут не найден")
        }

        val route = routes[0].jsonObject

        val geometry = route["geometry"]?.jsonObject
        val coordinates = geometry?.get("coordinates")?.jsonArray
            ?: throw RoutingException.RouteNotFound("Нет координат маршрута")

        val points = coordinates.map { coordArray ->
            val coords = coordArray.jsonArray
            GeoCoordinate(
                latitude = coords[1].jsonPrimitive.double,
                longitude = coords[0].jsonPrimitive.double
            )
        }


        val distanceMeters = route["distance"]?.jsonPrimitive?.double ?: 0.0
        val durationSeconds = route["duration"]?.jsonPrimitive?.double?.toLong() ?: 0L

        val instructions = parseInstructions(route)

        return RoutingResult(
            points = points,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            elevationGainMeters = null,
            elevationLossMeters = null,
            instructions = instructions,
            source = RoutingServiceType.ONLINE
        )
    }

    private fun parseInstructions(route: JsonObject): List<RoutingInstruction> {
        val legs = route["legs"]?.jsonArray ?: return emptyList()
        val instructions = mutableListOf<RoutingInstruction>()
        var globalPointIndex = 0

        for (leg in legs) {
            val steps = leg.jsonObject["steps"]?.jsonArray ?: continue
            for (step in steps) {
                val stepObj = step.jsonObject
                val maneuver = stepObj["maneuver"]?.jsonObject ?: continue

                val maneuverType = maneuver["type"]?.jsonPrimitive?.content ?: "unknown"
                val modifier = maneuver["modifier"]?.jsonPrimitive?.content
                val name = stepObj["name"]?.jsonPrimitive?.content
                val distance = stepObj["distance"]?.jsonPrimitive?.double ?: 0.0

                val type = mapManeuverType(maneuverType, modifier)

                val text = buildInstructionText(maneuverType, modifier, name)

                instructions.add(
                    RoutingInstruction(
                        type = type,
                        text = text,
                        distanceMeters = distance,
                        pointIndex = globalPointIndex,
                        streetName = name?.takeIf { it.isNotBlank() }
                    )
                )

                val stepGeometry = stepObj["geometry"]?.jsonObject
                val stepCoords = stepGeometry?.get("coordinates")?.jsonArray
                globalPointIndex += (stepCoords?.size ?: 1) - 1
            }
        }

        return instructions
    }

    private fun mapManeuverType(type: String, modifier: String?): InstructionType {
        return when (type) {
            "depart" -> InstructionType.WAYPOINT
            "arrive" -> InstructionType.DESTINATION
            "turn" -> when (modifier) {
                "left" -> InstructionType.TURN_LEFT
                "right" -> InstructionType.TURN_RIGHT
                "sharp left" -> InstructionType.TURN_SHARP_LEFT
                "sharp right" -> InstructionType.TURN_SHARP_RIGHT
                "slight left" -> InstructionType.TURN_SLIGHT_LEFT
                "slight right" -> InstructionType.TURN_SLIGHT_RIGHT
                "uturn" -> InstructionType.U_TURN
                else -> InstructionType.CONTINUE
            }
            "new name", "continue" -> InstructionType.CONTINUE
            "merge" -> when (modifier) {
                "left", "slight left" -> InstructionType.KEEP_LEFT
                "right", "slight right" -> InstructionType.KEEP_RIGHT
                else -> InstructionType.CONTINUE
            }
            "fork" -> when (modifier) {
                "left", "slight left" -> InstructionType.KEEP_LEFT
                "right", "slight right" -> InstructionType.KEEP_RIGHT
                else -> InstructionType.CONTINUE
            }
            "roundabout", "rotary" -> InstructionType.ROUNDABOUT
            "end of road" -> when (modifier) {
                "left" -> InstructionType.TURN_LEFT
                "right" -> InstructionType.TURN_RIGHT
                else -> InstructionType.CONTINUE
            }
            else -> InstructionType.UNKNOWN
        }
    }

    private fun buildInstructionText(type: String, modifier: String?, streetName: String?): String {
        val directionRu = when (modifier) {
            "left" -> "налево"
            "right" -> "направо"
            "sharp left" -> "резко налево"
            "sharp right" -> "резко направо"
            "slight left" -> "плавно налево"
            "slight right" -> "плавно направо"
            "straight" -> "прямо"
            "uturn" -> "разворот"
            else -> ""
        }

        val street = streetName?.takeIf { it.isNotBlank() }?.let { " на $it" } ?: ""

        return when (type) {
            "depart" -> "Старт$street"
            "arrive" -> "Прибытие$street"
            "turn" -> "Поверните $directionRu$street"
            "new name" -> "Продолжайте$street"
            "continue" -> "Продолжайте $directionRu$street"
            "merge" -> "Вливайтесь $directionRu$street"
            "fork" -> "Держитесь $directionRu$street"
            "roundabout", "rotary" -> "Круговое движение$street"
            "end of road" -> "Поверните $directionRu$street"
            else -> "Продолжайте движение$street"
        }
    }
}
