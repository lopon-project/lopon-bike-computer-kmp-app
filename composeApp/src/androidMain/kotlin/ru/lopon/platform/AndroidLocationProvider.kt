package ru.lopon.platform

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

class AndroidLocationProvider(
    context: Context
) : LocationProvider {

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _status = MutableStateFlow(LocationStatus.DISABLED)
    override val status: StateFlow<LocationStatus> = _status.asStateFlow()

    @SuppressLint("MissingPermission")
    override fun observeLocations(): Flow<LocationData> = callbackFlow {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                _status.value = LocationStatus.ACTIVE
                trySend(
                    LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = if (location.hasAccuracy()) location.accuracy else null,
                        altitude = if (location.hasAltitude()) location.altitude else null,
                        speed = if (location.hasSpeed()) location.speed else null,
                        bearing = if (location.hasBearing()) location.bearing else null,
                        timestampUtc = location.time
                    )
                )
            }

            @Deprecated("Deprecated in API 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            }

            override fun onProviderEnabled(provider: String) {
                _status.value = LocationStatus.SEARCHING
            }

            override fun onProviderDisabled(provider: String) {
                _status.value = LocationStatus.DISABLED
            }
        }

        val gpsAvailable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkAvailable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!gpsAvailable && !networkAvailable) {
            _status.value = LocationStatus.DISABLED
        } else {
            _status.value = LocationStatus.SEARCHING
        }

        try {
            if (gpsAvailable) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    listener,
                    Looper.getMainLooper()
                )
            }
            if (networkAvailable) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    listener,
                    Looper.getMainLooper()
                )
            }

            // Emit last known location immediately if available
            val lastKnown = if (gpsAvailable) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else if (networkAvailable) {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else null

            lastKnown?.let { location ->
                trySend(
                    LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = if (location.hasAccuracy()) location.accuracy else null,
                        altitude = if (location.hasAltitude()) location.altitude else null,
                        speed = if (location.hasSpeed()) location.speed else null,
                        bearing = if (location.hasBearing()) location.bearing else null,
                        timestampUtc = location.time
                    )
                )
            }
        } catch (e: SecurityException) {
            _status.value = LocationStatus.ERROR
        }

        awaitClose {
            locationManager.removeUpdates(listener)
            _status.value = LocationStatus.DISABLED
        }
    }

    override suspend fun start() {
    }

    override suspend fun stop() {
        _status.value = LocationStatus.DISABLED
    }

    companion object {
        private const val MIN_TIME_MS = 1000L
        private const val MIN_DISTANCE_M = 3f
    }
}
