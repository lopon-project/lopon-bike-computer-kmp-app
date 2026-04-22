package ru.lopon.ui.trip

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconOffset
import org.maplibre.android.style.layers.PropertyFactory.iconRotate
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.model.Route
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponTypography
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Map configuration constants.
 */
private object MapConfig {
    // Layer IDs
    const val ROUTE_SOURCE_ID = "trip_route_source"
    const val ROUTE_LAYER_ID = "trip_route_layer"
    const val TRACK_SOURCE_ID = "trip_track_source"
    const val TRACK_LAYER_ID = "trip_track_layer"
    const val POSITION_SOURCE_ID = "trip_position_source"
    const val POSITION_HALO_LAYER_ID = "trip_position_halo_layer"
    const val POSITION_LAYER_ID = "trip_position_layer"

    // Bearing arrow
    const val BEARING_SOURCE_ID = "trip_bearing_source"
    const val BEARING_LAYER_ID = "trip_bearing_layer"
    const val BEARING_ICON_ID = "bearing_arrow"
    const val BEARING_ICON_SIZE = 0.5f

    // Camera behavior
    const val CAMERA_MOVE_THRESHOLD_METERS = 30.0
    const val DEFAULT_MAP_ZOOM = 13.5
    const val CAMERA_UPDATE_MIN_INTERVAL_MS = 1200L
    const val MAP_BOUNDS_PADDING = 64

    // Route rendering
    const val ROUTE_LINE_WIDTH = 5f
    const val TRACK_LINE_WIDTH = 4f
    const val POSITION_DOT_RADIUS = 8f
    const val POSITION_HALO_RADIUS = 16f
    const val POSITION_STROKE_WIDTH = 3f

    const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
    const val LOG_TAG = "TripMapView"
}

/**
 * Holds mutable map state scoped to a single TripMapCard instance.
 */
private class MapStateHolder {
    var mapRef: MapLibreMap? = null
    var lastCameraUpdateMs: Long = 0L
    var lastCenterToken: Int = -1
    var lastCameraMode: TripMapCameraMode? = null
    var lastOverviewRouteId: String? = null
    var lastOverviewRoutePointCount: Int = -1
    var lastRenderedRouteId: String? = null
    var lastRenderedRoutePointCount: Int = -1
    var lastRenderedPosition: GeoCoordinate? = null
    var lastTrackPointCount: Int = -1
}

internal enum class TripMapCameraMode {
    FOLLOW_POSITION,
    ROUTE_OVERVIEW
}

@Composable
internal fun TripMapCard(
    route: Route?,
    currentPosition: GeoCoordinate?,
    autoCenterEnabled: Boolean,
    centerNowToken: Int,
    cameraMode: TripMapCameraMode,
    recordedTrack: List<GeoCoordinate> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = rememberMapViewWithLifecycle(context, lifecycleOwner)
    val stateHolder = remember { MapStateHolder() }

    Box(modifier = modifier) {
        if (mapView != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { mapView },
                update = {
                    val map = stateHolder.mapRef
                    if (map != null && map.style != null) {
                        updateMapData(stateHolder, map, route, currentPosition, recordedTrack, autoCenterEnabled, centerNowToken, cameraMode)
                    } else {
                        mapView.getMapAsync { newMap ->
                            stateHolder.mapRef = newMap
                            if (newMap.style == null) {
                                Log.d(MapConfig.LOG_TAG, "Loading map style: ${MapConfig.STYLE_URL}")
                                newMap.setStyle(MapConfig.STYLE_URL) {
                                    Log.d(MapConfig.LOG_TAG, "Map style loaded successfully")
                                    ensureStyleLayers(newMap)
                                    updateMapData(stateHolder, newMap, route, currentPosition, recordedTrack, autoCenterEnabled, centerNowToken, cameraMode)
                                }
                            } else {
                                ensureStyleLayers(newMap)
                                updateMapData(stateHolder, newMap, route, currentPosition, recordedTrack, autoCenterEnabled, centerNowToken, cameraMode)
                            }
                        }
                    }
                }
            )
        } else {
            Text(
                text = "Карта временно недоступна на этом устройстве",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(LoponDimens.spacerMedium),
                style = LoponTypography.caption,
                color = LoponColors.onSurfaceSecondary
            )
        }

        if (route == null && currentPosition == null && recordedTrack.isEmpty()) {
            Text(
                text = "Ожидание GPS-сигнала…",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(LoponDimens.spacerSmall),
                style = LoponTypography.caption,
                color = LoponColors.onSurfaceSecondary
            )
        }
    }
}

@Composable
private fun rememberMapViewWithLifecycle(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
): MapView? {
    val mapView = remember {
        runCatching {
            MapView(context).apply { onCreate(null) }
        }.onFailure { error ->
            Log.e(MapConfig.LOG_TAG, "Failed to initialize MapView", error)
        }.getOrNull()
    }

    if (mapView == null) return null

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return mapView
}

private fun ensureStyleLayers(map: MapLibreMap) {
    val style: Style = map.style ?: return

    // Track source + layer (recorded GPS breadcrumb trail)
    if (style.getSource(MapConfig.TRACK_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(MapConfig.TRACK_SOURCE_ID, FeatureCollection.fromFeatures(emptyArray())))
    }
    if (style.getLayer(MapConfig.TRACK_LAYER_ID) == null) {
        style.addLayer(
            LineLayer(MapConfig.TRACK_LAYER_ID, MapConfig.TRACK_SOURCE_ID).withProperties(
                lineColor("#FFD400"),
                lineWidth(MapConfig.TRACK_LINE_WIDTH),
                lineCap("round"),
                lineJoin("round")
            )
        )
    }

    // Route source + layer
    if (style.getSource(MapConfig.ROUTE_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(MapConfig.ROUTE_SOURCE_ID, FeatureCollection.fromFeatures(emptyArray())))
    }
    if (style.getLayer(MapConfig.ROUTE_LAYER_ID) == null) {
        style.addLayer(
            LineLayer(MapConfig.ROUTE_LAYER_ID, MapConfig.ROUTE_SOURCE_ID).withProperties(
                lineColor("#E65100"),
                lineWidth(MapConfig.ROUTE_LINE_WIDTH),
                lineCap("round"),
                lineJoin("round")
            )
        )
    }

    // Position source
    if (style.getSource(MapConfig.POSITION_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(MapConfig.POSITION_SOURCE_ID, FeatureCollection.fromFeatures(emptyArray())))
    }
    // Position halo
    if (style.getLayer(MapConfig.POSITION_HALO_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(MapConfig.POSITION_HALO_LAYER_ID, MapConfig.POSITION_SOURCE_ID).withProperties(
                circleColor("#FFD400"),
                circleRadius(MapConfig.POSITION_HALO_RADIUS),
                circleStrokeWidth(0f)
            )
        )
    }
    // Position dot
    if (style.getLayer(MapConfig.POSITION_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(MapConfig.POSITION_LAYER_ID, MapConfig.POSITION_SOURCE_ID).withProperties(
                circleColor("#FFD400"),
                circleRadius(MapConfig.POSITION_DOT_RADIUS),
                circleStrokeColor("#111111"),
                circleStrokeWidth(MapConfig.POSITION_STROKE_WIDTH)
            )
        )
    }

    // Bearing arrow icon
    if (style.getImage(MapConfig.BEARING_ICON_ID) == null) {
        style.addImage(MapConfig.BEARING_ICON_ID, createBearingArrowBitmap())
    }
    if (style.getSource(MapConfig.BEARING_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(MapConfig.BEARING_SOURCE_ID, FeatureCollection.fromFeatures(emptyArray())))
    }
    if (style.getLayer(MapConfig.BEARING_LAYER_ID) == null) {
        style.addLayer(
            SymbolLayer(MapConfig.BEARING_LAYER_ID, MapConfig.BEARING_SOURCE_ID).withProperties(
                iconImage(MapConfig.BEARING_ICON_ID),
                iconSize(MapConfig.BEARING_ICON_SIZE),
                iconRotate(0f),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
                iconOffset(arrayOf(0f, -24f))
            )
        )
    }
}

private fun updateMapData(
    state: MapStateHolder,
    map: MapLibreMap,
    route: Route?,
    currentPosition: GeoCoordinate?,
    recordedTrack: List<GeoCoordinate>,
    autoCenterEnabled: Boolean,
    centerNowToken: Int,
    cameraMode: TripMapCameraMode
) {
    val style = map.style ?: return
    ensureStyleLayers(map)

    val forceCenter = state.lastCenterToken != centerNowToken
    if (forceCenter) state.lastCenterToken = centerNowToken

    val modeChanged = state.lastCameraMode != null && state.lastCameraMode != cameraMode
    state.lastCameraMode = cameraMode

    val routeId = route?.id
    val routePointCount = route?.points?.size ?: 0
    val routeChanged = state.lastOverviewRouteId != routeId || state.lastOverviewRoutePointCount != routePointCount
    state.lastOverviewRouteId = routeId
    state.lastOverviewRoutePointCount = routePointCount

    val shouldUpdateRouteSource = state.lastRenderedRouteId != routeId || state.lastRenderedRoutePointCount != routePointCount
    val shouldUpdatePositionSource = state.lastRenderedPosition != currentPosition
    val shouldUpdateTrack = state.lastTrackPointCount != recordedTrack.size

    val routeSource = style.getSourceAs<GeoJsonSource>(MapConfig.ROUTE_SOURCE_ID)
    val positionSource = style.getSourceAs<GeoJsonSource>(MapConfig.POSITION_SOURCE_ID)
    val trackSource = style.getSourceAs<GeoJsonSource>(MapConfig.TRACK_SOURCE_ID)

    // Update recorded track (breadcrumb trail for "just riding" mode)
    if (trackSource != null && shouldUpdateTrack) {
        if (recordedTrack.size >= 2) {
            val linePoints = recordedTrack.map { Point.fromLngLat(it.longitude, it.latitude) }
            trackSource.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(linePoints)))
        } else {
            trackSource.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
        }
        state.lastTrackPointCount = recordedTrack.size
    }

    // Update route line
    if (routeSource != null) {
        val routePoints = route?.points.orEmpty()
        if (routePoints.size >= 2) {
            if (shouldUpdateRouteSource) {
                val line = LineString.fromLngLats(routePoints.map { Point.fromLngLat(it.longitude, it.latitude) })
                routeSource.setGeoJson(Feature.fromGeometry(line))
                state.lastRenderedRouteId = routeId
                state.lastRenderedRoutePointCount = routePointCount
            }

            if (autoCenterEnabled || forceCenter) {
                when (cameraMode) {
                    TripMapCameraMode.FOLLOW_POSITION -> {
                        if (currentPosition == null) {
                            val first = routePoints.first()
                            maybeMoveCamera(state, map, first.latitude, first.longitude, MapConfig.DEFAULT_MAP_ZOOM, forceCenter)
                        }
                    }
                    TripMapCameraMode.ROUTE_OVERVIEW -> {
                        if (forceCenter || modeChanged || routeChanged) {
                            maybeMoveCameraToBounds(state, map, routePoints, currentPosition)
                        }
                    }
                }
            }
        } else {
            if (shouldUpdateRouteSource) {
                routeSource.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
                state.lastRenderedRouteId = null
                state.lastRenderedRoutePointCount = -1
            }
        }
    }

    // Update bearing arrow
    val bearingSource = style.getSourceAs<GeoJsonSource>(MapConfig.BEARING_SOURCE_ID)
    if (bearingSource != null) {
        if (currentPosition != null && currentPosition.bearing != null) {
            val point = Point.fromLngLat(currentPosition.longitude, currentPosition.latitude)
            bearingSource.setGeoJson(Feature.fromGeometry(point))
            style.getLayerAs<SymbolLayer>(MapConfig.BEARING_LAYER_ID)
                ?.setProperties(iconRotate(currentPosition.bearing))
        } else {
            bearingSource.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
        }
    }

    // Update position dot
    if (positionSource != null) {
        if (currentPosition != null) {
            if (shouldUpdatePositionSource) {
                val point = Point.fromLngLat(currentPosition.longitude, currentPosition.latitude)
                positionSource.setGeoJson(Feature.fromGeometry(point))
                state.lastRenderedPosition = currentPosition
            }
            if ((autoCenterEnabled || forceCenter) && cameraMode == TripMapCameraMode.FOLLOW_POSITION) {
                maybeMoveCamera(state, map, currentPosition.latitude, currentPosition.longitude, null, forceCenter)
            }
        } else {
            if (shouldUpdatePositionSource) {
                positionSource.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
                state.lastRenderedPosition = null
            }
        }
    }
}

private fun maybeMoveCamera(
    state: MapStateHolder,
    map: MapLibreMap,
    targetLat: Double,
    targetLon: Double,
    zoom: Double?,
    force: Boolean
) {
    val now = System.currentTimeMillis()
    if (!force && now - state.lastCameraUpdateMs < MapConfig.CAMERA_UPDATE_MIN_INTERVAL_MS) return

    val current = map.cameraPosition.target
    if (!force && current != null) {
        val distance = haversineDistanceMeters(current.latitude, current.longitude, targetLat, targetLon)
        if (distance < MapConfig.CAMERA_MOVE_THRESHOLD_METERS) return
    }

    val target = LatLng(targetLat, targetLon)
    if (zoom != null) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, zoom))
    } else {
        map.animateCamera(CameraUpdateFactory.newLatLng(target))
    }
    state.lastCameraUpdateMs = now
}

private fun maybeMoveCameraToBounds(
    state: MapStateHolder,
    map: MapLibreMap,
    points: List<GeoCoordinate>,
    currentPosition: GeoCoordinate?
) {
    if (points.size < 2) return

    val builder = LatLngBounds.Builder()
    points.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
    currentPosition?.let { builder.include(LatLng(it.latitude, it.longitude)) }
    val bounds = builder.build()

    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, MapConfig.MAP_BOUNDS_PADDING))
    state.lastCameraUpdateMs = System.currentTimeMillis()
}

private fun createBearingArrowBitmap(): Bitmap {
    val size = 64
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111111")
        style = Paint.Style.FILL
    }
    val path = Path().apply {
        moveTo(size / 2f, 4f)
        lineTo(size - 8f, size - 4f)
        lineTo(size / 2f, size * 0.65f)
        lineTo(8f, size - 4f)
        close()
    }
    canvas.drawPath(path, paint)
    return bitmap
}

private fun haversineDistanceMeters(
    lat1: Double, lon1: Double, lat2: Double, lon2: Double
): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}
