import Foundation
import Combine
import ComposeApp

@MainActor
final class HistoryViewModelObservable: ObservableObject {
    @Published private(set) var state: HistoryUiState

    private let vm: HistoryViewModel
    private var stateJob: FlowJob?

    init(_ vm: HistoryViewModel) {
        self.vm = vm
        self.state = vm.uiState.value as! HistoryUiState
        self.stateJob = observe(vm.uiState) { [weak self] (s: HistoryUiState) in
            self?.state = s
        }
    }

    func openTripDetails(_ id: String) { vm.openTripDetails(tripId: id) }
    func closeTripDetails() { vm.closeTripDetails() }
    func requestDeleteTrip(_ id: String) { vm.requestDeleteTrip(tripId: id) }
    func dismissDeleteDialog() { vm.dismissDeleteDialog() }
    func confirmDeleteTrip() { vm.confirmDeleteTrip() }
    func clearError() { vm.clearError() }
}

@MainActor
final class RoutesViewModelObservable: ObservableObject {
    @Published private(set) var state: RoutesUiState

    private let vm: RoutesViewModel
    private var stateJob: FlowJob?

    init(_ vm: RoutesViewModel) {
        self.vm = vm
        self.state = vm.uiState.value as! RoutesUiState
        self.stateJob = observe(vm.uiState) { [weak self] (s: RoutesUiState) in
            self?.state = s
        }
    }

    func createQuickRoute() { vm.createQuickRoute() }
    func clearError() { vm.clearError() }
    func openRouteDetails(_ id: String) { vm.openRouteDetails(routeId: id) }
    func closeRouteDetails() { vm.closeRouteDetails() }
    func requestDeleteRoute(_ id: String) { vm.requestDeleteRoute(routeId: id) }
    func dismissDeleteDialog() { vm.dismissDeleteDialog() }
    func confirmDeleteRoute() { vm.confirmDeleteRoute() }
}

@MainActor
final class SettingsViewModelObservable: ObservableObject {
    @Published private(set) var state: SettingsUiState

    private let vm: SettingsViewModel
    private var stateJob: FlowJob?

    init(_ vm: SettingsViewModel) {
        self.vm = vm
        self.state = vm.uiState.value as! SettingsUiState
        self.stateJob = observe(vm.uiState) { [weak self] (s: SettingsUiState) in
            self?.state = s
        }
    }

    func updateWheel(_ text: String) { vm.updateWheelCircumference(text: text) }
    func selectWheelPreset(_ mm: Double) { vm.selectWheelPreset(mm: mm) }
    func updateUnits(_ units: UnitSystem) { vm.updateUnits(units: units) }
    func updateDefaultMode(_ mode: NavigationMode) { vm.updateDefaultMode(mode: mode) }
    func toggleAutoConnect() { vm.toggleAutoConnect() }
    func toggleKeepScreenOn() { vm.toggleKeepScreenOn() }
    func updateLanguage(_ lang: AppLanguage) { vm.updateLanguage(language: lang) }
    func updateThemeMode(_ mode: ThemeMode) { vm.updateThemeMode(mode: mode) }
    func updateMovingThreshold(_ text: String) { vm.updateMovingSpeedThreshold(text: text) }
    func clearError() { vm.clearError() }
}

@MainActor
final class SensorTestViewModelObservable: ObservableObject {
    @Published private(set) var state: SensorTestUiState

    private let vm: SensorTestViewModel
    private let fileStorage: IosFileStorage
    private var stateJob: FlowJob?

    init(_ vm: SensorTestViewModel, fileStorage: IosFileStorage) {
        self.vm = vm
        self.fileStorage = fileStorage
        self.state = vm.uiState.value as! SensorTestUiState
        self.stateJob = observe(vm.uiState) { [weak self] (s: SensorTestUiState) in
            self?.state = s
        }
    }

    func startScan() { vm.startScan() }
    func stopScan() { vm.stopScan() }
    func clearLog() { vm.clearLog() }
    func clearError() { vm.clearError() }

    func connect(_ deviceId: String) async {
        try? await vm.connectToDevice(deviceId: deviceId)
    }
    func disconnect() async { try? await vm.disconnect() }
    func startSensor() async { try? await vm.startSensor() }
    func stopSensor() async { try? await vm.stopSensor() }
    func requestPermissions() async { try? await vm.requestPermissions() }

    func exportLog() async {
        let text = vm.exportLogText()
        if text.isEmpty { return }
        let path = "exports/sensor_log_\(Int(Date().timeIntervalSince1970)).txt"
        let res = try? await fileStorage.writeText(path: path, content: text)
        if (res?.isSuccess ?? false) == true {
            fileStorage.presentShareSheet(path: path)
        }
    }
}

@MainActor
final class SensorViewModelObservable: ObservableObject {
    @Published private(set) var state: SensorUiState

    private let vm: SensorViewModel
    private var stateJob: FlowJob?

    init(_ vm: SensorViewModel) {
        self.vm = vm
        self.state = vm.uiState.value as! SensorUiState
        self.stateJob = observe(vm.uiState) { [weak self] (s: SensorUiState) in
            self?.state = s
        }
    }

    func startScan() { vm.startScan() }
    func stopScan() { vm.stopScan() }
    func clearError() { vm.clearError() }

    func connect(_ deviceId: String) async {
        try? await vm.connectToDevice(deviceId: deviceId)
    }
    func disconnect() async { try? await vm.disconnect() }
    func startSensor() async { try? await vm.startSensor() }
    func stopSensor() async { try? await vm.stopSensor() }
    func requestPermissions() async { try? await vm.requestPermissions() }
}

@MainActor
final class TripViewModelObservable: ObservableObject {
    @Published private(set) var state: TripUiState

    private let vm: TripViewModel
    private var stateJob: FlowJob?

    init(_ vm: TripViewModel) {
        self.vm = vm
        self.state = vm.uiState.value as! TripUiState
        self.stateJob = observe(vm.uiState) { [weak self] (s: TripUiState) in
            self?.state = s
        }
    }

    func openStartWizard() { vm.openStartWizard() }
    func openStartWizardWithRoute(_ id: String) { vm.openStartWizardWithRoute(routeId: id) }
    func closeStartWizard() { vm.closeStartWizard() }
    func selectMode(_ mode: NavigationMode) { vm.selectMode(mode: mode) }
    func selectRoute(_ id: String) { vm.selectRoute(routeId: id) }
    func quickStartTrip() { vm.quickStartTrip() }
    func startTripFromWizard() { vm.startTripFromWizard() }
    func stopTrip() { vm.stopTrip() }
    func dismissTripSummary() { vm.dismissTripSummary() }
    func pauseTrip() { vm.pauseTrip() }
    func resumeTrip() { vm.resumeTrip() }
    func clearError() { vm.clearError() }
    func createQuickRoute() { vm.createQuickRoute() }
    func exportSummaryGpx() { vm.exportSummaryGpx() }
    func exportTripGpx(_ tripId: String) { vm.exportTripGpx(tripId: tripId) }
}

@MainActor
final class RouteWizardViewModelObservable: ObservableObject {
    @Published private(set) var state: RouteWizardUiState

    private let vm: RouteWizardViewModel
    private var stateJob: FlowJob?

    init(_ vm: RouteWizardViewModel) {
        self.vm = vm
        self.state = vm.uiState.value as! RouteWizardUiState
        self.stateJob = observe(vm.uiState) { [weak self] (s: RouteWizardUiState) in
            self?.state = s
        }
    }

    func setActiveTab(_ tab: WizardTab) { vm.setActiveTab(tab: tab) }
    func addWaypoint(latitude: Double, longitude: Double) {
        vm.addWaypoint(coord: GeoCoordinate(latitude: latitude, longitude: longitude, bearing: nil))
    }
    func removeLastWaypoint() { vm.removeLastWaypoint() }
    func clearAll() { vm.clearAll() }
    func resetState() { vm.resetState() }
    func updateRouteName(_ name: String) { vm.updateRouteName(name: name) }
    func updateSearchQuery(_ q: String) { vm.updateSearchQuery(query: q) }
    func selectSearchResult(_ r: PhotonSearchResult) { vm.selectSearchResult(result: r) }
    func dismissSearch() { vm.dismissSearch() }
    func calculateRoute() { vm.calculateRoute() }
    func saveRoute() { vm.saveRoute() }
    func clearError() { vm.clearError() }
    func requestLocationPermission() { vm.requestLocationPermission() }
    func pickAndImportGpx() { vm.pickAndImportGpx() }
}

@MainActor
final class OfflineMapsViewModelObservable: ObservableObject {
    @Published private(set) var state: OfflineMapsUiState

    private let vm: OfflineMapsViewModel
    private var stateJob: FlowJob?

    init(_ vm: OfflineMapsViewModel) {
        self.vm = vm
        self.state = vm.uiState.value as! OfflineMapsUiState
        self.stateJob = observe(vm.uiState) { [weak self] (s: OfflineMapsUiState) in
            self?.state = s
        }
    }

    func loadRegions() { vm.loadRegions() }
    func updateSearchQuery(_ q: String) { vm.updateSearchQuery(query: q) }
    func searchRegion() { vm.searchRegion() }
    func downloadSearchResult(_ r: SearchResult) { vm.downloadSearchResult(result: r) }
    func deleteRegion(_ id: Int64) { vm.deleteRegion(id: id) }
    func clearError() { vm.clearError() }
}
