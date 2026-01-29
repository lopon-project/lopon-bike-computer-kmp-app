package ru.lopon.core.gpx

import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.model.Route
import ru.lopon.domain.model.TrackPoint
import ru.lopon.domain.model.Trip


class GpxSerializer {

    companion object {
        private const val GPX_HEADER = """<?xml version="1.0" encoding="UTF-8"?>"""
        private const val GPX_NAMESPACE = """xmlns="http://www.topografix.com/GPX/1/1""""
        private const val CREATOR = "Lopon Bike Navigator"
    }

    fun serializeRoute(route: Route): String {
        return buildSimpleGpx(route.name) { sb ->
            route.points.forEach { point ->
                sb.appendLine("""      <trkpt lat="${point.latitude}" lon="${point.longitude}"/>""")
            }
        }
    }

    fun serializeTrip(
        trip: Trip,
        trackPoints: List<TrackPoint>,
        name: String = "Trip ${trip.id}"
    ): String {
        return buildString {
            appendLine(GPX_HEADER)
            appendLine("""<gpx version="1.1" creator="$CREATOR" $GPX_NAMESPACE>""")
            appendLine("  <metadata>")
            appendLine("    <name>${escapeXml(name)}</name>")
            appendLine("    <time>${formatIsoTime(trip.startTimeUtc)}</time>")
            appendLine("  </metadata>")
            appendLine("  <trk>")
            appendLine("    <name>${escapeXml(name)}</name>")
            appendLine("    <type>cycling</type>")
            appendLine("    <trkseg>")
            trackPoints.forEach { point -> appendTrackPoint(this, point) }
            appendLine("    </trkseg>")
            appendLine("  </trk>")
            appendLine("</gpx>")
        }
    }

    fun serializeCoordinates(
        points: List<GeoCoordinate>,
        name: String = "Route"
    ): String {
        return buildSimpleGpx(name) { sb ->
            points.forEach { point ->
                sb.appendLine("""      <trkpt lat="${point.latitude}" lon="${point.longitude}"/>""")
            }
        }
    }

    private fun buildSimpleGpx(name: String, appendPoints: (StringBuilder) -> Unit): String {
        return buildString {
            appendLine(GPX_HEADER)
            appendLine("""<gpx version="1.1" creator="$CREATOR" $GPX_NAMESPACE>""")
            appendLine("  <trk>")
            appendLine("    <name>${escapeXml(name)}</name>")
            appendLine("    <trkseg>")
            appendPoints(this)
            appendLine("    </trkseg>")
            appendLine("  </trk>")
            appendLine("</gpx>")
        }
    }

    private fun appendTrackPoint(sb: StringBuilder, point: TrackPoint) {
        sb.append("      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">")

        point.elevation?.let { ele ->
            sb.append("<ele>$ele</ele>")
        }

        point.timestampUtc?.let { time ->
            sb.append("<time>${formatIsoTime(time)}</time>")
        }

        point.speed?.let { spd ->
            sb.append("<extensions><speed>$spd</speed></extensions>")
        }

        sb.appendLine("</trkpt>")
    }

    private fun formatIsoTime(timestampUtc: Long): String {
        val seconds = timestampUtc / 1000
        val millis = timestampUtc % 1000

        val days = seconds / 86400
        val timeOfDay = seconds % 86400

        val hours = (timeOfDay / 3600).toInt()
        val minutes = ((timeOfDay % 3600) / 60).toInt()
        val secs = (timeOfDay % 60).toInt()

        var remainingDays = days.toInt()
        var year = 1970

        while (true) {
            val daysInYear = if (isLeapYear(year)) 366 else 365
            if (remainingDays < daysInYear) break
            remainingDays -= daysInYear
            year++
        }

        val monthDays = if (isLeapYear(year)) {
            intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        } else {
            intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        }

        var month = 1
        for (i in monthDays.indices) {
            if (remainingDays < monthDays[i]) break
            remainingDays -= monthDays[i]
            month++
        }

        val day = remainingDays + 1

        return buildString {
            append(year.toString().padStart(4, '0'))
            append('-')
            append(month.toString().padStart(2, '0'))
            append('-')
            append(day.toString().padStart(2, '0'))
            append('T')
            append(hours.toString().padStart(2, '0'))
            append(':')
            append(minutes.toString().padStart(2, '0'))
            append(':')
            append(secs.toString().padStart(2, '0'))
            append('.')
            append(millis.toInt().toString().padStart(3, '0'))
            append('Z')
        }
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
