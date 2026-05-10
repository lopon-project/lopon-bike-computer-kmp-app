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
import ru.lopon.di.IosAppContainer
import ru.lopon.platform.AppPermission
import ru.lopon.platform.BleConnectionState
import ru.lopon.platform.BleDevice
import ru.lopon.platform.IosBleAdapter
import ru.lopon.platform.IosPermissionsManager
import ru.lopon.platform.PermissionState

class SensorViewModel(
    private val container: IosAppContainer
) : ViewModel() {

    private val bleAdapter: IosBleAdapter = container.bleAdapter
    private val permissionsManager: IosPermissionsManager = container.permissionsManager

    private val _uiState = MutableStateFlow(SensorUiState())
    val uiState: StateFlow<SensorUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        checkPermissions()
        observeConnectionState()
    }

    private fun checkPermissions() {
        val bt = permissionsManager.checkPermission(AppPermission.BLUETOOTH)
        val loc = permissionsManager.checkPermission(AppPermission.LOCATION)
        _uiState.update {
            it.copy(
                hasPermissions = bt == PermissionState.GRANTED && loc == PermissionState.GRANTED
            )
        }
    }

    suspend fun requestPermissions() {
        val bt = permissionsManager.requestPermission(AppPermission.BLUETOOTH)
        val loc = permissionsManager.requestPermission(AppPermission.LOCATION)
        permissionsManager.requestPermission(AppPermission.NOTIFICATION)

        _uiState.update {
            it.copy(
                hasPermissions = bt == PermissionState.GRANTED && loc == PermissionState.GRANTED,
                errorMessage = when {
                    bt == PermissionState.DENIED_FOREVER || loc == PermissionState.DENIED_FOREVER ->
                        "Разрешения отклонены. Откройте настройки приложения."
                    bt != PermissionState.GRANTED || loc != PermissionState.GRANTED ->
                        "Требуются разрешения для работы с Bluetooth"
                    else -> null
                }
            )
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            bleAdapter.connectionState.collect { state ->
                _uiState.update {
                    it.copy(
                        connectionState = state,
                        connectedDevice = if (state is BleConnectionState.Connected)
                            bleAdapter.connectedDevice.value else null
                    )
                }
            }
        }
    }

    fun startScan() {
        if (!_uiState.value.hasPermissions) {
            viewModelScope.launch { requestPermissions() }
            return
        }
        if (!permissionsManager.isBluetoothEnabled()) {
            _uiState.update { it.copy(errorMessage = "Bluetooth выключен. Включите Bluetooth.") }
            return
        }

        _uiState.update { it.copy(isScanning = true, devices = emptyList(), errorMessage = null) }

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            try {
                val devices = bleAdapter.scan(timeoutMs = 15000)
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        devices = devices,
                        errorMessage = if (devices.isEmpty()) "Датчики не найдены" else null
                    )
                }
            } catch (_: CancellationException) {
                _uiState.update { it.copy(isScanning = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = "Ошибка сканирования: ${e.message}"
                    )
                }
            } finally {
                scanJob = null
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false) }
    }

    suspend fun connectToDevice(deviceId: String) {
        _uiState.update { it.copy(errorMessage = null) }
        bleAdapter.connect(deviceId).onFailure { error ->
            _uiState.update { it.copy(errorMessage = "Ошибка подключения: ${error.message}") }
        }
    }

    suspend fun disconnect() {
        bleAdapter.disconnect()
    }

    suspend fun startSensor() {
        bleAdapter.startSensor().onSuccess {
            _uiState.update { it.copy(isSensorActive = true) }
        }.onFailure { error ->
            _uiState.update { it.copy(errorMessage = "Ошибка запуска датчика: ${error.message}") }
        }
    }

    suspend fun stopSensor() {
        bleAdapter.stopSensor()
        _uiState.update { it.copy(isSensorActive = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class SensorUiState(
    val connectionState: BleConnectionState = BleConnectionState.Disconnected,
    val connectedDevice: BleDevice? = null,
    val isScanning: Boolean = false,
    val devices: List<BleDevice> = emptyList(),
    val isSensorActive: Boolean = false,
    val hasPermissions: Boolean = false,
    val errorMessage: String? = null
)
