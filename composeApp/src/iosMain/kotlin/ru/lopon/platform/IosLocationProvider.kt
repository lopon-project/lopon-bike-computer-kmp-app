package ru.lopon.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreLocation.CLActivityTypeFitness
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyBestForNavigation
import platform.Foundation.NSError
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class IosLocationProvider : LocationProvider {

    private val _status = MutableStateFlow(LocationStatus.DISABLED)
    override val status: StateFlow<LocationStatus> = _status.asStateFlow()

    private val _locations = MutableSharedFlow<LocationData>(extraBufferCapacity = 64)

    private val delegate = LocationDelegate()

    private val manager: CLLocationManager = CLLocationManager().apply {
        desiredAccuracy = kCLLocationAccuracyBestForNavigation
        distanceFilter = 5.0
        activityType = CLActivityTypeFitness
        pausesLocationUpdatesAutomatically = false
        delegate = this@IosLocationProvider.delegate
    }

    private var isActive: Boolean = false

    override fun observeLocations(): Flow<LocationData> = _locations.asSharedFlow()

    override suspend fun start() {
        if (isActive) return
        isActive = true

        try {
            manager.allowsBackgroundLocationUpdates = true
        } catch (_: Throwable) {
        }

        _status.value = LocationStatus.SEARCHING
        manager.startUpdatingLocation()
    }

    override suspend fun stop() {
        if (!isActive) return
        isActive = false
        manager.stopUpdatingLocation()
        try {
            manager.allowsBackgroundLocationUpdates = false
        } catch (_: Throwable) {
        }
        _status.value = LocationStatus.DISABLED
    }

    private inner class LocationDelegate : NSObject(), CLLocationManagerDelegateProtocol {

        override fun locationManager(
            manager: CLLocationManager,
            didUpdateLocations: List<*>
        ) {
            val locations = didUpdateLocations.filterIsInstance<CLLocation>()
            for (location in locations) {
                _status.value = LocationStatus.ACTIVE
                _locations.tryEmit(toLocationData(location))
            }
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            _status.value = LocationStatus.ERROR
        }

        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            val status = manager.authorizationStatus
            _status.value = mapAuthToStatus(status)
        }

        override fun locationManager(
            manager: CLLocationManager,
            didChangeAuthorizationStatus: CLAuthorizationStatus
        ) {
            _status.value = mapAuthToStatus(didChangeAuthorizationStatus)
        }
    }

    private fun toLocationData(location: CLLocation): LocationData {
        val coord = location.coordinate.useContents { Pair(latitude, longitude) }
        val timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds()
        return LocationData(
            latitude = coord.first,
            longitude = coord.second,
            accuracy = if (location.horizontalAccuracy >= 0) {
                location.horizontalAccuracy.toFloat()
            } else null,
            altitude = if (location.verticalAccuracy >= 0) location.altitude else null,
            speed = if (location.speed >= 0) location.speed.toFloat() else null,
            bearing = if (location.course >= 0) location.course.toFloat() else null,
            timestampUtc = timestamp
        )
    }

    private fun mapAuthToStatus(status: CLAuthorizationStatus): LocationStatus = when (status) {
        kCLAuthorizationStatusAuthorizedAlways,
        kCLAuthorizationStatusAuthorizedWhenInUse ->
            if (isActive) LocationStatus.SEARCHING else LocationStatus.DISABLED

        kCLAuthorizationStatusDenied,
        kCLAuthorizationStatusRestricted -> LocationStatus.ERROR

        kCLAuthorizationStatusNotDetermined -> LocationStatus.DISABLED
        else -> LocationStatus.DISABLED
    }
}
