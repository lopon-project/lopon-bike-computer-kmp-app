package ru.lopon.platform

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBCharacteristicWriteWithResponse
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBPeripheralDelegateProtocol
import platform.CoreBluetooth.CBService
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.dataWithBytes
import platform.darwin.NSObject
import ru.lopon.domain.model.SensorReading

@OptIn(ExperimentalForeignApi::class)
class IosBleAdapter : BleAdapter {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val gattMutex = Mutex()
    private val prefs = IosPrefs()

    private val centralDelegate = CentralDelegate()
    private val peripheralDelegate = PeripheralDelegate()

    private val central: CBCentralManager =
        CBCentralManager(centralDelegate, queue = null, options = null)

    private val _connectionState =
        MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BleDevice?>(null)
    override val connectedDevice: StateFlow<BleDevice?> = _connectedDevice.asStateFlow()

    private val _wheelDataFlow = MutableSharedFlow<SensorReading>(extraBufferCapacity = 64)
    private val _configResponseFlow = MutableSharedFlow<ConfigResponse>(extraBufferCapacity = 8)

    private var activePeripheral: CBPeripheral? = null
    private var cscMeasurementCharacteristic: CBCharacteristic? = null
    private var configWriteCharacteristic: CBCharacteristic? = null
    private var configResponseCharacteristic: CBCharacteristic? = null

    private var connectContinuation: CompletableDeferred<Result<Unit>>? = null

    private val activeScan: MutableMap<String, BleDevice> = mutableMapOf()
    private var scanCompletion: CompletableDeferred<List<BleDevice>>? = null

    private val cscServiceUuid = CBUUID.UUIDWithString(LoponSensorProtocol.CSC_SERVICE_UUID)
    private val cscMeasurementUuid =
        CBUUID.UUIDWithString(LoponSensorProtocol.CSC_MEASUREMENT_UUID)
    private val configWriteUuid = CBUUID.UUIDWithString(LoponSensorProtocol.CONFIG_WRITE_UUID)
    private val configResponseUuid =
        CBUUID.UUIDWithString(LoponSensorProtocol.CONFIG_RESPONSE_UUID)

    override fun observeWheelData(): Flow<SensorReading> = _wheelDataFlow.asSharedFlow()

    override fun observeConfigResponse(): Flow<ConfigResponse> = _configResponseFlow.asSharedFlow()

    override suspend fun scan(timeoutMs: Long): List<BleDevice> {
        if (!isPoweredOn()) {
            withTimeoutOrNull(2000L) {
                while (!isPoweredOn()) delay(50)
            }
            if (!isPoweredOn()) return emptyList()
        }

        activeScan.clear()
        val deferred = CompletableDeferred<List<BleDevice>>()
        scanCompletion = deferred

        central.scanForPeripheralsWithServices(
            serviceUUIDs = listOf(cscServiceUuid),
            options = null
        )

        scope.launch {
            delay(timeoutMs)
            stopScan()
            val lastConnectedId = prefs.getString(IosPrefs.KEY_LAST_BLE_DEVICE_ID)
            val results = activeScan.values
                .map { it.copy(isPreviouslyConnected = it.id == lastConnectedId) }
                .sortedWith(
                    compareByDescending<BleDevice> { it.isPreviouslyConnected }
                        .thenByDescending { it.rssi }
                )
            deferred.complete(results)
        }

        return deferred.await()
    }

    private fun stopScan() {
        try {
            central.stopScan()
        } catch (_: Throwable) {
        }
    }

    override suspend fun connect(deviceId: String): Result<Unit> = gattMutex.withLock {
        activePeripheral?.let { p ->
            try {
                central.cancelPeripheralConnection(p)
            } catch (_: Throwable) {
            }
        }
        clearCharacteristics()

        val peripheral = peripheralDelegate.knownPeripherals[deviceId]
            ?: return@withLock Result.failure(Exception("Device not found in scan cache: $deviceId"))

        if (!isPoweredOn()) {
            return@withLock Result.failure(Exception("Bluetooth is not powered on"))
        }

        _connectionState.value = BleConnectionState.Connecting

        val deferred = CompletableDeferred<Result<Unit>>()
        connectContinuation = deferred

        peripheral.delegate = peripheralDelegate
        activePeripheral = peripheral

        central.connectPeripheral(peripheral, options = null)

        scope.launch {
            val finished = withTimeoutOrNull(15_000L) { deferred.await() }
            if (finished == null && !deferred.isCompleted) {
                deferred.complete(Result.failure(Exception("Connect timeout")))
                _connectionState.value = BleConnectionState.Error("Connect timeout")
            }
        }

        deferred.await()
    }

    override suspend fun disconnect() = gattMutex.withLock {
        try {
            if (_connectionState.value == BleConnectionState.Connected) {
                writeConfigCommandFast(LoponSensorProtocol.createStopCommand())
            }
        } catch (_: Throwable) {
        }

        activePeripheral?.let { p ->
            try {
                central.cancelPeripheralConnection(p)
            } catch (_: Throwable) {
            }
        }
        activePeripheral = null
        clearCharacteristics()
        _connectionState.value = BleConnectionState.Disconnected
        _connectedDevice.value = null
    }

    override suspend fun startSensor(): Result<Unit> {
        return writeConfigCommandFast(LoponSensorProtocol.createStartCommand())
    }

    override suspend fun stopSensor(): Result<Unit> {
        return writeConfigCommandFast(LoponSensorProtocol.createStopCommand())
    }

    override suspend fun setPulsesPerRevolution(ppr: Int): Result<Unit> {
        val cmd = LoponSensorProtocol.createSetPprCommand(ppr)
        val result = writeConfigCommandFast(cmd)
        if (result.isFailure) return result

        val response = withTimeoutOrNull(500L) {
            _configResponseFlow.first()
        }
        return when (response) {
            ConfigResponse.Success -> Result.success(Unit)
            ConfigResponse.Error -> Result.failure(Exception("Sensor returned error"))
            else -> Result.success(Unit)
        }
    }

    fun release() {
        scope.launch { disconnect() }
        scope.cancel()
    }

    fun getLastConnectedDeviceId(): String? = prefs.getString(IosPrefs.KEY_LAST_BLE_DEVICE_ID)

    private fun isPoweredOn(): Boolean = central.state == CBManagerStatePoweredOn

    private fun writeConfigCommandFast(command: ByteArray): Result<Unit> {
        val peripheral = activePeripheral
            ?: return Result.failure(Exception("Not connected"))
        val characteristic = configWriteCharacteristic
            ?: return Result.failure(Exception("Config characteristic not available"))

        return try {
            val data = command.toNSData()
            peripheral.writeValue(
                data,
                forCharacteristic = characteristic,
                type = CBCharacteristicWriteWithResponse
            )
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private fun clearCharacteristics() {
        cscMeasurementCharacteristic = null
        configWriteCharacteristic = null
        configResponseCharacteristic = null
    }

    private fun saveLastConnectedDeviceId(deviceId: String) {
        prefs.setString(IosPrefs.KEY_LAST_BLE_DEVICE_ID, deviceId)
    }

    private fun nowMs(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()

    private inner class CentralDelegate : NSObject(), CBCentralManagerDelegateProtocol {

        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            if (central.state != CBManagerStatePoweredOn) {
                _connectionState.value = BleConnectionState.Disconnected
                _connectedDevice.value = null
                connectContinuation?.takeIf { !it.isCompleted }
                    ?.complete(Result.failure(Exception("Bluetooth not available")))
            }
        }

        override fun centralManager(
            central: CBCentralManager,
            didDiscoverPeripheral: CBPeripheral,
            advertisementData: Map<Any?, *>,
            RSSI: NSNumber
        ) {
            val name = didDiscoverPeripheral.name
            if (name == null || !LoponSensorProtocol.isLoponDevice(name)) return

            val id = didDiscoverPeripheral.identifier.UUIDString
            peripheralDelegate.knownPeripherals[id] = didDiscoverPeripheral

            val rssi = RSSI.intValue
            activeScan[id] = BleDevice(
                id = id,
                name = name,
                rssi = rssi,
                isPreviouslyConnected = false
            )
        }

        override fun centralManager(
            central: CBCentralManager,
            didConnectPeripheral: CBPeripheral
        ) {
            didConnectPeripheral.discoverServices(listOf(cscServiceUuid))
        }

        @ObjCSignatureOverride
        override fun centralManager(
            central: CBCentralManager,
            didFailToConnectPeripheral: CBPeripheral,
            error: NSError?
        ) {
            val message = error?.localizedDescription ?: "Failed to connect"
            _connectionState.value = BleConnectionState.Error(message)
            _connectedDevice.value = null
            connectContinuation?.takeIf { !it.isCompleted }
                ?.complete(Result.failure(Exception(message)))
            activePeripheral = null
        }

        @ObjCSignatureOverride
        override fun centralManager(
            central: CBCentralManager,
            didDisconnectPeripheral: CBPeripheral,
            error: NSError?
        ) {
            _connectionState.value = BleConnectionState.Disconnected
            _connectedDevice.value = null
            clearCharacteristics()
            connectContinuation?.takeIf { !it.isCompleted }
                ?.complete(Result.failure(Exception("Disconnected during connection")))
            activePeripheral = null
        }
    }

    private inner class PeripheralDelegate : NSObject(), CBPeripheralDelegateProtocol {

        val knownPeripherals: MutableMap<String, CBPeripheral> = mutableMapOf()

        override fun peripheral(
            peripheral: CBPeripheral,
            didDiscoverServices: NSError?
        ) {
            if (didDiscoverServices != null) {
                fail("Service discovery failed: ${didDiscoverServices.localizedDescription}")
                return
            }
            val service = peripheral.services?.firstOrNull {
                (it as CBService).UUID == cscServiceUuid
            } as? CBService
            if (service == null) {
                fail("CSC service not found")
                return
            }
            peripheral.discoverCharacteristics(
                listOf(cscMeasurementUuid, configWriteUuid, configResponseUuid),
                forService = service
            )
        }

        override fun peripheral(
            peripheral: CBPeripheral,
            didDiscoverCharacteristicsForService: CBService,
            error: NSError?
        ) {
            if (error != null) {
                fail("Characteristics discovery failed: ${error.localizedDescription}")
                return
            }

            didDiscoverCharacteristicsForService.characteristics?.forEach { ch ->
                val c = ch as CBCharacteristic
                when (c.UUID) {
                    cscMeasurementUuid -> cscMeasurementCharacteristic = c
                    configWriteUuid -> configWriteCharacteristic = c
                    configResponseUuid -> configResponseCharacteristic = c
                }
            }

            if (cscMeasurementCharacteristic == null) {
                fail("CSC Measurement characteristic not found")
                return
            }

            cscMeasurementCharacteristic?.let { peripheral.setNotifyValue(true, forCharacteristic = it) }
            configResponseCharacteristic?.let { peripheral.setNotifyValue(true, forCharacteristic = it) }

            val deviceId = peripheral.identifier.UUIDString
            _connectedDevice.value = BleDevice(
                id = deviceId,
                name = peripheral.name ?: LoponSensorProtocol.DEVICE_NAME,
                rssi = 0,
                isPreviouslyConnected = true
            )
            saveLastConnectedDeviceId(deviceId)
            _connectionState.value = BleConnectionState.Connected
            connectContinuation?.takeIf { !it.isCompleted }
                ?.complete(Result.success(Unit))
        }

        @ObjCSignatureOverride
        override fun peripheral(
            peripheral: CBPeripheral,
            didUpdateValueForCharacteristic: CBCharacteristic,
            error: NSError?
        ) {
            if (error != null) return
            val data = didUpdateValueForCharacteristic.value ?: return
            val bytes = data.toByteArray()
            when (didUpdateValueForCharacteristic.UUID) {
                cscMeasurementUuid -> {
                    LoponSensorProtocol.parseCscMeasurement(bytes, nowMs())?.let {
                        _wheelDataFlow.tryEmit(it)
                    }
                }
                configResponseUuid -> {
                    val response = LoponSensorProtocol.parseConfigResponse(bytes)
                    _configResponseFlow.tryEmit(response)
                }
            }
        }

        @ObjCSignatureOverride
        override fun peripheral(
            peripheral: CBPeripheral,
            didWriteValueForCharacteristic: CBCharacteristic,
            error: NSError?
        ) {
        }

        private fun fail(message: String) {
            _connectionState.value = BleConnectionState.Error(message)
            connectContinuation?.takeIf { !it.isCompleted }
                ?.complete(Result.failure(Exception(message)))
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    val out = ByteArray(length)
    out.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), this.bytes, length.toULong())
    }
    return out
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = memScoped {
    val arr = allocArrayOf(this@toNSData)
    NSData.dataWithBytes(arr, this@toNSData.size.toULong())
}
