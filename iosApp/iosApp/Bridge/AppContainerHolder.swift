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
        self.sensorTest = SensorTestViewModelObservable(container.sensorTestViewModel)
        self.routeWizard = RouteWizardViewModelObservable(RouteWizardViewModel(container: container))
        self.offlineMaps = OfflineMapsViewModelObservable(OfflineMapsViewModel(container: container))
    }

    func requestInitialPermissions() async {
        let pm = container.permissionsManager
        _ = await pm.requestPermission(permission: .location)
        _ = await pm.requestPermission(permission: .bluetooth)
        _ = await pm.requestPermission(permission: .notification)
    }
}
