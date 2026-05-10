import SwiftUI
import ComposeApp

struct RouteDetailView: View {
    let route: Route
    @EnvironmentObject var holder: AppContainerHolder
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                RoutePreviewMiniMap(points: route.points, height: 220)
                infoCard
                actions
            }
            .padding()
        }
        .navigationTitle(route.name)
    }

    private var infoCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            row("Дистанция", value: String(format: "%.2f км", route.distanceMeters / 1000))
            row("Количество точек", value: "\(route.pointCount)")
            if let start = route.startPoint, let end = route.endPoint {
                row("Старт", value: String(format: "%.4f, %.4f", start.latitude, start.longitude))
                row("Финиш", value: String(format: "%.4f, %.4f", end.latitude, end.longitude))
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private var actions: some View {
        VStack(spacing: 12) {
            LoponButton(title: "Использовать для старта",
                        systemImage: "play.fill") {
                holder.trip.openStartWizardWithRoute(route.id)
                dismiss()
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
}
