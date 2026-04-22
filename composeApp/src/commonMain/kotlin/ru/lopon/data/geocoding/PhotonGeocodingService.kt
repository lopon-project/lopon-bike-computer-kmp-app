package ru.lopon.data.geocoding

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import ru.lopon.domain.model.GeoCoordinate

data class PhotonSearchResult(
    val name: String,
    val displayName: String,
    val coordinate: GeoCoordinate,
    val type: String, // city, street, house, tourism, amenity, etc.
    val osmKey: String,
    val osmValue: String
)

class PhotonGeocodingService(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://photon.komoot.io"
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun search(
        query: String,
        lat: Double? = null,
        lon: Double? = null,
        limit: Int = 7,
        lang: String = "ru"
    ): Result<List<PhotonSearchResult>> {
        if (query.length < 2) return Result.success(emptyList())

        return try {
            val response = httpClient.get("$baseUrl/api/") {
                parameter("q", query)
                parameter("lang", lang)
                parameter("limit", limit)
                if (lat != null && lon != null) {
                    parameter("lat", lat)
                    parameter("lon", lon)
                }
            }

            val body = response.bodyAsText()
            val parsed = json.parseToJsonElement(body).jsonObject
            val features = parsed["features"]?.jsonArray ?: JsonArray(emptyList())

            val results = features.mapNotNull { feature ->
                try {
                    parseFeature(feature.jsonObject)
                } catch (_: Exception) {
                    null
                }
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reverseGeocode(
        lat: Double,
        lon: Double
    ): Result<PhotonSearchResult?> {
        return try {
            val response = httpClient.get("$baseUrl/reverse") {
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("lang", "ru")
            }

            val body = response.bodyAsText()
            val parsed = json.parseToJsonElement(body).jsonObject
            val features = parsed["features"]?.jsonArray ?: JsonArray(emptyList())

            val result = features.firstOrNull()?.jsonObject?.let { parseFeature(it) }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseFeature(feature: JsonObject): PhotonSearchResult? {
        val geometry = feature["geometry"]?.jsonObject ?: return null
        val coordinates = geometry["coordinates"]?.jsonArray ?: return null
        if (coordinates.size < 2) return null

        val lon = coordinates[0].jsonPrimitive.double
        val lat = coordinates[1].jsonPrimitive.double

        val properties = feature["properties"]?.jsonObject ?: return null
        val name = properties["name"]?.jsonPrimitive?.content ?: ""
        val city = properties["city"]?.jsonPrimitive?.content
        val state = properties["state"]?.jsonPrimitive?.content
        val country = properties["country"]?.jsonPrimitive?.content
        val street = properties["street"]?.jsonPrimitive?.content
        val houseNumber = properties["housenumber"]?.jsonPrimitive?.content
        val osmKey = properties["osm_key"]?.jsonPrimitive?.content ?: ""
        val osmValue = properties["osm_value"]?.jsonPrimitive?.content ?: ""
        val type = properties["type"]?.jsonPrimitive?.content ?: osmValue

        val parts = mutableListOf<String>()
        if (name.isNotBlank()) parts.add(name)
        if (street != null) {
            val streetPart = if (houseNumber != null) "$street, $houseNumber" else street
            if (streetPart != name) parts.add(streetPart)
        }
        if (city != null && city != name) parts.add(city)
        if (state != null && state != city) parts.add(state)

        val displayName = if (parts.isNotEmpty()) parts.joinToString(", ")
        else country ?: "Unknown"

        return PhotonSearchResult(
            name = name.ifBlank { displayName },
            displayName = displayName,
            coordinate = GeoCoordinate(latitude = lat, longitude = lon),
            type = type,
            osmKey = osmKey,
            osmValue = osmValue
        )
    }
}
