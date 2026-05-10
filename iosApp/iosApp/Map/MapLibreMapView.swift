import SwiftUI
import MapLibre
import ComposeApp

struct MapLibreMapView: UIViewRepresentable {

    var initialCenter: CLLocationCoordinate2D = .init(latitude: 55.7558, longitude: 37.6173)
    var initialZoom: Double = 11.0

    var userLocation: CLLocationCoordinate2D? = nil
    var followUser: Bool = false

    var routePoints: [CLLocationCoordinate2D] = []

    var recordedTrackPoints: [CLLocationCoordinate2D] = []

    var waypointMarkers: [CLLocationCoordinate2D] = []

    var onTap: ((CLLocationCoordinate2D) -> Void)? = nil

    func makeUIView(context: Context) -> MLNMapView {
        let styleURL = URL(string: SwiftMapLibreOfflineHelper.defaultStyleUrl)!
        let map = MLNMapView(frame: .zero, styleURL: styleURL)
        map.setCenter(initialCenter, zoomLevel: initialZoom, animated: false)
        map.showsUserLocation = true
        map.tintColor = .systemBlue
        map.delegate = context.coordinator

        let tap = UITapGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.handleTap(_:))
        )
        map.addGestureRecognizer(tap)

        return map
    }

    func updateUIView(_ map: MLNMapView, context: Context) {
        context.coordinator.parent = self

        if followUser, let user = userLocation {
            map.setCenter(user, zoomLevel: max(15.0, map.zoomLevel), animated: true)
        }

        context.coordinator.refreshOverlays(on: map)
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    final class Coordinator: NSObject, MLNMapViewDelegate {
        var parent: MapLibreMapView
        private var routeAnnotation: MLNPolyline?
        private var trackAnnotation: MLNPolyline?
        private var waypointAnnotations: [MLNPointAnnotation] = []

        init(parent: MapLibreMapView) {
            self.parent = parent
        }

        func refreshOverlays(on map: MLNMapView) {
            if let r = routeAnnotation { map.removeAnnotation(r) }
            if let t = trackAnnotation { map.removeAnnotation(t) }
            if !waypointAnnotations.isEmpty {
                map.removeAnnotations(waypointAnnotations)
            }
            routeAnnotation = nil
            trackAnnotation = nil
            waypointAnnotations.removeAll()

            if parent.routePoints.count >= 2 {
                var coords = parent.routePoints
                let line = MLNPolyline(coordinates: &coords, count: UInt(coords.count))
                line.title = "route"
                map.addAnnotation(line)
                routeAnnotation = line
            }

            if parent.recordedTrackPoints.count >= 2 {
                var coords = parent.recordedTrackPoints
                let line = MLNPolyline(coordinates: &coords, count: UInt(coords.count))
                line.title = "track"
                map.addAnnotation(line)
                trackAnnotation = line
            }

            for (idx, coord) in parent.waypointMarkers.enumerated() {
                let p = MLNPointAnnotation()
                p.coordinate = coord
                p.title = "Точка \(idx + 1)"
                map.addAnnotation(p)
                waypointAnnotations.append(p)
            }
        }

        func mapView(_ mapView: MLNMapView,
                     strokeColorForShapeAnnotation annotation: MLNShape) -> UIColor {
            if let title = annotation.title ?? nil {
                if title == "route" { return UIColor.systemBlue }
                if title == "track" { return UIColor.systemGreen }
            }
            return UIColor.systemBlue
        }

        func mapView(_ mapView: MLNMapView,
                     lineWidthForPolylineAnnotation annotation: MLNPolyline) -> CGFloat {
            return 5.0
        }

        @objc func handleTap(_ recognizer: UITapGestureRecognizer) {
            guard let onTap = parent.onTap else { return }
            guard let map = recognizer.view as? MLNMapView else { return }
            let point = recognizer.location(in: map)
            let coord = map.convert(point, toCoordinateFrom: map)
            onTap(coord)
        }
    }
}
