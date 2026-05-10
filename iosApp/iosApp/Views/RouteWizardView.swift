import SwiftUI
import ComposeApp

struct RouteWizardView: View {
    @EnvironmentObject var holder: AppContainerHolder
    @Environment(\.dismiss) private var dismiss
    @State private var selectedTab: Int = 0

    var body: some View {
        VStack(spacing: 0) {
            Picker("", selection: $selectedTab) {
                Text("Точки").tag(0)
                Text("Импорт GPX").tag(1)
            }
            .pickerStyle(.segmented)
            .padding()
            .onChange(of: selectedTab) { _, new in
                holder.routeWizard.setActiveTab(new == 0 ? .points : .gpxImport)
            }

            if selectedTab == 0 {
                pointsTab
            } else {
                gpxImportTab
            }
        }
        .navigationTitle("Новый маршрут")
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("Отмена") { dismiss() }
            }
        }
        .alert("Ошибка", isPresented: errorBinding) {
            Button("OK") { holder.routeWizard.clearError() }
        } message: {
            Text(holder.routeWizard.state.errorMessage ?? "")
        }
        .onChange(of: holder.routeWizard.state.savedSuccessfully) { _, saved in
            if saved { dismiss() }
        }
    }

    private var pointsTab: some View {
        VStack(spacing: 12) {
            TextField(
                "Поиск адреса или места...",
                text: Binding(
                    get: { holder.routeWizard.state.searchQuery },
                    set: { holder.routeWizard.updateSearchQuery($0) }
                )
            )
            .textFieldStyle(.roundedBorder)
            .padding(.horizontal)

            if holder.routeWizard.state.showSearchResults {
                searchResultsList
            }

            ZStack(alignment: .topTrailing) {
                MapLibreMapView(
                    routePoints: routePointsCoords,
                    waypointMarkers: waypointMarkersCoords,
                    onTap: { coord in
                        holder.routeWizard.addWaypoint(
                            latitude: coord.latitude,
                            longitude: coord.longitude
                        )
                    }
                )
                .ignoresSafeArea(edges: .bottom)

                if holder.routeWizard.state.isCalculating {
                    ProgressView()
                        .padding(8)
                        .background(.ultraThinMaterial, in: Circle())
                        .padding(8)
                }
            }

            VStack(spacing: 8) {
                TextField(
                    "Название маршрута",
                    text: Binding(
                        get: { holder.routeWizard.state.routeName },
                        set: { holder.routeWizard.updateRouteName($0) }
                    )
                )
                .textFieldStyle(.roundedBorder)

                if let details = holder.routeWizard.state.routeDetails {
                    HStack {
                        Label(String(format: "%.2f км", details.distanceMeters / 1000),
                              systemImage: "ruler")
                        Spacer()
                        Label(formatSeconds(details.durationSeconds),
                              systemImage: "clock")
                    }
                    .font(.caption)
                    .foregroundStyle(.secondary)
                }

                HStack(spacing: 8) {
                    Button(role: .destructive) {
                        holder.routeWizard.removeLastWaypoint()
                    } label: {
                        Label("Удалить точку", systemImage: "minus")
                    }
                    .disabled(holder.routeWizard.state.waypoints.isEmpty)

                    Button {
                        holder.routeWizard.calculateRoute()
                    } label: {
                        Label("Построить", systemImage: "arrow.triangle.turn.up.right.diamond")
                    }
                    .disabled(holder.routeWizard.state.waypoints.count < 2)
                }
                .buttonStyle(.bordered)

                LoponButton(
                    title: "Сохранить маршрут",
                    systemImage: "square.and.arrow.down",
                    isLoading: holder.routeWizard.state.isSaving
                ) {
                    holder.routeWizard.saveRoute()
                }
                .disabled(holder.routeWizard.state.calculatedRoute == nil)
            }
            .padding(.horizontal)
            .padding(.bottom)
        }
    }

    private var searchResultsList: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(holder.routeWizard.state.searchResults, id: \.displayName) { result in
                    Button {
                        holder.routeWizard.selectSearchResult(result)
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(result.name)
                                .lineLimit(1)
                            Text(result.displayName)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .lineLimit(2)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 8)
                        .padding(.horizontal)
                    }
                    .buttonStyle(.plain)
                    Divider()
                }
            }
        }
        .frame(maxHeight: 220)
        .background(.regularMaterial)
    }

    private var gpxImportTab: some View {
        VStack(spacing: 16) {
            Image(systemName: "square.and.arrow.down.on.square")
                .font(.system(size: 64))
                .foregroundStyle(.accentColor)
            Text("Импорт маршрута из GPX-файла")
                .font(.headline)
            Text("Выберите файл формата GPX 1.1 — он будет добавлен в список маршрутов.")
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .padding(.horizontal)

            LoponButton(
                title: "Выбрать файл",
                systemImage: "doc.badge.plus",
                isLoading: holder.routeWizard.state.isCalculating
            ) {
                holder.routeWizard.pickAndImportGpx()
            }
            .padding(.horizontal)

            Spacer()
        }
        .padding(.top, 32)
    }

    private var routePointsCoords: [CLLocationCoordinate2D] {
        guard let route = holder.routeWizard.state.calculatedRoute
            ?? holder.routeWizard.state.importedRoute else { return [] }
        return route.points.map {
            CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude)
        }
    }

    private var waypointMarkersCoords: [CLLocationCoordinate2D] {
        holder.routeWizard.state.waypoints.map {
            CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude)
        }
    }

    private var errorBinding: Binding<Bool> {
        Binding(
            get: { holder.routeWizard.state.errorMessage != nil },
            set: { if !$0 { holder.routeWizard.clearError() } }
        )
    }

    private func formatSeconds(_ s: Int64) -> String {
        let total = Int(s)
        let h = total / 3600
        let m = (total % 3600) / 60
        if h > 0 { return "\(h) ч \(m) мин" }
        return "\(m) мин"
    }
}
