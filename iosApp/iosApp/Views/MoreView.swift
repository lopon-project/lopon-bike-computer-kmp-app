import SwiftUI

struct MoreView: View {
    @EnvironmentObject var holder: AppContainerHolder
    @State private var showSensorSheet = false
    @State private var showSettings = false
    @State private var showOfflineMaps = false
    @State private var showDiagnostics = false

    var body: some View {
        List {
            Section(header: Text("Подключения")) {
                row(
                    title: "Подключение датчика",
                    subtitle: "Сканирование и подключение к BLE-датчику",
                    icon: "antenna.radiowaves.left.and.right"
                ) { showSensorSheet = true }

                row(
                    title: "Диагностика датчика",
                    subtitle: "Журнал событий BLE и текущие показатели",
                    icon: "waveform.path.ecg"
                ) { showDiagnostics = true }
            }

            Section(header: Text("Карты и настройки")) {
                row(
                    title: "Офлайн-карты",
                    subtitle: "Скачивание регионов для работы без интернета",
                    icon: "map"
                ) { showOfflineMaps = true }

                row(
                    title: "Настройки",
                    subtitle: "Параметры велосипеда, единицы, тема",
                    icon: "gearshape"
                ) { showSettings = true }
            }

            Section(header: Text("О приложении")) {
                Label("Lopon — велосипедный навигатор", systemImage: "bicycle")
                Text("Версия 1.0").foregroundStyle(.secondary)
            }
        }
        .sheet(isPresented: $showSensorSheet) { SensorView() }
        .sheet(isPresented: $showSettings) {
            NavigationStack { SettingsView() }
        }
        .sheet(isPresented: $showOfflineMaps) {
            NavigationStack { OfflineMapsView() }
        }
        .sheet(isPresented: $showDiagnostics) {
            NavigationStack { SensorTestView() }
        }
    }

    private func row(
        title: String,
        subtitle: String,
        icon: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack {
                Image(systemName: icon)
                    .frame(width: 28)
                    .foregroundStyle(Color.accentColor)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).foregroundStyle(.primary)
                    Text(subtitle).font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }
        }
    }
}
