package ru.lopon.core

import ru.lopon.domain.model.GeoCoordinate

object GeoCoordinateParser {

    fun parseCoordinatePair(
        startLat: String,
        startLon: String,
        endLat: String,
        endLon: String
    ): Pair<GeoCoordinate, GeoCoordinate>? {
        val sLat = startLat.trim().toDoubleOrNull() ?: return null
        val sLon = startLon.trim().toDoubleOrNull() ?: return null
        val eLat = endLat.trim().toDoubleOrNull() ?: return null
        val eLon = endLon.trim().toDoubleOrNull() ?: return null

        if (sLat !in -90.0..90.0 || eLat !in -90.0..90.0) return null
        if (sLon !in -180.0..180.0 || eLon !in -180.0..180.0) return null

        return GeoCoordinate(sLat, sLon) to GeoCoordinate(eLat, eLon)
    }
}
