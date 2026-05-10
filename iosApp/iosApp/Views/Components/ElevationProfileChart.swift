import SwiftUI
import Charts

struct ElevationPoint: Identifiable {
    let id = UUID()
    let distanceKm: Double
    let elevationM: Double
}

struct ElevationProfileChart: View {
    let points: [ElevationPoint]
    var height: CGFloat = 140

    var body: some View {
        if points.count < 2 {
            EmptyView()
        } else {
            VStack(alignment: .leading, spacing: 4) {
                Text("Профиль высот")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Chart(points) { p in
                    AreaMark(
                        x: .value("Дистанция", p.distanceKm),
                        y: .value("Высота", p.elevationM)
                    )
                    .foregroundStyle(
                        LinearGradient(
                            colors: [.accentColor.opacity(0.55), .accentColor.opacity(0.05)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    LineMark(
                        x: .value("Дистанция", p.distanceKm),
                        y: .value("Высота", p.elevationM)
                    )
                    .foregroundStyle(.accentColor)
                    .lineStyle(StrokeStyle(lineWidth: 2))
                }
                .frame(height: height)
                .chartYAxis {
                    AxisMarks(position: .leading) { value in
                        AxisGridLine()
                        AxisValueLabel {
                            if let v = value.as(Double.self) {
                                Text("\(Int(v)) м").font(.caption2)
                            }
                        }
                    }
                }
                .chartXAxis {
                    AxisMarks { value in
                        AxisGridLine()
                        AxisValueLabel {
                            if let v = value.as(Double.self) {
                                Text(String(format: "%.1f км", v)).font(.caption2)
                            }
                        }
                    }
                }
            }
        }
    }
}

extension ElevationProfileChart {
    static func from(routePoints: [Double], elevations: [Double?]) -> [ElevationPoint] {
        guard routePoints.count == elevations.count, routePoints.count >= 2 else {
            return []
        }
        var result: [ElevationPoint] = []
        for i in 0..<routePoints.count {
            if let e = elevations[i] {
                result.append(ElevationPoint(distanceKm: routePoints[i], elevationM: e))
            }
        }
        return result
    }
}
