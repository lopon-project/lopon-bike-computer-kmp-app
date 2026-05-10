import SwiftUI
import ComposeApp

struct StatusIndicators: View {
    let bleConnected: Bool
    let bleConnectionState: BleConnectionState
    let gpsStatus: LocationStatus
    let mode: NavigationMode

    var body: some View {
        HStack(spacing: 8) {
            pill(icon: "location.fill", label: gpsLabel, color: gpsColor)
            pill(icon: "antenna.radiowaves.left.and.right",
                 label: bleLabel,
                 color: bleColor)
            pill(icon: "speedometer", label: modeLabel, color: .accentColor)
        }
    }

    private func pill(icon: String, label: String, color: Color) -> some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption2)
            Text(label)
                .font(.caption)
                .lineLimit(1)
        }
        .foregroundStyle(.white)
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(color.opacity(0.85), in: Capsule())
    }

    private var gpsLabel: String {
        switch gpsStatus {
        case .active: return "GPS ✓"
        case .searching: return "GPS поиск"
        case .error: return "GPS ошибка"
        default: return "GPS off"
        }
    }
    private var gpsColor: Color {
        switch gpsStatus {
        case .active: return .green
        case .searching: return .orange
        case .error: return .red
        default: return .gray
        }
    }

    private var bleLabel: String {
        switch bleConnectionState {
        case is BleConnectionState.Connected: return "BLE ✓"
        case is BleConnectionState.Connecting: return "BLE подкл."
        case is BleConnectionState.Error:
            return "BLE ошибка"
        default: return "BLE off"
        }
    }
    private var bleColor: Color {
        switch bleConnectionState {
        case is BleConnectionState.Connected: return .green
        case is BleConnectionState.Connecting: return .orange
        case is BleConnectionState.Error: return .red
        default: return .gray
        }
    }

    private var modeLabel: String {
        switch mode {
        case is NavigationMode.Sensor: return "Sensor"
        case is NavigationMode.Hybrid: return "Hybrid"
        case is NavigationMode.Gps: return "GPS"
        default: return "—"
        }
    }
}
