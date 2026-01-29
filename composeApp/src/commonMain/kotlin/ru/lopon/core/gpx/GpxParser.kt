package ru.lopon.core.gpx

import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.model.Route


class GpxParser {

    fun parse(gpxContent: String): Result<List<Route>> {
        return try {
            val routes = mutableListOf<Route>()
            val normalizedContent = gpxContent.replace("\n", " ").replace("\r", " ")
            val tracks = extractTags(normalizedContent, "trk")

            for ((index, trackContent) in tracks.withIndex()) {
                val name = extractTagContent(trackContent, "name") ?: "Track ${index + 1}"
                val points = mutableListOf<GeoCoordinate>()
                val segments = extractTags(trackContent, "trkseg")

                for (segmentContent in segments) {
                    points.addAll(parseTrackPoints(segmentContent))
                }

                if (points.isNotEmpty()) {
                    routes.add(Route(
                        id = "gpx-track-$index",
                        name = name.trim(),
                        points = points
                    ))
                }
            }

            val rtes = extractTags(normalizedContent, "rte")

            for ((index, routeContent) in rtes.withIndex()) {
                val name = extractTagContent(routeContent, "name") ?: "Route ${index + 1}"
                val points = parseRoutePoints(routeContent)

                if (points.isNotEmpty()) {
                    routes.add(Route(
                        id = "gpx-route-$index",
                        name = name.trim(),
                        points = points
                    ))
                }
            }

            if (routes.isEmpty()) {
                Result.failure(GpxParseException("No tracks or routes found in GPX"))
            } else {
                Result.success(routes)
            }
        } catch (e: Exception) {
            Result.failure(GpxParseException("Failed to parse GPX: ${e.message}"))
        }
    }

    fun parseFirst(gpxContent: String): Result<Route> {
        return parse(gpxContent).mapCatching { routes ->
            routes.firstOrNull()
                ?: throw GpxParseException("No tracks found in GPX")
        }
    }

    private fun extractTags(content: String, tagName: String): List<String> {
        val results = mutableListOf<String>()
        val openTag = "<$tagName>"
        val openTagAlt = "<$tagName "
        val closeTag = "</$tagName>"

        var startIndex = 0
        while (true) {
            var tagStart = content.indexOf(openTag, startIndex)
            if (tagStart == -1) {
                tagStart = content.indexOf(openTagAlt, startIndex)
            }
            if (tagStart == -1) break

            val contentStart = content.indexOf('>', tagStart) + 1
            if (contentStart == 0) break

            val tagEnd = content.indexOf(closeTag, contentStart)
            if (tagEnd == -1) break

            results.add(content.substring(contentStart, tagEnd))
            startIndex = tagEnd + closeTag.length
        }

        return results
    }


    private fun extractTagContent(content: String, tagName: String): String? {
        val openTag = "<$tagName>"
        val closeTag = "</$tagName>"

        val start = content.indexOf(openTag)
        if (start == -1) return null

        val contentStart = start + openTag.length
        val end = content.indexOf(closeTag, contentStart)
        if (end == -1) return null

        return content.substring(contentStart, end)
    }


    private fun parseTrackPoints(segmentContent: String): List<GeoCoordinate> {
        return parsePoints(segmentContent, "trkpt")
    }


    private fun parseRoutePoints(routeContent: String): List<GeoCoordinate> {
        return parsePoints(routeContent, "rtept")
    }


    private fun parsePoints(content: String, tagName: String): List<GeoCoordinate> {
        val points = mutableListOf<GeoCoordinate>()
        val openTag = "<$tagName"

        var startIndex = 0
        while (true) {
            val tagStart = content.indexOf(openTag, startIndex)
            if (tagStart == -1) break

            val tagEnd = content.indexOf('>', tagStart)
            if (tagEnd == -1) break

            val tagContent = content.substring(tagStart, tagEnd + 1)

            val lat = extractAttribute(tagContent, "lat")?.toDoubleOrNull()
            val lon = extractAttribute(tagContent, "lon")?.toDoubleOrNull()

            if (lat != null && lon != null) {
                points.add(GeoCoordinate(lat, lon))
            }

            startIndex = tagEnd + 1
        }

        return points
    }


    private fun extractAttribute(tagContent: String, attributeName: String): String? {
        val patterns = listOf(
            "$attributeName=\"",
            "$attributeName='"
        )

        for (pattern in patterns) {
            val start = tagContent.indexOf(pattern)
            if (start != -1) {
                val valueStart = start + pattern.length
                val quote = pattern.last()
                val valueEnd = tagContent.indexOf(quote, valueStart)
                if (valueEnd != -1) {
                    return tagContent.substring(valueStart, valueEnd)
                }
            }
        }

        return null
    }
}

class GpxParseException(message: String) : Exception(message)
