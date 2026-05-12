import SwiftUI
import ComposeApp

struct SensorView: View {
    @EnvironmentObject var holder: AppContainerHolder
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                statusCard
                if holder.sensor.state.isScanning {
                    ProgressView("Сканирование (15 с)...")
                        .padding()
                }
                devicesList
                Spacer()
                actionsRow
            }
            .padding()
            .navigationTitle("Датчик скорости")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Готово") { dismiss() }
                }
            }
            .alert("Ошибка", isPresented: errorBinding) {
                Button("OK") { holder.sensor.clearError() }
            } message: {
                Text(holder.sensor.state.errorMessage ?? "")
            }
        }
    }

    private var statusCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Circle()
                    .fill(statusColor)
                    .frame(width: 10, height: 10)
                Text(statusText)
                    .font(.headline)
            }
            if let device = holder.sensor.state.connectedDevice {
                Text("Устройство: \(device.name ?? device.id)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private var devicesList: some View {
        List(holder.sensor.state.devices, id: \.id) { device in
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(device.name ?? "Без имени")
                    HStack(spacing: 4) {
                        Text(device.id)
                        if device.isPreviouslyConnected {
                            Text("• подключалось ранее")
                                .foregroundStyle(Color.accentColor)
                        }
                    }
                    .font(.caption)
                    .foregroundStyle(.secondary)
                }
                Spacer()
                Text("\(device.rssi) dBm")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .contentShape(Rectangle())
            .onTapGesture {
                Task { await holder.sensor.connect(device.id) }
            }
        }
        .listStyle(.plain)
    }

    private var actionsRow: some View {
        HStack(spacing: 8) {
            if holder.sensor.state.isScanning {
                Button("Остановить") { holder.sensor.stopScan() }
                    .buttonStyle(.bordered)
                    .controlSize(.large)
                    .frame(maxWidth: .infinity)
            } else {
                Button {
                    holder.sensor.startScan()
                } label: {
                    Label("Сканировать", systemImage: "magnifyingglass")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
            }

            if isConnected {
                Button {
                    Task { await holder.sensor.disconnect() }
                } label: {
                    Label("Отключить", systemImage: "xmark.circle")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .tint(.red)
                .controlSize(.large)
            }
        }
    }

    private var isConnected: Bool {
        holder.sensor.state.connectionState is BleConnectionState.Connected
    }

    private var statusText: String {
        switch holder.sensor.state.connectionState {
        case is BleConnectionState.Disconnected: return "Отключено"
        case is BleConnectionState.Connecting: return "Подключение..."
        case is BleConnectionState.Connected: return "Подключено"
        case let e as BleConnectionState.Error: return "Ошибка: \(e.message)"
        default: return "—"
        }
    }
    private var statusColor: Color {
        switch holder.sensor.state.connectionState {
        case is BleConnectionState.Connected: return .green
        case is BleConnectionState.Connecting: return .orange
        case is BleConnectionState.Error: return .red
        default: return .gray
        }
    }

    private var errorBinding: Binding<Bool> {
        Binding(
            get: { holder.sensor.state.errorMessage != nil },
            set: { if !$0 { holder.sensor.clearError() } }
        )
    }
}
