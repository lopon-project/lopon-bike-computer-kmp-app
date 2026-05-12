import SwiftUI
import CoreLocation
import ComposeApp

struct HistoryDetailView: View {
    let trip: Trip
    @EnvironmentObject var holder: AppContainerHolder

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if !trackCoords.isEmpty {
                    MapLibreMapView(
                        initialCenter: trackCoords.first ?? CLLocationCoordinate2D(latitude: 55.7558, longitude: 37.6173),
                        initialZoom: 13,
                        recordedTrackPoints: trackCoords
                    )
                    .frame(height: 220)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                infoCard
                metricsGrid
                if !elevationPoints.isEmpty {
                    ElevationProfileChart(points: elevationPoints)
                        .padding(.vertical, 8)
                }
                LoponButton(title: "Экспорт в GPX", systemImage: "square.and.arrow.up") {
                    holder.trip.exportTripGpx(trip.id)
                }
            }
            .padding()
        }
        .navigationTitle("Детали поездки")
    }

    private var trackCoords: [CLLocationCoordinate2D] {
        trip.trackPoints.compactMap { tp in
            guard let lon = tp.longitude?.doubleValue else { return nil }
            return CLLocationCoordinate2D(latitude: tp.latitude, longitude: lon)
        }
    }

    private var elevationPoints: [ElevationPoint] {
        let pts = trip.trackPoints
        guard pts.count >= 2 else { return [] }
        var result: [ElevationPoint] = []
        var distanceM: Double = 0.0
        var prevLat: Double? = nil
        var prevLon: Double? = nil
        for tp in pts {
            guard let lon = tp.longitude?.doubleValue else { continue }
            if let pLat = prevLat, let pLon = prevLon {
                distanceM += haversine(prevLat: pLat, prevLon: pLon, lat: tp.latitude, lon: lon)
            }
            prevLat = tp.latitude
            prevLon = lon
            if let e = tp.elevation?.doubleValue {
                result.append(ElevationPoint(distanceKm: distanceM / 1000.0, elevationM: e))
            }
        }
        return result
    }

    private func haversine(prevLat: Double, prevLon: Double, lat: Double, lon: Double) -> Double {
        let r = 6371000.0
        let toRad = Double.pi / 180.0
        let dLat = (lat - prevLat) * toRad
        let dLon = (lon - prevLon) * toRad
        let a = sin(dLat / 2) * sin(dLat / 2) +
                cos(prevLat * toRad) * cos(lat * toRad) *
                sin(dLon / 2) * sin(dLon / 2)
        let c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private var infoCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            row("Режим", value: modeLabel)
            row("Старт", value: dateString(trip.startTimeUtc))
            if let end = trip.endTimeUtc {
                row("Финиш", value: dateString(end.int64Value))
            }
            if let routeId = trip.routeId, !routeId.isEmpty {
                row("Маршрут", value: routeId)
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private var metricsGrid: some View {
        let columns = [GridItem(.flexible()), GridItem(.flexible())]
        return LazyVGrid(columns: columns, spacing: 12) {
            MetricCard(title: "Дистанция",
                       value: String(format: "%.2f", trip.distanceMeters / 1000),
                       unit: "км",
                       systemImage: "ruler")
            MetricCard(title: "Время в движении",
                       value: durationString(trip.movingTimeMs),
                       unit: "",
                       systemImage: "timer")
            if let avg = trip.averageSpeedMs?.doubleValue {
                MetricCard(title: "Сред. скорость",
                           value: String(format: "%.1f", avg * 3.6),
                           unit: "км/ч",
                           systemImage: "speedometer")
            }
            if let max = trip.maxSpeedMs?.doubleValue {
                MetricCard(title: "Макс. скорость",
                           value: String(format: "%.1f", max * 3.6),
                           unit: "км/ч",
                           systemImage: "bolt")
            }
        }
    }

    private func row(_ key: String, value: String) -> some View {
        HStack {
            Text(key).foregroundStyle(.secondary)
            Spacer()
            Text(value).foregroundStyle(.primary)
        }
    }

    private var modeLabel: String {
        switch trip.mode {
        case is NavigationMode.Sensor: return "Sensor"
        case is NavigationMode.Hybrid: return "Hybrid"
        case is NavigationMode.Gps: return "GPS"
        default: return "—"
        }
    }

    private func dateString(_ ts: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(ts) / 1000.0)
        let f = DateFormatter()
        f.dateStyle = .short
        f.timeStyle = .short
        return f.string(from: date)
    }

    private func durationString(_ ms: Int64) -> String {
        let total = Int(ms / 1000)
        let h = total / 3600
        let m = (total % 3600) / 60
        let s = total % 60
        if h > 0 {
            return String(format: "%d:%02d:%02d", h, m, s)
        }
        return String(format: "%d:%02d", m, s)
    }
}
