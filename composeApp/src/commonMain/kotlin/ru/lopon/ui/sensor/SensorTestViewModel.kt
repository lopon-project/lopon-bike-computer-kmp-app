package ru.lopon.ui.sensor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import ru.lopon.core.metrics.MetricsFormatter
import ru.lopon.domain.model.SensorReading
import ru.lopon.domain.repository.SettingsRepository
import ru.lopon.platform.AppPermission
import ru.lopon.platform.BleAdapter
import ru.lopon.platform.BleConnectionState
import ru.lopon.platform.BleDevice
import ru.lopon.platform.ConfigResponse
import ru.lopon.platform.PermissionState
import ru.lopon.platform.PermissionsManager

enum class LogEntryType { CONNECTION, SENSOR_DATA, CONFIG, ERROR, INFO }

data class LogEntry(
    val timestamp: Long,
    val type: LogEntryType,
    val message: String
)

class SensorTestViewModel(
    private val bleAdapter: BleAdapter,
    private val permissionsManager: PermissionsManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SensorTestUiState())
    val uiState: StateFlow<SensorTestUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var dataCollectionJob: Job? = null
    private var previousReading: SensorReading? = null

    companion object {
        private const val MAX_LOG_ENTRIES = 200
    }

    init {
        checkPermissions()
        observeConnectionState()
        observeConfigResponses()
    }

    private fun appendLog(type: LogEntryType, message: String) {
        _uiState.update {
            val next = (it.logEntries + LogEntry(Clock.System.now().toEpochMilliseconds(), type, message))
                .takeLast(MAX_LOG_ENTRIES)
            it.copy(logEntries = next)
        }
    }

    fun clearLog() {
        _uiState.update { it.copy(logEntries = emptyList()) }
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
        appendLog(LogEntryType.ERROR, message)
    }

    private fun checkPermissions() {
        val bt = permissionsManager.checkPermission(AppPermission.BLUETOOTH)
        val loc = permissionsManager.checkPermission(AppPermission.LOCATION)
        _uiState.update {
            it.copy(hasPermissions = bt == PermissionState.GRANTED && loc == PermissionState.GRANTED)
        }
    }

    suspend fun requestPermissions() {
        val bt = permissionsManager.requestPermission(AppPermission.BLUETOOTH)
        val loc = permissionsManager.requestPermission(AppPermission.LOCATION)
        permissionsManager.requestPermission(AppPermission.NOTIFICATION)
        val granted = bt == PermissionState.GRANTED && loc == PermissionState.GRANTED
        val error = when {
            bt == PermissionState.DENIED_FOREVER || loc == PermissionState.DENIED_FOREVER ->
                "Разрешения отклонены. Откройте настройки приложения."
            bt != PermissionState.GRANTED || loc != PermissionState.GRANTED ->
                "Требуются разрешения для работы с Bluetooth"
            else -> null
        }
        _uiState.update {
            it.copy(hasPermissions = granted, errorMessage = error)
        }
        if (error != null) appendLog(LogEntryType.ERROR, error)
        else appendLog(LogEntryType.INFO, "Разрешения получены")
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            bleAdapter.connectionState.collect { state ->
                val label = when (state) {
                    is BleConnectionState.Disconnected -> "Отключено"
                    is BleConnectionState.Connecting -> "Подключение..."
                    is BleConnectionState.Connected -> "Подключено"
                    is BleConnectionState.Error -> "Ошибка: ${state.message}"
                }
                appendLog(LogEntryType.CONNECTION, "Состояние: $label")
                _uiState.update {
                    it.copy(
                        connectionState = state,
                        connectedDevice = if (state is BleConnectionState.Connected)
                            bleAdapter.connectedDevice.value else null
                    )
                }
                if (state is BleConnectionState.Disconnected || state is BleConnectionState.Error) {
                    stopDataCollection()
                }
            }
        }
    }

    private fun observeConfigResponses() {
        viewModelScope.launch {
            bleAdapter.observeConfigResponse().collect { response ->
                val label = when (response) {
                    is ConfigResponse.Success -> "Success"
                    is ConfigResponse.Error -> "Error"
                    is ConfigResponse.Unknown -> "Unknown"
                }
                appendLog(LogEntryType.CONFIG, "Ответ конфигурации: $label")
            }
        }
    }

    fun startScan() {
        if (!_uiState.value.hasPermissions) {
            viewModelScope.launch { requestPermissions() }
            return
        }
        if (!permissionsManager.isBluetoothEnabled()) {
            setError("Bluetooth выключен. Включите Bluetooth.")
            return
        }
        appendLog(LogEntryType.INFO, "Сканирование: запуск")
        _uiState.update { it.copy(isScanning = true, devices = emptyList(), errorMessage = null) }
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            try {
                val devices = bleAdapter.scan(timeoutMs = 15000)
                appendLog(LogEntryType.INFO, "Сканирование: найдено устройств ${devices.size}")
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        devices = devices,
                        errorMessage = if (devices.isEmpty()) "Датчики не найдены" else null
                    )
                }
                if (devices.isEmpty()) appendLog(LogEntryType.INFO, "Датчики не найдены")
            } catch (_: CancellationException) {
                _uiState.update { it.copy(isScanning = false) }
                appendLog(LogEntryType.INFO, "Сканирование: отменено")
            } catch (e: Exception) {
                _uiState.update { it.copy(isScanning = false) }
                setError("Ошибка сканирования: ${e.message}")
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false) }
        appendLog(LogEntryType.INFO, "Сканирование: остановлено")
    }

    suspend fun connectToDevice(deviceId: String) {
        _uiState.update { it.copy(errorMessage = null) }
        appendLog(LogEntryType.INFO, "Подключение к $deviceId")
        bleAdapter.connect(deviceId).onFailure { error ->
            setError("Ошибка подключения: ${error.message}")
        }
    }

    suspend fun disconnect() {
        appendLog(LogEntryType.INFO, "Отключение от датчика")
        stopDataCollection()
        bleAdapter.disconnect()
    }

    suspend fun startSensor() {
        appendLog(LogEntryType.INFO, "Запуск датчика")
        bleAdapter.startSensor().onSuccess {
            _uiState.update { it.copy(isSensorActive = true) }
            appendLog(LogEntryType.INFO, "Датчик запущен")
            startDataCollection()
        }.onFailure { error ->
            setError("Ошибка запуска датчика: ${error.message}")
        }
    }

    suspend fun stopSensor() {
        appendLog(LogEntryType.INFO, "Остановка датчика")
        bleAdapter.stopSensor()
        stopDataCollection()
        _uiState.update { it.copy(isSensorActive = false) }
    }

    private fun startDataCollection() {
        dataCollectionJob?.cancel()
        previousReading = null
        dataCollectionJob = viewModelScope.launch {
            bleAdapter.observeWheelData().collect { reading ->
                processReading(reading)
            }
        }
    }

    private fun stopDataCollection() {
        dataCollectionJob?.cancel()
        dataCollectionJob = null
        previousReading = null
    }

    private suspend fun processReading(reading: SensorReading) {
        val wheelCircMm = settingsRepository.getCurrentSettings().wheelCircumferenceMm

        val prev = previousReading
        var speedKmh = 0.0
        if (prev != null) {
            val timeDeltaSec = reading.timeDeltaSeconds(prev)
            if (timeDeltaSec > 0.0 && timeDeltaSec < 10.0) {
                val revDelta = reading.revolutionsDelta(prev)
                val distanceM = revDelta * wheelCircMm / 1000.0
                speedKmh = (distanceM / timeDeltaSec) * 3.6
                if (speedKmh < 0.0 || speedKmh > 120.0) speedKmh = 0.0
            }
        }
        previousReading = reading

        val cadenceText = reading.cadence?.let { MetricsFormatter.formatDecimal(it, 1) } ?: "—"
        val speedText = MetricsFormatter.formatDecimal(speedKmh, 1)
        appendLog(
            LogEntryType.SENSOR_DATA,
            "rev=${reading.cumulativeRevolutions} evt=${reading.wheelEventTimeUnits} " +
                "v=$speedText км/ч cad=$cadenceText"
        )

        _uiState.update {
            it.copy(
                lastRevolutions = reading.cumulativeRevolutions,
                lastEventTime = reading.wheelEventTimeUnits,
                calculatedSpeedKmh = speedKmh,
                cadenceRpm = reading.cadence,
                readingsCount = it.readingsCount + 1
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class SensorTestUiState(
    val connectionState: BleConnectionState = BleConnectionState.Disconnected,
    val connectedDevice: BleDevice? = null,
    val isScanning: Boolean = false,
    val devices: List<BleDevice> = emptyList(),
    val isSensorActive: Boolean = false,
    val hasPermissions: Boolean = false,
    val errorMessage: String? = null,
    val lastRevolutions: Long = 0,
    val lastEventTime: Int = 0,
    val calculatedSpeedKmh: Double = 0.0,
    val cadenceRpm: Double? = null,
    val readingsCount: Int = 0,
    val logEntries: List<LogEntry> = emptyList()
)
