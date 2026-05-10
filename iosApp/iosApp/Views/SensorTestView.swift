import SwiftUI
import ComposeApp

struct SensorTestView: View {
    @EnvironmentObject var holder: AppContainerHolder
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                metricsRow
                actionsRow
                logCard
            }
            .padding()
        }
        .navigationTitle("Диагностика")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Готово") { dismiss() }
            }
        }
    }

    private var metricsRow: some View {
        let columns = [GridItem(.flexible()), GridItem(.flexible())]
        return LazyVGrid(columns: columns, spacing: 8) {
            MetricCard(title: "Обороты",
                       value: "\(holder.sensorTest.state.lastRevolutions)",
                       unit: "",
                       systemImage: "arrow.triangle.2.circlepath")
            MetricCard(title: "Время события",
                       value: "\(holder.sensorTest.state.lastEventTime)",
                       unit: "× 1/1024 с",
                       systemImage: "clock")
            MetricCard(title: "Скорость",
                       value: String(format: "%.1f", holder.sensorTest.state.calculatedSpeedKmh),
                       unit: "км/ч",
                       systemImage: "speedometer")
            MetricCard(title: "Пакеты",
                       value: "\(holder.sensorTest.state.readingsCount)",
                       unit: "",
                       systemImage: "antenna.radiowaves.left.and.right")
        }
    }

    private var actionsRow: some View {
        HStack(spacing: 8) {
            Button {
                holder.sensorTest.startScan()
            } label: {
                Label("Скан", systemImage: "magnifyingglass")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)

            Button {
                Task { await holder.sensorTest.startSensor() }
            } label: {
                Label("Старт датчика", systemImage: "play.fill")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(!isConnected)

            Button {
                Task { await holder.sensorTest.stopSensor() }
            } label: {
                Label("Стоп", systemImage: "stop.fill")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .tint(.red)
            .disabled(!isConnected)
        }
    }

    private var logCard: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text("Журнал событий").font(.headline)
                Spacer()
                Button {
                    Task { await holder.sensorTest.exportLog() }
                } label: {
                    Label("Экспорт", systemImage: "square.and.arrow.up")
                }
                .font(.caption)
                .disabled(holder.sensorTest.state.logEntries.isEmpty)
                Button("Очистить") { holder.sensorTest.clearLog() }
                    .font(.caption)
            }
            ForEach(holder.sensorTest.state.logEntries, id: \.timestamp) { entry in
                HStack(alignment: .top, spacing: 6) {
                    Image(systemName: iconFor(entry.type))
                        .font(.caption2)
                        .foregroundStyle(colorFor(entry.type))
                        .frame(width: 16)
                    Text(entry.message)
                        .font(.caption)
                        .foregroundStyle(.primary)
                    Spacer(minLength: 0)
                }
                .padding(.vertical, 1)
                Divider()
            }
            if holder.sensorTest.state.logEntries.isEmpty {
                Text("Журнал пуст. Подключите датчик и нажмите «Старт».")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .padding(.top, 8)
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private var isConnected: Bool {
        holder.sensorTest.state.connectionState is BleConnectionState.Connected
    }

    private func iconFor(_ t: LogEntryType) -> String {
        switch t {
        case .connection: return "link"
        case .sensorData: return "waveform"
        case .config: return "gearshape"
        case .error: return "exclamationmark.triangle"
        case .info: return "info.circle"
        default: return "circle"
        }
    }
    private func colorFor(_ t: LogEntryType) -> Color {
        switch t {
        case .connection: return .blue
        case .sensorData: return .green
        case .config: return .orange
        case .error: return .red
        case .info: return .gray
        default: return .gray
        }
    }
}
