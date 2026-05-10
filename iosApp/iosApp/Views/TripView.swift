import SwiftUI
import ComposeApp

struct TripView: View {
    @EnvironmentObject var holder: AppContainerHolder
    @State private var showSensorSheet = false
    @State private var showStartWizard = false
    @State private var hideMetrics = false

    var body: some View {
        ZStack {
            mapLayer
                .ignoresSafeArea()

            VStack {
                topStatuses
                Spacer()
                if !hideMetrics {
                    metricsPanel
                }
                controlsPanel
            }
            .padding()
        }
        .sheet(isPresented: $showSensorSheet) { SensorView() }
        .sheet(isPresented: $showStartWizard) {
            NavigationStack { StartTripWizardView() }
        }
        .sheet(isPresented: tripSummaryBinding) {
            TripSummaryView()
        }
        .onChange(of: holder.trip.state.isWizardOpen) { _, open in
            showStartWizard = open
        }
        .onChange(of: showStartWizard) { _, open in
            if !open {
                holder.trip.closeStartWizard()
            }
        }
        .onChange(of: holder.trip.state.isRecording) { _, recording in
            updateIdleTimer(recording: recording)
        }
        .onAppear {
            updateIdleTimer(recording: holder.trip.state.isRecording)
        }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = false
        }
    }

    private func updateIdleTimer(recording: Bool) {
        let keepOn = holder.settings.state.settings.keepScreenOn
        UIApplication.shared.isIdleTimerDisabled = recording && keepOn
    }

    private var mapLayer: some View {
        let user = holder.trip.state.lastKnownLocation.flatMap {
            CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude)
        }
        let routeCoords: [CLLocationCoordinate2D] = {
            guard let routeId = holder.trip.state.selectedRouteId,
                  let route = holder.trip.state.availableRoutes.first(where: { $0.id == routeId }) else {
                return []
            }
            return route.points.map { CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude) }
        }()
        let trackCoords = holder.trip.state.recordedTrackPoints.map {
            CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude)
        }
        return MapLibreMapView(
            initialCenter: user ?? .init(latitude: 55.7558, longitude: 37.6173),
            initialZoom: 14,
            userLocation: user,
            followUser: holder.trip.state.isRecording,
            routePoints: routeCoords,
            recordedTrackPoints: trackCoords
        )
    }

    private var topStatuses: some View {
        HStack {
            StatusIndicators(
                bleConnected: holder.trip.state.isConnected,
                bleConnectionState: holder.trip.state.connectionState,
                gpsStatus: holder.container.locationProvider.status.value,
                mode: holder.trip.state.selectedMode
            )
            Spacer()
            Button {
                showSensorSheet = true
            } label: {
                Image(systemName: "antenna.radiowaves.left.and.right")
                    .padding(8)
                    .background(.regularMaterial, in: Circle())
            }
            Button {
                hideMetrics.toggle()
            } label: {
                Image(systemName: hideMetrics ? "eye" : "eye.slash")
                    .padding(8)
                    .background(.regularMaterial, in: Circle())
            }
        }
    }

    private var metricsPanel: some View {
        VStack(spacing: 8) {
            SpeedDisplay(
                speedKmh: holder.trip.state.metrics.currentSpeedKmh,
                unit: "км/ч"
            )
            let cols = [GridItem(.flexible()), GridItem(.flexible())]
            LazyVGrid(columns: cols, spacing: 8) {
                MetricCard(title: "Дистанция",
                           value: String(format: "%.2f", holder.trip.state.metrics.totalDistanceKm),
                           unit: "км",
                           systemImage: "ruler")
                MetricCard(title: "Время",
                           value: formatDuration(holder.trip.state.metrics.elapsedTimeMs),
                           unit: "",
                           systemImage: "clock")
                MetricCard(title: "Сред. скорость",
                           value: String(format: "%.1f", holder.trip.state.metrics.averageSpeedKmh),
                           unit: "км/ч",
                           systemImage: "speedometer")
                MetricCard(title: "Макс. скорость",
                           value: String(format: "%.1f", holder.trip.state.metrics.maxSpeedKmh),
                           unit: "км/ч",
                           systemImage: "bolt")
            }
        }
    }

    private var controlsPanel: some View {
        TripControls(
            isRecording: holder.trip.state.isRecording,
            isPaused: holder.trip.state.isPaused,
            onStart: {
                holder.trip.openStartWizard()
            },
            onPause: { holder.trip.pauseTrip() },
            onResume: { holder.trip.resumeTrip() },
            onStop: { holder.trip.stopTrip() }
        )
    }

    private var tripSummaryBinding: Binding<Bool> {
        Binding(
            get: { holder.trip.state.showTripSummary },
            set: { if !$0 { holder.trip.dismissTripSummary() } }
        )
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

private struct StartTripWizardView: View {
    @EnvironmentObject var holder: AppContainerHolder
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Form {
            Section(header: Text("Режим")) {
                Picker(
                    "Режим",
                    selection: Binding(
                        get: { modeIndex(holder.trip.state.selectedMode) },
                        set: { holder.trip.selectMode(modeAt($0)) }
                    )
                ) {
                    Text("Sensor").tag(0)
                    Text("Hybrid").tag(1)
                    Text("GPS").tag(2)
                }
                .pickerStyle(.segmented)
            }

            Section(header: Text("Маршрут (для Sensor — обязателен)")) {
                if holder.trip.state.availableRoutes.isEmpty {
                    Text("Нет сохранённых маршрутов. Создайте маршрут на вкладке «Маршруты».")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(holder.trip.state.availableRoutes, id: \.id) { route in
                        HStack {
                            Image(systemName: holder.trip.state.selectedRouteId == route.id
                                  ? "largecircle.fill.circle"
                                  : "circle")
                            VStack(alignment: .leading) {
                                Text(route.name)
                                Text(String(format: "%.1f км", route.distanceMeters / 1000))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .contentShape(Rectangle())
                        .onTapGesture { holder.trip.selectRoute(route.id) }
                    }
                }
            }

            Section {
                LoponButton(title: "Старт", systemImage: "play.fill") {
                    holder.trip.startTripFromWizard()
                    dismiss()
                }
            }
        }
        .navigationTitle("Старт поездки")
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("Отмена") { dismiss() }
            }
        }
    }

    private func modeIndex(_ m: NavigationMode) -> Int {
        if m is NavigationMode.Sensor { return 0 }
        if m is NavigationMode.Hybrid { return 1 }
        return 2
    }
    private func modeAt(_ idx: Int) -> NavigationMode {
        switch idx {
        case 0: return NavigationMode.Sensor()
        case 1: return NavigationMode.Hybrid()
        default: return NavigationMode.Gps()
        }
    }
}
