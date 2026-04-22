package ru.lopon.ui.routes

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
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineDasharray
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.domain.model.Route
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponTypography
import ru.lopon.ui.theme.LoponColors
import kotlin.math.sqrt

private object WizardMapConfig {
    const val LOG_TAG = "RouteWizardMap"
    const val DEFAULT_ZOOM = 12.0
    const val ROUTE_TAP_TOLERANCE_PX = 60f

    const val WAYPOINTS_SOURCE = "wizard_waypoints_source"
    const val WAYPOINTS_LAYER = "wizard_waypoints_layer"
    const val SELECTED_WP_SOURCE = "wizard_selected_wp_source"
    const val SELECTED_WP_LAYER = "wizard_selected_wp_layer"
    const val ROUTE_SOURCE = "wizard_route_source"
    const val ROUTE_LAYER = "wizard_route_layer"
    const val STRAIGHT_LINE_SOURCE = "wizard_straight_source"
    const val STRAIGHT_LINE_LAYER = "wizard_straight_layer"
    const val POSITION_SOURCE = "wizard_position_source"
    const val POSITION_HALO_LAYER = "wizard_position_halo_layer"
    const val POSITION_LAYER = "wizard_position_layer"

    const val WAYPOINT_RADIUS = 10f
    const val WAYPOINT_STROKE_WIDTH = 3f
    const val SELECTED_WP_RADIUS = 14f
    const val ROUTE_LINE_WIDTH = 5f
    const val STRAIGHT_LINE_WIDTH = 3f
    const val POSITION_DOT_RADIUS = 8f
    const val POSITION_HALO_RADIUS = 14f
    const val POSITION_STROKE_WIDTH = 2.5f
    const val BOUNDS_PADDING = 80
    const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
}

private class WizardMapState {
    var mapRef: MapLibreMap? = null
    var styleReady: Boolean = false
    var lastWaypointCount: Int = -1
    var lastRoutePointCount: Int = -1
    var lastPosition: GeoCoordinate? = null
    var lastSelectedIndex: Int? = null
    var clickListenerSet: Boolean = false
    var longClickListenerSet: Boolean = false
    var initialCameraDone: Boolean = false
}

@Composable
fun RouteWizardMapView(
    waypoints: List<GeoCoordinate>,
    calculatedRoute: Route?,
    currentPosition: GeoCoordinate?,
    selectedWaypointIndex: Int?,
    onMapTap: (GeoCoordinate) -> Unit,
    onInsertWaypoint: (index: Int, coord: GeoCoordinate) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = rememberWizardMapView(context, lifecycleOwner)
    val state = remember { WizardMapState() }

    Box(modifier = modifier) {
        if (mapView != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { mapView },
                update = {
                    val map = state.mapRef
                    if (map != null && state.styleReady) {
                        updateWizardMap(state, map, waypoints, calculatedRoute, currentPosition, selectedWaypointIndex)
                    } else {
                        mapView.getMapAsync { newMap ->
                            state.mapRef = newMap

                            if (!state.clickListenerSet) {
                                newMap.addOnMapClickListener { latLng ->
                                    onMapTap(GeoCoordinate(latLng.latitude, latLng.longitude))
                                    true
                                }
                                state.clickListenerSet = true
                            }

                            if (!state.longClickListenerSet) {
                                newMap.addOnMapLongClickListener { latLng ->
                                    val route = calculatedRoute
                                    if (route != null && route.points.size >= 2) {
                                        val screenPt = newMap.projection.toScreenLocation(latLng)
                                        val insertIndex = findInsertionIndex(
                                            newMap, route.points, waypoints,
                                            screenPt.x, screenPt.y,
                                            WizardMapConfig.ROUTE_TAP_TOLERANCE_PX
                                        )
                                        if (insertIndex != null) {
                                            onInsertWaypoint(
                                                insertIndex,
                                                GeoCoordinate(latLng.latitude, latLng.longitude)
                                            )
                                            return@addOnMapLongClickListener true
                                        }
                                    }
                                    onInsertWaypoint(waypoints.size, GeoCoordinate(latLng.latitude, latLng.longitude))
                                    true
                                }
                                state.longClickListenerSet = true
                            }

                            if (!state.styleReady) {
                                newMap.setStyle(WizardMapConfig.STYLE_URL) {
                                    state.styleReady = true
                                    ensureWizardLayers(newMap)
                                    updateWizardMap(
                                        state,
                                        newMap,
                                        waypoints,
                                        calculatedRoute,
                                        currentPosition,
                                        selectedWaypointIndex
                                    )
                                }
                            }
                        }
                    }
                }
            )
        } else {
            Text(
                text = "Карта недоступна",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(LoponDimens.spacerMedium),
                style = LoponTypography.caption,
                color = LoponColors.onSurfaceSecondary
            )
        }

        if (waypoints.isEmpty()) {
            Text(
                text = "Нажмите на карту или найдите место через поиск",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = LoponDimens.spacerLarge)
                    .padding(horizontal = LoponDimens.screenPadding),
                style = LoponTypography.caption,
                color = LoponColors.onSurfaceSecondary
            )
        }
    }
}

private fun findInsertionIndex(
    map: MapLibreMap,
    routePoints: List<GeoCoordinate>,
    waypoints: List<GeoCoordinate>,
    tapX: Float,
    tapY: Float,
    tolerancePx: Float
): Int? {
    if (routePoints.size < 2 || waypoints.size < 2) return null

    var minDist = Float.MAX_VALUE
    var bestSegmentIdx = -1

    // Check each route segment
    for (i in 0 until routePoints.size - 1) {
        val p1 = map.projection.toScreenLocation(LatLng(routePoints[i].latitude, routePoints[i].longitude))
        val p2 = map.projection.toScreenLocation(LatLng(routePoints[i + 1].latitude, routePoints[i + 1].longitude))

        val dist = pointToSegmentDistance(tapX, tapY, p1.x, p1.y, p2.x, p2.y)
        if (dist < minDist) {
            minDist = dist
            bestSegmentIdx = i
        }
    }

    if (minDist > tolerancePx) return null

    val closestRoutePoint = routePoints[bestSegmentIdx]
    var bestWpIdx = waypoints.size - 1

    for (i in 0 until waypoints.size - 1) {
        val wpLat = (waypoints[i].latitude + waypoints[i + 1].latitude) / 2
        val wpLon = (waypoints[i].longitude + waypoints[i + 1].longitude) / 2
        val midDist = haversine(closestRoutePoint.latitude, closestRoutePoint.longitude, wpLat, wpLon)

        val segDist = haversine(
            waypoints[i].latitude, waypoints[i].longitude,
            waypoints[i + 1].latitude, waypoints[i + 1].longitude
        )

        if (midDist < segDist) {
            bestWpIdx = i + 1
            break
        }
    }

    return bestWpIdx
}

private fun pointToSegmentDistance(
    px: Float, py: Float,
    x1: Float, y1: Float,
    x2: Float, y2: Float
): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    val lenSq = dx * dx + dy * dy

    if (lenSq == 0f) return sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1))

    val t = ((px - x1) * dx + (py - y1) * dy) / lenSq
    val clamped = t.coerceIn(0f, 1f)
    val projX = x1 + clamped * dx
    val projY = y1 + clamped * dy

    return sqrt((px - projX) * (px - projX) + (py - projY) * (py - projY))
}

private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return r * c
}

@Composable
private fun rememberWizardMapView(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
): MapView? {
    val mapView = remember {
        runCatching {
            MapView(context).apply { onCreate(null) }
        }.onFailure { e ->
            Log.e(WizardMapConfig.LOG_TAG, "Failed to init MapView", e)
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

private fun ensureWizardLayers(map: MapLibreMap) {
    val style: Style = map.style ?: return

    if (style.getSource(WizardMapConfig.POSITION_SOURCE) == null) {
        style.addSource(GeoJsonSource(WizardMapConfig.POSITION_SOURCE, FeatureCollection.fromFeatures(emptyArray())))
    }
    if (style.getLayer(WizardMapConfig.POSITION_HALO_LAYER) == null) {
        style.addLayer(
            CircleLayer(WizardMapConfig.POSITION_HALO_LAYER, WizardMapConfig.POSITION_SOURCE).withProperties(
                circleColor("#2196F3"),
                circleRadius(WizardMapConfig.POSITION_HALO_RADIUS),
                circleStrokeWidth(0f)
            )
        )
    }
    if (style.getLayer(WizardMapConfig.POSITION_LAYER) == null) {
        style.addLayer(
            CircleLayer(WizardMapConfig.POSITION_LAYER, WizardMapConfig.POSITION_SOURCE).withProperties(
                circleColor("#2196F3"),
                circleRadius(WizardMapConfig.POSITION_DOT_RADIUS),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(WizardMapConfig.POSITION_STROKE_WIDTH)
            )
        )
    }

    if (style.getSource(WizardMapConfig.STRAIGHT_LINE_SOURCE) == null) {
        style.addSource(
            GeoJsonSource(
                WizardMapConfig.STRAIGHT_LINE_SOURCE,
                FeatureCollection.fromFeatures(emptyArray())
            )
        )
    }
    if (style.getLayer(WizardMapConfig.STRAIGHT_LINE_LAYER) == null) {
        style.addLayer(
            LineLayer(WizardMapConfig.STRAIGHT_LINE_LAYER, WizardMapConfig.STRAIGHT_LINE_SOURCE).withProperties(
                lineColor("#888888"),
                lineWidth(WizardMapConfig.STRAIGHT_LINE_WIDTH),
                lineCap("round"),
                lineJoin("round"),
                lineDasharray(arrayOf(3f, 3f))
            )
        )
    }

    if (style.getSource(WizardMapConfig.ROUTE_SOURCE) == null) {
        style.addSource(GeoJsonSource(WizardMapConfig.ROUTE_SOURCE, FeatureCollection.fromFeatures(emptyArray())))
    }
    if (style.getLayer(WizardMapConfig.ROUTE_LAYER) == null) {
        style.addLayer(
            LineLayer(WizardMapConfig.ROUTE_LAYER, WizardMapConfig.ROUTE_SOURCE).withProperties(
                lineColor("#E65100"),
                lineWidth(WizardMapConfig.ROUTE_LINE_WIDTH),
                lineCap("round"),
                lineJoin("round")
            )
        )
    }

    if (style.getSource(WizardMapConfig.WAYPOINTS_SOURCE) == null) {
        style.addSource(GeoJsonSource(WizardMapConfig.WAYPOINTS_SOURCE, FeatureCollection.fromFeatures(emptyArray())))
    }
    if (style.getLayer(WizardMapConfig.WAYPOINTS_LAYER) == null) {
        style.addLayer(
            CircleLayer(WizardMapConfig.WAYPOINTS_LAYER, WizardMapConfig.WAYPOINTS_SOURCE).withProperties(
                circleColor("#FFD400"),
                circleRadius(WizardMapConfig.WAYPOINT_RADIUS),
                circleStrokeColor("#111111"),
                circleStrokeWidth(WizardMapConfig.WAYPOINT_STROKE_WIDTH)
            )
        )
    }

    if (style.getSource(WizardMapConfig.SELECTED_WP_SOURCE) == null) {
        style.addSource(GeoJsonSource(WizardMapConfig.SELECTED_WP_SOURCE, FeatureCollection.fromFeatures(emptyArray())))
    }
    if (style.getLayer(WizardMapConfig.SELECTED_WP_LAYER) == null) {
        style.addLayer(
            CircleLayer(WizardMapConfig.SELECTED_WP_LAYER, WizardMapConfig.SELECTED_WP_SOURCE).withProperties(
                circleColor("#FF5722"), // Orange for selected
                circleRadius(WizardMapConfig.SELECTED_WP_RADIUS),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(4f)
            )
        )
    }
}

private fun updateWizardMap(
    state: WizardMapState,
    map: MapLibreMap,
    waypoints: List<GeoCoordinate>,
    calculatedRoute: Route?,
    currentPosition: GeoCoordinate?,
    selectedWaypointIndex: Int?
) {
    val style = map.style ?: return
    ensureWizardLayers(map)

    val waypointCountChanged = state.lastWaypointCount != waypoints.size
    val routePointCount = calculatedRoute?.points?.size ?: 0
    val routeChanged = state.lastRoutePointCount != routePointCount
    val positionChanged = state.lastPosition != currentPosition
    val selectedChanged = state.lastSelectedIndex != selectedWaypointIndex

    if (positionChanged && currentPosition != null) {
        val posSource = style.getSourceAs<GeoJsonSource>(WizardMapConfig.POSITION_SOURCE)
        posSource?.setGeoJson(
            Feature.fromGeometry(
                Point.fromLngLat(
                    currentPosition.longitude,
                    currentPosition.latitude
                )
            )
        )
        state.lastPosition = currentPosition

        if (!state.initialCameraDone && waypoints.isEmpty()) {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(currentPosition.latitude, currentPosition.longitude),
                    WizardMapConfig.DEFAULT_ZOOM
                )
            )
            state.initialCameraDone = true
        }
    }

    if (selectedChanged || waypointCountChanged) {
        val selectedSource = style.getSourceAs<GeoJsonSource>(WizardMapConfig.SELECTED_WP_SOURCE)
        if (selectedSource != null) {
            if (selectedWaypointIndex != null && selectedWaypointIndex < waypoints.size) {
                val wp = waypoints[selectedWaypointIndex]
                selectedSource.setGeoJson(Feature.fromGeometry(Point.fromLngLat(wp.longitude, wp.latitude)))
            } else {
                selectedSource.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
            }
        }
        state.lastSelectedIndex = selectedWaypointIndex
    }

    if (waypointCountChanged) {
        val waypointsSource = style.getSourceAs<GeoJsonSource>(WizardMapConfig.WAYPOINTS_SOURCE)
        if (waypointsSource != null) {
            if (waypoints.isNotEmpty()) {
                val features = waypoints.map { coord ->
                    Feature.fromGeometry(Point.fromLngLat(coord.longitude, coord.latitude))
                }
                waypointsSource.setGeoJson(FeatureCollection.fromFeatures(features))
            } else {
                waypointsSource.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
            }
        }

        val straightSource = style.getSourceAs<GeoJsonSource>(WizardMapConfig.STRAIGHT_LINE_SOURCE)
        if (straightSource != null) {
            if (waypoints.size >= 2 && calculatedRoute == null) {
                val linePoints = waypoints.map { Point.fromLngLat(it.longitude, it.latitude) }
                straightSource.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(linePoints)))
            } else {
                straightSource.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
            }
        }

        state.lastWaypointCount = waypoints.size

        if (waypoints.size == 1) {
            val first = waypoints.first()
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(first.latitude, first.longitude),
                    WizardMapConfig.DEFAULT_ZOOM
                )
            )
            state.initialCameraDone = true
        } else if (waypoints.size >= 2) {
            val boundsBuilder = LatLngBounds.Builder()
            waypoints.forEach { boundsBuilder.include(LatLng(it.latitude, it.longitude)) }
            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                    boundsBuilder.build(),
                    WizardMapConfig.BOUNDS_PADDING
                )
            )
            state.initialCameraDone = true
        }
    }

    if (routeChanged) {
        val routeSource = style.getSourceAs<GeoJsonSource>(WizardMapConfig.ROUTE_SOURCE)
        if (routeSource != null) {
            val routePoints = calculatedRoute?.points.orEmpty()
            if (routePoints.size >= 2) {
                val linePoints = routePoints.map { Point.fromLngLat(it.longitude, it.latitude) }
                routeSource.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(linePoints)))

                val straightSource = style.getSourceAs<GeoJsonSource>(WizardMapConfig.STRAIGHT_LINE_SOURCE)
                straightSource?.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))

                val boundsBuilder = LatLngBounds.Builder()
                routePoints.forEach { boundsBuilder.include(LatLng(it.latitude, it.longitude)) }
                map.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        boundsBuilder.build(),
                        WizardMapConfig.BOUNDS_PADDING
                    )
                )
            } else {
                routeSource.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
            }
        }
        state.lastRoutePointCount = routePointCount
    }
}
