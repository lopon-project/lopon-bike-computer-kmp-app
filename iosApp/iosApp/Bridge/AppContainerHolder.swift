import Foundation
import Combine
import ComposeApp

@MainActor
final class AppContainerHolder: ObservableObject {

    let container: IosAppContainer

    let trip: TripViewModelObservable
    let history: HistoryViewModelObservable
    let routes: RoutesViewModelObservable
    let settings: SettingsViewModelObservable
    let sensor: SensorViewModelObservable
    let sensorTest: SensorTestViewModelObservable
    let routeWizard: RouteWizardViewModelObservable
    let offlineMaps: OfflineMapsViewModelObservable

    init(_ container: IosAppContainer) {
        self.container = container
        self.trip = TripViewModelObservable(TripViewModel(container: container))
        self.history = HistoryViewModelObservable(container.historyViewModel)
        self.routes = RoutesViewModelObservable(container.routesViewModel)
        self.settings = SettingsViewModelObservable(container.settingsViewModel)
        self.sensor = SensorViewModelObservable(SensorViewModel(container: container))
        self.sensorTest = SensorTestViewModelObservable(container.sensorTestViewModel, fileStorage: container.fileStorage)
        self.routeWizard = RouteWizardViewModelObservable(RouteWizardViewModel(container: container))
        self.offlineMaps = OfflineMapsViewModelObservable(OfflineMapsViewModel(container: container))
    }

    func requestInitialPermissions() async {
        let pm = container.permissionsManager
        _ = try? await pm.requestPermission(permission: .location)
        _ = try? await pm.requestPermission(permission: .bluetooth)
        _ = try? await pm.requestPermission(permission: .notification)
        await tryAutoReconnectBleIfEnabled()
    }

    private func tryAutoReconnectBleIfEnabled() async {
        let settings = try? await container.settingsRepository.getCurrentSettings()
        guard let settings = settings, settings.autoConnectBle else { return }
        guard let lastId = container.bleAdapter.getLastConnectedDeviceId(), !lastId.isEmpty else { return }
        guard container.permissionsManager.checkPermission(permission: .bluetooth) == .granted else { return }

        let scanned: [BleDevice] = (try? await container.bleAdapter.scan(timeoutMs: 5000)) ?? []
        if scanned.contains(where: { $0.id == lastId }) {
            _ = try? await container.bleAdapter.connect(deviceId: lastId)
        }
    }
}
