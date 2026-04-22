package ru.lopon.data.routing

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.routing.*

class OrsRoutingService(
    private val httpClient: HttpClient,
    private val apiKey: String = DEFAULT_API_KEY
) : RoutingService {

    companion object {
        private const val BASE_URL = "https://api.openrouteservice.org/v2/directions"
        const val DEFAULT_API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImEwNDQzNTZlN2U4YjQ1YzZiZTRhNzgyNDM5NTI4YjM1IiwiaCI6Im11cm11cjY0In0="
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
            val orsProfile = mapProfile(profile)
            val url = "$BASE_URL/$orsProfile/geojson"

            val requestBody = buildJsonObject {
                put("coordinates", JsonArray(waypoints.map { coord ->
                    JsonArray(listOf(
                        JsonPrimitive(coord.longitude),
                        JsonPrimitive(coord.latitude)
                    ))
                }))
                put("instructions", JsonPrimitive(true))
            }

            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                header("Authorization", apiKey)
                setBody(requestBody.toString())
            }

            val responseText = response.bodyAsText()
            val responseJson = json.parseToJsonElement(responseText).jsonObject

            val error = responseJson["error"]
            if (error != null) {
                val errorMessage = error.jsonObject["message"]?.jsonPrimitive?.content
                    ?: "Ошибка маршрутизации"
                return Result.failure(RoutingException.RouteNotFound(errorMessage))
            }

            val result = parseGeoJsonResponse(responseJson)
            Result.success(result)
        } catch (e: RoutingException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(
                RoutingException.NetworkError("Ошибка сети: ${e.message ?: "неизвестная ошибка"}")
            )
        }
    }

    override suspend fun isAvailable(): Boolean {
        return true
    }

    override fun getServiceType(): RoutingServiceType = RoutingServiceType.ONLINE

    private fun mapProfile(profile: RoutingProfile): String = when (profile) {
        RoutingProfile.BIKE, RoutingProfile.ROAD_BIKE -> "cycling-road"
        RoutingProfile.MTB -> "cycling-mountain"
        RoutingProfile.CITY_BIKE -> "cycling-regular"
        RoutingProfile.FOOT_WALKING -> "foot-walking"
    }

    private fun parseGeoJsonResponse(responseJson: JsonObject): RoutingResult {
        val features = responseJson["features"]?.jsonArray
            ?: throw RoutingException.RouteNotFound("Маршрут не найден: нет данных")

        if (features.isEmpty()) {
            throw RoutingException.RouteNotFound("Маршрут не найден")
        }

        val feature = features[0].jsonObject
        val geometry = feature["geometry"]?.jsonObject
        val properties = feature["properties"]?.jsonObject

        val coordinates = geometry?.get("coordinates")?.jsonArray
            ?: throw RoutingException.RouteNotFound("Нет координат маршрута")

        val points = coordinates.map { coordArray ->
            val coords = coordArray.jsonArray
            GeoCoordinate(
                latitude = coords[1].jsonPrimitive.double,
                longitude = coords[0].jsonPrimitive.double
            )
        }

        val summary = properties?.get("summary")?.jsonObject
        val distanceMeters = summary?.get("distance")?.jsonPrimitive?.double ?: 0.0
        val durationSeconds = summary?.get("duration")?.jsonPrimitive?.double?.toLong() ?: 0L

        val ascent = properties?.get("ascent")?.jsonPrimitive?.double
        val descent = properties?.get("descent")?.jsonPrimitive?.double

        val instructions = parseInstructions(properties)

        return RoutingResult(
            points = points,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            elevationGainMeters = ascent,
            elevationLossMeters = descent,
            instructions = instructions,
            source = RoutingServiceType.ONLINE
        )
    }

    private fun parseInstructions(properties: JsonObject?): List<RoutingInstruction> {
        val segments = properties?.get("segments")?.jsonArray ?: return emptyList()
        val instructions = mutableListOf<RoutingInstruction>()

        for (segment in segments) {
            val steps = segment.jsonObject["steps"]?.jsonArray ?: continue
            for (step in steps) {
                val stepObj = step.jsonObject
                val type = mapInstructionType(stepObj["type"]?.jsonPrimitive?.int ?: 0)
                val instruction = stepObj["instruction"]?.jsonPrimitive?.content ?: ""
                val distance = stepObj["distance"]?.jsonPrimitive?.double ?: 0.0
                val wayPoints = stepObj["way_points"]?.jsonArray
                val pointIndex = wayPoints?.firstOrNull()?.jsonPrimitive?.int ?: 0
                val name = stepObj["name"]?.jsonPrimitive?.content

                instructions.add(
                    RoutingInstruction(
                        type = type,
                        text = instruction,
                        distanceMeters = distance,
                        pointIndex = pointIndex,
                        streetName = name?.takeIf { it.isNotBlank() && it != "-" }
                    )
                )
            }
        }

        return instructions
    }

    private fun mapInstructionType(orsType: Int): InstructionType = when (orsType) {
        0 -> InstructionType.TURN_LEFT
        1 -> InstructionType.TURN_RIGHT
        2 -> InstructionType.TURN_SHARP_LEFT
        3 -> InstructionType.TURN_SHARP_RIGHT
        4 -> InstructionType.TURN_SLIGHT_LEFT
        5 -> InstructionType.TURN_SLIGHT_RIGHT
        6 -> InstructionType.CONTINUE
        7 -> InstructionType.ROUNDABOUT
        8 -> InstructionType.ROUNDABOUT
        9 -> InstructionType.U_TURN
        10 -> InstructionType.DESTINATION
        11 -> InstructionType.WAYPOINT
        12 -> InstructionType.KEEP_LEFT
        13 -> InstructionType.KEEP_RIGHT
        else -> InstructionType.UNKNOWN
    }
}
