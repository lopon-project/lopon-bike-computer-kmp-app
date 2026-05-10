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
                        Spacer()
                        if let gain = details.elevationGainMeters?.doubleValue, gain > 0 {
                            Label(String(format: "↑ %.0f м", gain),
                                  systemImage: "arrow.up.right")
                        }
                    }
                    .font(.caption)
                    .foregroundStyle(.secondary)

                    if let elev = synthElevation(for: holder.routeWizard.state.calculatedRoute,
                                                 totalGain: details.elevationGainMeters?.doubleValue ?? 0,
                                                 totalLoss: details.elevationLossMeters?.doubleValue ?? 0),
                       elev.count >= 2 {
                        ElevationProfileChart(points: elev, height: 100)
                    }
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

    private func synthElevation(for route: Route?, totalGain: Double, totalLoss: Double) -> [ElevationPoint]? {
        guard let route = route, route.points.count >= 2 else { return nil }
        guard totalGain > 0 || totalLoss > 0 else { return nil }
        let coords = route.points.map {
            CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude)
        }
        var distancesKm: [Double] = [0.0]
        var total = 0.0
        for i in 1..<coords.count {
            total += haversine(prevLat: coords[i - 1].latitude, prevLon: coords[i - 1].longitude,
                               lat: coords[i].latitude, lon: coords[i].longitude) / 1000.0
            distancesKm.append(total)
        }
        let baseElevation: Double = 100
        let n = coords.count
        let half = max(1, n / 2)
        var elev: [Double] = []
        for i in 0..<n {
            let v: Double
            if i <= half {
                v = baseElevation + (totalGain * Double(i)) / Double(half)
            } else {
                v = baseElevation + totalGain - (totalLoss * Double(i - half)) / Double(n - half)
            }
            elev.append(v)
        }
        return zip(distancesKm, elev).map { ElevationPoint(distanceKm: $0.0, elevationM: $0.1) }
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
}
