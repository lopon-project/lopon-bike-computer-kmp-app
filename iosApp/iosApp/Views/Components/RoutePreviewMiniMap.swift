import SwiftUI
import CoreLocation
import ComposeApp

struct RoutePreviewMiniMap: View {
    let points: [GeoCoordinate]
    var height: CGFloat = 140

    var body: some View {
        let coords = points.map {
            CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude)
        }
        let center = coords.first ?? CLLocationCoordinate2D(latitude: 55.7558, longitude: 37.6173)

        return MapLibreMapView(
            initialCenter: center,
            initialZoom: 12,
            routePoints: coords
        )
        .frame(height: height)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .allowsHitTesting(false)
    }
}
