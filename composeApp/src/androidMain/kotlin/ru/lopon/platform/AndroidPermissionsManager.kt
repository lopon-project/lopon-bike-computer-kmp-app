package ru.lopon.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference
import androidx.core.content.edit

class AndroidPermissionsManager(
    private val context: Context
) : PermissionsManager {

    private var activityRef: WeakReference<Activity>? = null
    private val permissionResultChannel = Channel<Map<String, Boolean>>(Channel.CONFLATED)

    companion object {
        const val PERMISSION_REQUEST_CODE = 1001

        private const val PERMISSION_TIMEOUT_MS = 30_000L
    }
    fun registerActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun unregisterActivity() {
        activityRef = null
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != PERMISSION_REQUEST_CODE) return

        val results = permissions.zip(grantResults.toTypedArray())
            .associate { (permission, result) ->
                permission to (result == PackageManager.PERMISSION_GRANTED)
            }

        permissionResultChannel.trySend(results)
    }

    override fun checkPermission(permission: AppPermission): PermissionState {
        val androidPermissions = mapToAndroidPermissions(permission)

        if (androidPermissions.isEmpty()) {
            return PermissionState.GRANTED
        }

        val allGranted = androidPermissions.all { androidPerm ->
            ContextCompat.checkSelfPermission(context, androidPerm) == PackageManager.PERMISSION_GRANTED
        }

        return if (allGranted) PermissionState.GRANTED else PermissionState.DENIED
    }

    override suspend fun requestPermission(permission: AppPermission): PermissionState {
        if (checkPermission(permission) == PermissionState.GRANTED) {
            return PermissionState.GRANTED
        }

        val activity = activityRef?.get()
            ?: return PermissionState.DENIED

        val androidPermissions = mapToAndroidPermissions(permission)

        if (androidPermissions.isEmpty()) {
            return PermissionState.GRANTED
        }

        val anyDeniedForever = androidPermissions.any { androidPerm ->
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, androidPerm) &&
            ContextCompat.checkSelfPermission(context, androidPerm) == PackageManager.PERMISSION_DENIED &&
            wasPermissionPreviouslyRequested(androidPerm)
        }

        if (anyDeniedForever) {
            return PermissionState.DENIED_FOREVER
        }

        ActivityCompat.requestPermissions(
            activity,
            androidPermissions.toTypedArray(),
            PERMISSION_REQUEST_CODE
        )

        androidPermissions.forEach { markPermissionRequested(it) }

        val result = withTimeoutOrNull(PERMISSION_TIMEOUT_MS) {
            permissionResultChannel.receive()
        }

        if (result == null) {
            return PermissionState.DENIED
        }

        val allGranted = androidPermissions.all { result[it] == true }

        return when {
            allGranted -> PermissionState.GRANTED
            androidPermissions.any { perm ->
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
            } -> PermissionState.DENIED_FOREVER
            else -> PermissionState.DENIED
        }
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }


    fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun isBluetoothEnabled(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        return bluetoothManager?.adapter?.isEnabled == true
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        return locationManager?.let {
            it.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
            it.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        } ?: false
    }


    private fun mapToAndroidPermissions(permission: AppPermission): List<String> {
        return when (permission) {
            AppPermission.BLUETOOTH -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    listOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                } else {
                    listOf(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            AppPermission.LOCATION -> {
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
            AppPermission.FILE_ACCESS -> {
                emptyList()
            }
            AppPermission.NOTIFICATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    emptyList()
                }
            }
        }
    }

    private val requestedPermissionsKey = "lopon_requested_permissions"

    private fun wasPermissionPreviouslyRequested(permission: String): Boolean {
        val prefs = context.getSharedPreferences(requestedPermissionsKey, Context.MODE_PRIVATE)
        return prefs.getBoolean(permission, false)
    }

    private fun markPermissionRequested(permission: String) {
        val prefs = context.getSharedPreferences(requestedPermissionsKey, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(permission, true) }
    }
}
