import SwiftUI
import ComposeApp

struct HistoryView: View {
    @EnvironmentObject var holder: AppContainerHolder

    var body: some View {
        Group {
            if holder.history.state.isLoading {
                ProgressView("Загрузка...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if holder.history.state.trips.isEmpty {
                emptyState
            } else {
                List {
                    ForEach(holder.history.state.trips, id: \.id) { trip in
                        NavigationLink {
                            HistoryDetailView(trip: trip)
                        } label: {
                            HistoryRow(trip: trip)
                        }
                        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                            Button(role: .destructive) {
                                holder.history.requestDeleteTrip(trip.id)
                            } label: {
                                Label("Удалить", systemImage: "trash")
                            }
                        }
                    }
                }
                .listStyle(.insetGrouped)
            }
        }
        .alert(
            "Удалить поездку?",
            isPresented: Binding(
                get: { holder.history.state.pendingDeleteTripId != nil },
                set: { if !$0 { holder.history.dismissDeleteDialog() } }
            )
        ) {
            Button("Отмена", role: .cancel) { holder.history.dismissDeleteDialog() }
            Button("Удалить", role: .destructive) { holder.history.confirmDeleteTrip() }
        }
        .alert("Ошибка", isPresented: errorBinding) {
            Button("OK") { holder.history.clearError() }
        } message: {
            Text(holder.history.state.errorMessage ?? "")
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "clock.arrow.circlepath")
                .font(.system(size: 64))
                .foregroundStyle(.secondary)
            Text("Поездок пока нет")
                .font(.headline)
            Text("Начните первую поездку, чтобы здесь появилась история.")
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .padding(.horizontal)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var errorBinding: Binding<Bool> {
        Binding(
            get: { holder.history.state.errorMessage != nil },
            set: { if !$0 { holder.history.clearError() } }
        )
    }
}

private struct HistoryRow: View {
    let trip: Trip

    var body: some View {
        HStack {
            Image(systemName: modeIcon)
                .frame(width: 32, height: 32)
                .foregroundStyle(.accentColor)
            VStack(alignment: .leading, spacing: 2) {
                Text(modeLabel + " · " + dateString)
                    .font(.subheadline)
                Text(String(format: "%.2f км", trip.distanceMeters / 1000))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(.vertical, 4)
    }

    private var modeLabel: String {
        switch trip.mode {
        case is NavigationMode.Sensor: return "Sensor"
        case is NavigationMode.Hybrid: return "Hybrid"
        case is NavigationMode.Gps: return "GPS"
        default: return "—"
        }
    }
    private var modeIcon: String {
        switch trip.mode {
        case is NavigationMode.Sensor: return "speedometer"
        case is NavigationMode.Hybrid: return "antenna.radiowaves.left.and.right"
        default: return "location.fill"
        }
    }
    private var dateString: String {
        let date = Date(timeIntervalSince1970: TimeInterval(trip.startTimeUtc) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}
