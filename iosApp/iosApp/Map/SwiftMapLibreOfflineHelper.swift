import Foundation
import MapLibre
import ComposeApp

@objc final class SwiftMapLibreOfflineHelper: NSObject, PlatformOfflineMapHelper {

    static let defaultStyleUrl = "https://tiles.openfreemap.org/styles/liberty"

    private let storage = MLNOfflineStorage.shared

    func listRegions() async throws -> [OfflineRegionInfo] {
        try await withCheckedThrowingContinuation { continuation in
            storage.getPacks { packs, error in
                if let err = error {
                    continuation.resume(throwing: err)
                    return
                }
                let result = (packs ?? []).enumerated().map { (idx, pack) -> OfflineRegionInfo in
                    let progress = pack.progress
                    let name = (try? Self.decodeName(from: pack.context)) ?? "Region \(idx + 1)"
                    return OfflineRegionInfo(
                        id: Int64(idx),
                        name: name,
                        completedResources: Int64(progress.countOfResourcesCompleted),
                        requiredResources: Int64(progress.countOfResourcesExpected),
                        completedSize: Int64(progress.countOfBytesCompleted),
                        isComplete: progress.countOfResourcesCompleted >= progress.countOfResourcesExpected
                                   && progress.countOfResourcesExpected > 0
                    )
                }
                continuation.resume(returning: result)
            }
        }
    }

    func downloadRegion(name: String,
                        bounds: CommonBounds,
                        minZoom: Double,
                        maxZoom: Double) -> Kotlinx_coroutines_coreFlow {
        let flow = MutableSharedFlowKt.MutableSharedFlow(
            replay: 1,
            extraBufferCapacity: 64,
            onBufferOverflow: BufferOverflow.dropOldest
        )

        let sw = CLLocationCoordinate2D(latitude: bounds.southWest.lat,
                                        longitude: bounds.southWest.lng)
        let ne = CLLocationCoordinate2D(latitude: bounds.northEast.lat,
                                        longitude: bounds.northEast.lng)
        let mlBounds = MLNCoordinateBounds(sw: sw, ne: ne)

        guard let styleURL = URL(string: Self.defaultStyleUrl) else {
            return flow
        }

        let region = MLNTilePyramidOfflineRegion(
            styleURL: styleURL,
            bounds: mlBounds,
            fromZoomLevel: minZoom,
            toZoomLevel: maxZoom
        )

        let context = (try? Self.encodeName(name)) ?? Data()

        var observer: NSObjectProtocol?
        observer = NotificationCenter.default.addObserver(
            forName: NSNotification.Name.MLNOfflinePackProgressChanged,
            object: nil, queue: .main
        ) { note in
            guard let pack = note.object as? MLNOfflinePack else { return }
            let progress = pack.progress
            let percentage: Float = progress.countOfResourcesExpected > 0
                ? Float(progress.countOfResourcesCompleted) / Float(progress.countOfResourcesExpected)
                : 0
            _ = flow.tryEmit(value: DownloadProgress(
                completedResources: Int64(progress.countOfResourcesCompleted),
                requiredResources: Int64(progress.countOfResourcesExpected),
                completedSize: Int64(progress.countOfBytesCompleted),
                percentage: percentage
            ))
            if progress.countOfResourcesCompleted >= progress.countOfResourcesExpected
               && progress.countOfResourcesExpected > 0,
               let obs = observer {
                NotificationCenter.default.removeObserver(obs)
            }
        }

        storage.addPack(for: region, withContext: context) { pack, error in
            if let err = error {
                NSLog("[Lopon] addPack error: \(err.localizedDescription)")
                return
            }
            pack?.resume()
        }

        return flow
    }

    func deleteRegion(id: Int64) async throws -> ResultBox<KotlinUnit> {
        try await withCheckedThrowingContinuation { continuation in
            storage.getPacks { packs, error in
                if let err = error {
                    continuation.resume(returning: ResultBox<KotlinUnit>.failure(message: err.localizedDescription))
                    return
                }
                guard let packs = packs, Int(id) >= 0, Int(id) < packs.count else {
                    continuation.resume(returning: ResultBox<KotlinUnit>.failure(message: "Region not found"))
                    return
                }
                self.storage.removePack(packs[Int(id)]) { err in
                    if let err = err {
                        continuation.resume(returning: ResultBox<KotlinUnit>.failure(message: err.localizedDescription))
                    } else {
                        continuation.resume(returning: ResultBox<KotlinUnit>.success())
                    }
                }
            }
        }
    }

    func estimateRegionSizeBytes(bounds: CommonBounds,
                                 minZoom: Double,
                                 maxZoom: Double) -> Int64 {
        let lat1 = bounds.southWest.lat
        let lat2 = bounds.northEast.lat
        let lng1 = bounds.southWest.lng
        let lng2 = bounds.northEast.lng

        var totalTiles: Double = 0
        var z = Int(minZoom)
        while Double(z) <= maxZoom {
            let n = pow(2.0, Double(z))
            let xTiles = abs((lng2 + 180.0) / 360.0 - (lng1 + 180.0) / 360.0) * n
            let yTiles = abs(latToTileY(lat1, n: n) - latToTileY(lat2, n: n))
            totalTiles += xTiles * yTiles
            z += 1
        }
        let avgBytesPerTile: Double = 20 * 1024
        return Int64(totalTiles * avgBytesPerTile)
    }

    private func latToTileY(_ lat: Double, n: Double) -> Double {
        let radians = lat * .pi / 180.0
        return (1.0 - log(tan(radians) + 1.0 / cos(radians)) / .pi) / 2.0 * n
    }

    private static func encodeName(_ name: String) throws -> Data {
        let dict: [String: Any] = ["name": name]
        return try JSONSerialization.data(withJSONObject: dict, options: [])
    }

    private static func decodeName(from context: Data) throws -> String {
        let obj = try JSONSerialization.jsonObject(with: context, options: [])
        if let dict = obj as? [String: Any], let name = dict["name"] as? String {
            return name
        }
        return "Region"
    }
}

@objc public final class ResultBox<T: AnyObject>: NSObject {
    @objc public let isSuccess: Bool
    @objc public let errorMessage: String?

    private init(success: Bool, error: String?) {
        self.isSuccess = success
        self.errorMessage = error
    }

    @objc public static func success() -> ResultBox<T> { .init(success: true, error: nil) }
    @objc public static func failure(message: String) -> ResultBox<T> {
        .init(success: false, error: message)
    }
}
