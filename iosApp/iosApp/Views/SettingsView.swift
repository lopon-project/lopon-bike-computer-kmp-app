import SwiftUI
import ComposeApp

struct SettingsView: View {
    @EnvironmentObject var holder: AppContainerHolder
    @Environment(\.dismiss) private var dismiss

    private let wheelPresets: [(label: String, mm: Double)] = [
        ("26x2.0", 2070),
        ("27.5x2.1", 2148),
        ("29x2.1", 2288),
        ("700x23c", 2096),
        ("700x25c", 2105),
        ("700x28c", 2136),
        ("700x32c", 2155),
        ("700x42c", 2224),
        ("700x45c", 2242)
    ]

    var body: some View {
        Form {
            wheelSection
            unitsSection
            modeSection
            bluetoothSection
            screenSection
            languageSection
            themeSection
        }
        .navigationTitle("Настройки")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Готово") { dismiss() }
            }
        }
    }

    private var wheelSection: some View {
        Section(header: Text("Окружность колеса"), footer: Text("Введите длину окружности в мм (1000–2500) или выберите пресет.")) {
            TextField(
                "Длина окружности (мм)",
                text: Binding(
                    get: { holder.settings.state.wheelInputText },
                    set: { holder.settings.updateWheel($0) }
                )
            )
            .keyboardType(.numberPad)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(wheelPresets, id: \.label) { preset in
                        Button(action: { holder.settings.selectWheelPreset(preset.mm) }) {
                            Text(preset.label)
                                .font(.caption)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(.thinMaterial, in: Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private var unitsSection: some View {
        Section(header: Text("Единицы измерения")) {
            Picker(
                "Единицы",
                selection: Binding(
                    get: { holder.settings.state.settings.units },
                    set: { holder.settings.updateUnits($0) }
                )
            ) {
                Text("Метрическая").tag(UnitSystem.metric)
                Text("Имперская").tag(UnitSystem.imperial)
            }
            .pickerStyle(.segmented)
        }
    }

    private var modeSection: some View {
        Section(header: Text("Режим навигации по умолчанию")) {
            Picker(
                "Режим",
                selection: Binding(
                    get: { modeIndex(holder.settings.state.settings.defaultMode) },
                    set: { holder.settings.updateDefaultMode(modeAt($0)) }
                )
            ) {
                Text("Sensor").tag(0)
                Text("Hybrid").tag(1)
                Text("GPS").tag(2)
            }
            .pickerStyle(.segmented)
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

    private var bluetoothSection: some View {
        Section(header: Text("Bluetooth")) {
            Toggle(
                "Автоподключение к датчику",
                isOn: Binding(
                    get: { holder.settings.state.settings.autoConnectBle },
                    set: { _ in holder.settings.toggleAutoConnect() }
                )
            )

            VStack(alignment: .leading, spacing: 4) {
                Text("Порог скорости движения (км/ч)")
                Slider(
                    value: Binding(
                        get: { holder.settings.state.settings.movingSpeedThresholdKmh },
                        set: { holder.settings.updateMovingThreshold(String(format: "%.1f", $0)) }
                    ),
                    in: 0.5...10.0,
                    step: 0.5
                )
                Text(String(format: "%.1f км/ч", holder.settings.state.settings.movingSpeedThresholdKmh))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private var screenSection: some View {
        Section(header: Text("Экран")) {
            Toggle(
                "Не гасить экран во время поездки",
                isOn: Binding(
                    get: { holder.settings.state.settings.keepScreenOn },
                    set: { _ in holder.settings.toggleKeepScreenOn() }
                )
            )
        }
    }

    private var languageSection: some View {
        Section(header: Text("Язык интерфейса")) {
            Picker(
                "Язык",
                selection: Binding(
                    get: { holder.settings.state.settings.language },
                    set: { holder.settings.updateLanguage($0) }
                )
            ) {
                Text("Системный").tag(AppLanguage.system)
                Text("Русский").tag(AppLanguage.ru)
                Text("English").tag(AppLanguage.en)
            }
        }
    }

    private var themeSection: some View {
        Section(header: Text("Тема оформления")) {
            Picker(
                "Тема",
                selection: Binding(
                    get: { holder.settings.state.settings.themeMode },
                    set: { holder.settings.updateThemeMode($0) }
                )
            ) {
                Text("Системная").tag(ThemeMode.system)
                Text("Светлая").tag(ThemeMode.light)
                Text("Тёмная").tag(ThemeMode.dark)
            }
            .pickerStyle(.segmented)
        }
    }
}
