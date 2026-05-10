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
                    actions
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

    private var actions: some View {
        VStack(spacing: 8) {
            Text("Поездка сохранена в истории. Откройте её там, чтобы экспортировать GPX.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
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
