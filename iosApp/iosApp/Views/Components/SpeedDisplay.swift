import SwiftUI

struct SpeedDisplay: View {
    let speedKmh: Double
    let unit: String

    var body: some View {
        VStack(spacing: 0) {
            Text(formatSpeed(speedKmh))
                .font(.system(size: 64, weight: .bold, design: .rounded))
                .foregroundStyle(.primary)
                .monospacedDigit()
            Text(unit)
                .font(.headline)
                .foregroundStyle(.secondary)
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private func formatSpeed(_ v: Double) -> String {
        String(format: "%.1f", max(0, v))
    }
}
