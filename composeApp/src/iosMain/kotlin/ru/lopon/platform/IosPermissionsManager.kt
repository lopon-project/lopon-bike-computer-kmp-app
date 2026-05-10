package ru.lopon.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBManagerAuthorization
import platform.CoreBluetooth.CBManagerAuthorizationAllowedAlways
import platform.CoreBluetooth.CBManagerAuthorizationDenied
import platform.CoreBluetooth.CBManagerAuthorizationNotDetermined
import platform.CoreBluetooth.CBManagerAuthorizationRestricted
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.Foundation.NSURL
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNNotificationSettings
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class IosPermissionsManager : PermissionsManager {

    private val prefs = IosPrefs()
    private val locationDelegate = AuthDelegate()
    private val locationManager: CLLocationManager = CLLocationManager().apply {
        delegate = locationDelegate
    }

    private var bluetoothProbe: CBCentralManager? = null
    private val bluetoothProbeDelegate = BluetoothProbeDelegate()

    private var pendingLocationRequest: CompletableDeferred<PermissionState>? = null
    private var pendingBluetoothRequest: CompletableDeferred<PermissionState>? = null

    override fun checkPermission(permission: AppPermission): PermissionState = when (permission) {
        AppPermission.LOCATION -> mapLocationStatus(locationManager.authorizationStatus)
        AppPermission.BLUETOOTH -> mapBluetoothAuthorization(CBCentralManager.authorization)
        AppPermission.FILE_ACCESS -> PermissionState.GRANTED
        AppPermission.NOTIFICATION -> PermissionState.DENIED
    }

    override suspend fun requestPermission(permission: AppPermission): PermissionState {
        return when (permission) {
            AppPermission.LOCATION -> requestLocation()
            AppPermission.BLUETOOTH -> requestBluetooth()
            AppPermission.FILE_ACCESS -> PermissionState.GRANTED
            AppPermission.NOTIFICATION -> requestNotifications()
        }
    }

    override fun isBluetoothEnabled(): Boolean {
        val probe = bluetoothProbe ?: CBCentralManager(bluetoothProbeDelegate, queue = null).also {
            bluetoothProbe = it
        }
        return probe.state == CBManagerStatePoweredOn
    }

    fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        val app = UIApplication.sharedApplication
        if (app.canOpenURL(url)) {
            app.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        }
    }

    private suspend fun requestLocation(): PermissionState {
        val current = locationManager.authorizationStatus
        if (current == kCLAuthorizationStatusAuthorizedWhenInUse ||
            current == kCLAuthorizationStatusAuthorizedAlways
        ) {
            return PermissionState.GRANTED
        }

        if (current == kCLAuthorizationStatusDenied || current == kCLAuthorizationStatusRestricted) {
            return PermissionState.DENIED_FOREVER
        }

        val deferred = CompletableDeferred<PermissionState>()
        pendingLocationRequest = deferred
        prefs.setBoolean(IosPrefs.permissionRequestedKey("location"), true)

        locationManager.requestWhenInUseAuthorization()

        return deferred.await()
    }

    private suspend fun requestBluetooth(): PermissionState {
        val auth = CBCentralManager.authorization
        if (auth == CBManagerAuthorizationAllowedAlways) return PermissionState.GRANTED
        if (auth == CBManagerAuthorizationDenied || auth == CBManagerAuthorizationRestricted) {
            return PermissionState.DENIED_FOREVER
        }

        val deferred = CompletableDeferred<PermissionState>()
        pendingBluetoothRequest = deferred
        prefs.setBoolean(IosPrefs.permissionRequestedKey("bluetooth"), true)

        if (bluetoothProbe == null) {
            bluetoothProbe = CBCentralManager(bluetoothProbeDelegate, queue = null)
        }

        return deferred.await()
    }

    private suspend fun requestNotifications(): PermissionState {
        val deferred = CompletableDeferred<PermissionState>()
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or
                UNAuthorizationOptionSound

        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            options
        ) { granted, _ ->
            deferred.complete(if (granted) PermissionState.GRANTED else PermissionState.DENIED)
        }
        return deferred.await()
    }

    private inner class AuthDelegate : NSObject(), CLLocationManagerDelegateProtocol {

        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            val status = manager.authorizationStatus
            pendingLocationRequest?.takeIf { !it.isCompleted }
                ?.complete(mapLocationStatus(status))
            pendingLocationRequest = null
        }

        override fun locationManager(
            manager: CLLocationManager,
            didChangeAuthorizationStatus: CLAuthorizationStatus
        ) {
            pendingLocationRequest?.takeIf { !it.isCompleted }
                ?.complete(mapLocationStatus(didChangeAuthorizationStatus))
            pendingLocationRequest = null
        }
    }

    private inner class BluetoothProbeDelegate : NSObject(), CBCentralManagerDelegateProtocol {
        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            val auth = CBCentralManager.authorization
            val mapped = mapBluetoothAuthorization(auth)
            if (mapped != PermissionState.DENIED || auth != CBManagerAuthorizationNotDetermined) {
                pendingBluetoothRequest?.takeIf { !it.isCompleted }?.complete(mapped)
                pendingBluetoothRequest = null
            }
        }
    }

    private fun mapLocationStatus(status: CLAuthorizationStatus): PermissionState = when (status) {
        kCLAuthorizationStatusAuthorizedAlways,
        kCLAuthorizationStatusAuthorizedWhenInUse -> PermissionState.GRANTED
        kCLAuthorizationStatusDenied,
        kCLAuthorizationStatusRestricted -> PermissionState.DENIED_FOREVER
        kCLAuthorizationStatusNotDetermined -> PermissionState.DENIED
        else -> PermissionState.DENIED
    }

    private fun mapBluetoothAuthorization(auth: CBManagerAuthorization): PermissionState =
        when (auth) {
            CBManagerAuthorizationAllowedAlways -> PermissionState.GRANTED
            CBManagerAuthorizationDenied,
            CBManagerAuthorizationRestricted -> PermissionState.DENIED_FOREVER
            CBManagerAuthorizationNotDetermined -> PermissionState.DENIED
            else -> PermissionState.DENIED
        }
}
