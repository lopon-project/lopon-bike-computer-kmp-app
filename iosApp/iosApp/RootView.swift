import SwiftUI
import ComposeApp

struct RootView: View {
    @EnvironmentObject var holder: AppContainerHolder
    @State private var selectedTab: Tab = .trip
    @State private var showSplash: Bool = true

    var body: some View {
        ZStack {
            if showSplash {
                SplashView()
                    .transition(.opacity)
            } else {
                mainContent
                    .transition(.opacity)
            }
        }
        .preferredColorScheme(preferredColorScheme(for: holder.settings.state.settings.themeMode))
        .task {
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            withAnimation { showSplash = false }
        }
    }

    private var mainContent: some View {
        TabView(selection: $selectedTab) {
            NavigationStack {
                TripView()
                    .navigationTitle("Поездка")
            }
            .tabItem { Label("Поездка", systemImage: "bicycle") }
            .tag(Tab.trip)

            NavigationStack {
                HistoryView()
                    .navigationTitle("История")
            }
            .tabItem { Label("История", systemImage: "clock") }
            .tag(Tab.history)

            NavigationStack {
                RoutesView()
                    .navigationTitle("Маршруты")
            }
            .tabItem { Label("Маршруты", systemImage: "map") }
            .tag(Tab.routes)

            NavigationStack {
                MoreView()
                    .navigationTitle("Ещё")
            }
            .tabItem { Label("Ещё", systemImage: "ellipsis.circle") }
            .tag(Tab.more)
        }
    }

    private func preferredColorScheme(for mode: ThemeMode) -> ColorScheme? {
        switch mode {
        case .light: return .light
        case .dark: return .dark
        default: return nil
        }
    }

    enum Tab { case trip, history, routes, more }
}
