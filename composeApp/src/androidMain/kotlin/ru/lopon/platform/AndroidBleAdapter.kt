package ru.lopon.platform

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import ru.lopon.domain.model.SensorReading
import java.util.UUID
import kotlin.coroutines.resume
import androidx.core.content.edit


@SuppressLint("MissingPermission")
class AndroidBleAdapter(
    private val context: Context
) : BleAdapter {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val gattMutex = Mutex()

    private var gatt: BluetoothGatt? = null
    private var cscMeasurementCharacteristic: BluetoothGattCharacteristic? = null
    private var configWriteCharacteristic: BluetoothGattCharacteristic? = null
    private var configResponseCharacteristic: BluetoothGattCharacteristic? = null


    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BleDevice?>(null)
    override val connectedDevice: StateFlow<BleDevice?> = _connectedDevice.asStateFlow()

    private val _wheelDataFlow = MutableSharedFlow<SensorReading>(extraBufferCapacity = 64)
    private val _configResponseFlow = MutableSharedFlow<ConfigResponse>(extraBufferCapacity = 8)

    private val prefs = context.getSharedPreferences("lopon_ble_prefs", Context.MODE_PRIVATE)
    private val lastDeviceKey = "last_connected_device_id"


    private val cscServiceUuid = UUID.fromString(LoponSensorProtocol.CSC_SERVICE_UUID)
    private val cscMeasurementUuid = UUID.fromString(LoponSensorProtocol.CSC_MEASUREMENT_UUID)
    private val configWriteUuid = UUID.fromString(LoponSensorProtocol.CONFIG_WRITE_UUID)
    private val configResponseUuid = UUID.fromString(LoponSensorProtocol.CONFIG_RESPONSE_UUID)

    private val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    override fun observeWheelData(): Flow<SensorReading> = _wheelDataFlow.asSharedFlow()

    override fun observeConfigResponse(): Flow<ConfigResponse> = _configResponseFlow.asSharedFlow()

    override suspend fun scan(timeoutMs: Long): List<BleDevice> {
        val scanner = this.scanner ?: return emptyList()

        return callbackFlow {
            val devices = mutableMapOf<String, BleDevice>()
            val lastConnectedId = getLastConnectedDeviceId()

            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val device = result.device
                    val name = device.name ?: result.scanRecord?.deviceName

                    if (name != null && LoponSensorProtocol.isLoponDevice(name)) {
                        val bleDevice = BleDevice(
                            id = device.address,
                            name = name,
                            rssi = result.rssi,
                            isPreviouslyConnected = device.address == lastConnectedId
                        )
                        devices[device.address] = bleDevice
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    close(Exception("Scan failed with error code: $errorCode"))
                }
            }

            val filters = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(cscServiceUuid))
                    .build()
            )

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            scanner.startScan(filters, settings, callback)

            delay(timeoutMs)

            scanner.stopScan(callback)

            val sortedDevices = devices.values.sortedWith(
                compareByDescending<BleDevice> { it.isPreviouslyConnected }
                    .thenByDescending { it.rssi }
            )

            send(sortedDevices)
            close()

            awaitClose {
                try {
                    scanner.stopScan(callback)
                } catch (e: Exception) {
                }
            }
        }.let { flow ->
            var result: List<BleDevice> = emptyList()
            flow.collect { result = it }
            result
        }
    }

    override suspend fun connect(deviceId: String): Result<Unit> = gattMutex.withLock {
        gatt?.let { existingGatt ->
            existingGatt.disconnect()
            existingGatt.close()
            gatt = null
        }

        val adapter = bluetoothAdapter ?: return Result.failure(
            Exception("Bluetooth not available")
        )

        if (!adapter.isEnabled) {
            return Result.failure(Exception("Bluetooth is disabled"))
        }

        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(deviceId)
        } catch (e: Exception) {
            return Result.failure(Exception("Invalid device address: $deviceId"))
        }

        _connectionState.value = BleConnectionState.Connecting

        return suspendCancellableCoroutine { continuation ->
            val gattCallback = createGattCallback(
                onConnected = { newGatt ->
                    gatt = newGatt
                    _connectedDevice.value = BleDevice(
                        id = deviceId,
                        name = device.name ?: LoponSensorProtocol.DEVICE_NAME,
                        rssi = 0,
                        isPreviouslyConnected = true
                    )
                    saveLastConnectedDeviceId(deviceId)
                    _connectionState.value = BleConnectionState.Connected

                    if (continuation.isActive) {
                        continuation.resume(Result.success(Unit))
                    }
                },
                onDisconnected = {
                    _connectionState.value = BleConnectionState.Disconnected
                    _connectedDevice.value = null
                    clearCharacteristics()

                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception("Disconnected during connection")))
                    }
                },
                onError = { error ->
                    _connectionState.value = BleConnectionState.Error(error)

                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception(error)))
                    }
                }
            )

            val newGatt =
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)

            continuation.invokeOnCancellation {
                newGatt?.disconnect()
                newGatt?.close()
            }
        }
    }

    override suspend fun disconnect() = gattMutex.withLock {
        try {
            if (_connectionState.value == BleConnectionState.Connected) {
                stopSensorInternal()
            }
        } catch (e: Exception) {
        }

        gatt?.disconnect()
        gatt?.close()
        gatt = null

        clearCharacteristics()
        _connectionState.value = BleConnectionState.Disconnected
        _connectedDevice.value = null
    }

    override suspend fun startSensor(): Result<Unit> {
        return writeConfigCommand(LoponSensorProtocol.createStartCommand())
    }

    override suspend fun stopSensor(): Result<Unit> {
        return writeConfigCommand(LoponSensorProtocol.createStopCommand())
    }

    private suspend fun stopSensorInternal() {
        try {
            writeConfigCommand(LoponSensorProtocol.createStopCommand())
        } catch (e: Exception) {
        }
    }

    override suspend fun setPulsesPerRevolution(ppr: Int): Result<Unit> {
        return writeConfigCommand(LoponSensorProtocol.createSetPprCommand(ppr))
    }


    fun release() {
        scope.cancel()
        scope.launch {
            disconnect()
        }
    }

    private fun createGattCallback(
        onConnected: (BluetoothGatt) -> Unit,
        onDisconnected: () -> Unit,
        onError: (String) -> Unit
    ): BluetoothGattCallback {
        return object : BluetoothGattCallback() {

            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when {
                    status != BluetoothGatt.GATT_SUCCESS -> {
                        onError("Connection failed with status: $status")
                        gatt.close()
                    }
                    newState == BluetoothProfile.STATE_CONNECTED -> {
                        gatt.discoverServices()
                    }
                    newState == BluetoothProfile.STATE_DISCONNECTED -> {
                        onDisconnected()
                        gatt.close()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    onError("Service discovery failed: $status")
                    return
                }

                val cscService = gatt.getService(cscServiceUuid)
                if (cscService == null) {
                    onError("CSC Service not found")
                    return
                }

                cscMeasurementCharacteristic = cscService.getCharacteristic(cscMeasurementUuid)
                configWriteCharacteristic = cscService.getCharacteristic(configWriteUuid)
                configResponseCharacteristic = cscService.getCharacteristic(configResponseUuid)

                if (cscMeasurementCharacteristic == null) {
                    onError("CSC Measurement characteristic not found")
                    return
                }

                scope.launch {
                    enableNotifications(gatt)
                    onConnected(gatt)
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                handleCharacteristicChanged(characteristic.uuid, value)
            }

            @Deprecated("Deprecated in API 33")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                @Suppress("DEPRECATION")
                val value = characteristic.value ?: return
                handleCharacteristicChanged(characteristic.uuid, value)
            }
        }
    }

    private fun handleCharacteristicChanged(uuid: UUID, value: ByteArray) {
        when (uuid) {
            cscMeasurementUuid -> {
                val reading = LoponSensorProtocol.parseCscMeasurement(
                    data = value,
                    timestampUtc = System.currentTimeMillis()
                )
                reading?.let { _wheelDataFlow.tryEmit(it) }
            }
            configResponseUuid -> {
                val response = LoponSensorProtocol.parseConfigResponse(value)
                _configResponseFlow.tryEmit(response)
            }
        }
    }

    private suspend fun enableNotifications(gatt: BluetoothGatt) {
        cscMeasurementCharacteristic?.let { characteristic ->
            enableNotificationForCharacteristic(gatt, characteristic)
        }

        configResponseCharacteristic?.let { characteristic ->
            enableNotificationForCharacteristic(gatt, characteristic)
        }
    }

    private suspend fun enableNotificationForCharacteristic(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        gatt.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic.getDescriptor(cccdUuid)
        if (descriptor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
            delay(100)
        }
    }

    private suspend fun writeConfigCommand(command: ByteArray): Result<Unit> {
        val gatt = this.gatt ?: return Result.failure(Exception("Not connected"))
        val characteristic = configWriteCharacteristic
            ?: return Result.failure(Exception("Config characteristic not available"))

        return try {
            val writeResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    characteristic,
                    command,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = command
                @Suppress("DEPRECATION")
                if (gatt.writeCharacteristic(characteristic)) {
                    BluetoothGatt.GATT_SUCCESS
                } else {
                    BluetoothGatt.GATT_FAILURE
                }
            }

            if (writeResult == BluetoothGatt.GATT_SUCCESS) {
                val response = withTimeoutOrNull(2000) {
                    var received: ConfigResponse? = null
                    _configResponseFlow.collect {
                        received = it
                        return@collect
                    }
                    received
                }

                when (response) {
                    ConfigResponse.Success -> Result.success(Unit)
                    ConfigResponse.Error -> Result.failure(Exception("Sensor returned error"))
                    else -> Result.success(Unit)
                }
            } else {
                Result.failure(Exception("Failed to write command"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun clearCharacteristics() {
        cscMeasurementCharacteristic = null
        configWriteCharacteristic = null
        configResponseCharacteristic = null
    }

    private fun getLastConnectedDeviceId(): String? {
        return prefs.getString(lastDeviceKey, null)
    }

    private fun saveLastConnectedDeviceId(deviceId: String) {
        prefs.edit { putString(lastDeviceKey, deviceId) }
    }
}
