package ru.lopon.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import ru.lopon.domain.model.SensorReading
import ru.lopon.platform.BleConnectionState
import ru.lopon.platform.BleDevice

class BleServiceController(
    private val context: Context
) {
    private var service: LoponSensorService? = null
    private var isBound = false

    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: StateFlow<Boolean> = _isServiceBound.asStateFlow()

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? LoponSensorService.LocalBinder
            service = localBinder?.service
            isBound = true
            _isServiceBound.value = true

            service?.observeConnectionState()?.let { flow ->
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
            _isServiceBound.value = false
            _connectionState.value = BleConnectionState.Disconnected
        }
    }

    fun startAndBind(autoConnectDeviceId: String? = null) {
        LoponSensorService.start(context, autoConnectDeviceId)
        bindService()
    }

    fun bindService() {
        if (isBound) return

        val intent = Intent(context, LoponSensorService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService() {
        if (!isBound) return

        try {
            context.unbindService(serviceConnection)
        } catch (e: Exception) {
            // Already unbound
        }

        isBound = false
        _isServiceBound.value = false
        service = null
    }

    fun stopService() {
        unbindService()
        LoponSensorService.stop(context)
    }

    fun connectToDevice(deviceId: String) {
        service?.connectToDevice(deviceId)
    }

    fun startRecording() {
        service?.startRecording()
    }

    fun stopRecording() {
        service?.stopRecording()
    }

    fun observeSensorData(): Flow<SensorReading> {
        return service?.observeSensorData() ?: emptyFlow()
    }

    fun observeConnectionState(): StateFlow<BleConnectionState>? {
        return service?.observeConnectionState()
    }

    fun getConnectedDevice(): BleDevice? {
        return service?.getConnectedDevice()
    }

    fun observeServiceState(): StateFlow<ServiceState>? {
        return service?.serviceState
    }

    fun getBleAdapter() = service?.getBleAdapter()
}
