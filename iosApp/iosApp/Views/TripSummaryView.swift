import SwiftUI
import ComposeApp

struct TripSummaryView: View {
    @EnvironmentObject var holder: AppContainerHolder

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    headerCard
                    metricsGrid
                    if !elevationPoints.isEmpty {
                        ElevationProfileChart(points: elevationPoints)
                            .padding(.vertical, 4)
                    }
                    LoponButton(title: "Экспорт в GPX", systemImage: "square.and.arrow.up") {
                        holder.trip.exportSummaryGpx()
                    }
                }
                .padding()
            }
            .navigationTitle("Итоги поездки")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Готово") { holder.trip.dismissTripSummary() }
                }
            }
        }
    }

    private var elevationPoints: [ElevationPoint] {
        let pts = holder.trip.state.recordedTrackPointsFull
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

    private var headerCard: some View {
        VStack(spacing: 6) {
            Image(systemName: "checkmark.seal.fill")
                .font(.system(size: 56))
                .foregroundStyle(.green)
            Text("Поездка завершена")
                .font(.title3.bold())
            Text("Режим: \(holder.trip.state.summaryModeName)")
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private var metricsGrid: some View {
        let metrics = holder.trip.state.summaryMetrics
        let cols = [GridItem(.flexible()), GridItem(.flexible())]
        return LazyVGrid(columns: cols, spacing: 12) {
            MetricCard(title: "Дистанция",
                       value: String(format: "%.2f", (metrics?.totalDistanceKm ?? 0)),
                       unit: "км",
                       systemImage: "ruler")
            MetricCard(title: "Время",
                       value: formatDuration(metrics?.elapsedTimeMs ?? 0),
                       unit: "",
                       systemImage: "clock")
            MetricCard(title: "Сред. скорость",
                       value: String(format: "%.1f", metrics?.averageSpeedKmh ?? 0),
                       unit: "км/ч",
                       systemImage: "speedometer")
            MetricCard(title: "Макс. скорость",
                       value: String(format: "%.1f", metrics?.maxSpeedKmh ?? 0),
                       unit: "км/ч",
                       systemImage: "bolt")
        }
    }

    private func formatDuration(_ ms: Int64) -> String {
        let total = Int(ms / 1000)
        let h = total / 3600
        let m = (total % 3600) / 60
        let s = total % 60
        if h > 0 { return String(format: "%d:%02d:%02d", h, m, s) }
        return String(format: "%d:%02d", m, s)
    }
}
