import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    @StateObject private var containerHolder: AppContainerHolder

    init() {
        let container = IosAppContainer.shared
        container.attachOfflineMapHelper(helper: SwiftMapLibreOfflineHelper())
        _containerHolder = StateObject(wrappedValue: AppContainerHolder(container))
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(containerHolder)
                .task {
                    await containerHolder.requestInitialPermissions()
                }
        }
    }
}
