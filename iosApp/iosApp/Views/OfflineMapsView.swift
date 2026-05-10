import SwiftUI
import ComposeApp

struct OfflineMapsView: View {
    @EnvironmentObject var holder: AppContainerHolder
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Form {
            searchSection
            if !holder.offlineMaps.state.searchResults.isEmpty {
                searchResultsSection
            }
            if holder.offlineMaps.state.isDownloading {
                progressSection
            }
            regionsSection
        }
        .navigationTitle("Офлайн-карты")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Готово") { dismiss() }
            }
        }
        .alert("Ошибка", isPresented: errorBinding) {
            Button("OK") { holder.offlineMaps.clearError() }
        } message: {
            Text(holder.offlineMaps.state.errorMessage ?? "")
        }
    }

    private var searchSection: some View {
        Section(header: Text("Поиск региона")) {
            HStack {
                TextField(
                    "Название города или региона",
                    text: Binding(
                        get: { holder.offlineMaps.state.searchQuery },
                        set: { holder.offlineMaps.updateSearchQuery($0) }
                    )
                )
                .textFieldStyle(.roundedBorder)
                Button("Найти") { holder.offlineMaps.searchRegion() }
                    .disabled(holder.offlineMaps.state.isSearching)
            }
            if holder.offlineMaps.state.isSearching {
                ProgressView()
            }
        }
    }

    private var searchResultsSection: some View {
        Section(header: Text("Результаты поиска")) {
            ForEach(holder.offlineMaps.state.searchResults, id: \.name) { result in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(result.name)
                        Text("≈ \(formatBytes(result.estimatedSizeBytes))")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Button("Скачать") {
                        holder.offlineMaps.downloadSearchResult(result)
                    }
                    .disabled(holder.offlineMaps.state.isDownloading)
                }
            }
        }
    }

    private var progressSection: some View {
        Section(header: Text("Загрузка...")) {
            VStack(alignment: .leading, spacing: 4) {
                Text(holder.offlineMaps.state.downloadRegionName)
                if let p = holder.offlineMaps.state.downloadProgress {
                    ProgressView(value: Double(p.percentage))
                    Text("\(Int(p.percentage * 100))% · \(formatBytes(p.completedSize))")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    private var regionsSection: some View {
        Section(header: Text("Загруженные регионы")) {
            if holder.offlineMaps.state.regions.isEmpty {
                Text("Пока нет загруженных регионов")
                    .foregroundStyle(.secondary)
            } else {
                ForEach(holder.offlineMaps.state.regions, id: \.id) { region in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(region.name)
                            Text(formatBytes(region.completedSize))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Button(role: .destructive) {
                            holder.offlineMaps.deleteRegion(region.id)
                        } label: {
                            Image(systemName: "trash")
                        }
                    }
                }
            }
        }
    }

    private func formatBytes(_ b: Int64) -> String {
        let mb = Double(b) / (1024 * 1024)
        return String(format: "%.1f МБ", mb)
    }

    private var errorBinding: Binding<Bool> {
        Binding(
            get: { holder.offlineMaps.state.errorMessage != nil },
            set: { if !$0 { holder.offlineMaps.clearError() } }
        )
    }
}
