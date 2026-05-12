import SwiftUI
import ComposeApp

struct RoutesView: View {
    @EnvironmentObject var holder: AppContainerHolder
    @State private var showWizard = false

    var body: some View {
        Group {
            if holder.routes.state.isLoading {
                ProgressView("Загрузка...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if holder.routes.state.routes.isEmpty {
                emptyState
            } else {
                List {
                    ForEach(holder.routes.state.routes, id: \.id) { route in
                        NavigationLink {
                            RouteDetailView(route: route)
                        } label: {
                            RouteRow(route: route)
                        }
                        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                            Button(role: .destructive) {
                                holder.routes.requestDeleteRoute(route.id)
                            } label: {
                                Label("Удалить", systemImage: "trash")
                            }
                        }
                    }
                }
                .listStyle(.insetGrouped)
            }
        }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showWizard = true } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showWizard) {
            NavigationStack { RouteWizardView() }
        }
        .alert(
            "Удалить маршрут?",
            isPresented: Binding(
                get: { holder.routes.state.pendingDeleteRouteId != nil },
                set: { if !$0 { holder.routes.dismissDeleteDialog() } }
            )
        ) {
            Button("Отмена", role: .cancel) { holder.routes.dismissDeleteDialog() }
            Button("Удалить", role: .destructive) { holder.routes.confirmDeleteRoute() }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "map")
                .font(.system(size: 64))
                .foregroundStyle(.secondary)
            Text("Маршрутов пока нет")
                .font(.headline)
            Text("Создайте маршрут или импортируйте GPX-файл.")
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .padding(.horizontal)
            Button { showWizard = true } label: {
                Label("Создать маршрут", systemImage: "plus")
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
}

private struct RouteRow: View {
    let route: Route

    var body: some View {
        HStack {
            Image(systemName: "map.fill")
                .frame(width: 32, height: 32)
                .foregroundStyle(Color.accentColor)
            VStack(alignment: .leading, spacing: 2) {
                Text(route.name)
                    .font(.subheadline)
                    .lineLimit(1)
                Text("\(Int(route.distanceMeters / 1000)) км · \(route.pointCount) точек")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(.vertical, 4)
    }
}
