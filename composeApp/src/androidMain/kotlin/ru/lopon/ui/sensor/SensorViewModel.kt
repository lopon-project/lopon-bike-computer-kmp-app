package ru.lopon.ui.sensor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.lopon.di.AndroidAppContainer
import ru.lopon.platform.AndroidBleAdapter
import ru.lopon.platform.AndroidPermissionsManager
import ru.lopon.platform.AppPermission
import ru.lopon.platform.BleConnectionState
import ru.lopon.platform.BleDevice
import ru.lopon.platform.PermissionState

class SensorViewModel(
    application: Application,
    private val container: AndroidAppContainer
) : AndroidViewModel(application) {

    private val bleAdapter: AndroidBleAdapter = container.bleAdapter
    private val permissionsManager: AndroidPermissionsManager = container.permissionsManager

    private val _uiState = MutableStateFlow(SensorUiState())
    val uiState: StateFlow<SensorUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        checkPermissions()
        observeConnectionState()
    }

    class Factory(
        private val application: Application,
        private val container: AndroidAppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SensorViewModel(application, container) as T
        }
    }

    private fun checkPermissions() {
        val bluetoothState = permissionsManager.checkPermission(AppPermission.BLUETOOTH)
        val locationState = permissionsManager.checkPermission(AppPermission.LOCATION)

        _uiState.update {
            it.copy(
                hasPermissions = bluetoothState == PermissionState.GRANTED &&
                        locationState == PermissionState.GRANTED
            )
        }
    }

    suspend fun requestPermissions() {
        val bluetoothState = permissionsManager.requestPermission(AppPermission.BLUETOOTH)
        val locationState = permissionsManager.requestPermission(AppPermission.LOCATION)
        permissionsManager.requestPermission(AppPermission.NOTIFICATION)

        _uiState.update {
            it.copy(
                hasPermissions = bluetoothState == PermissionState.GRANTED &&
                        locationState == PermissionState.GRANTED,
                errorMessage = when {
                    bluetoothState == PermissionState.DENIED_FOREVER ||
                            locationState == PermissionState.DENIED_FOREVER ->
                        "Разрешения отклонены. Откройте настройки приложения."
                    bluetoothState != PermissionState.GRANTED ||
                            locationState != PermissionState.GRANTED ->
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

        _uiState.update {
            it.copy(isScanning = true, devices = emptyList(), errorMessage = null)
        }

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

        val result = bleAdapter.connect(deviceId)

        result.onFailure { error ->
            _uiState.update {
                it.copy(errorMessage = "Ошибка подключения: ${error.message}")
            }
        }
    }

    suspend fun disconnect() {
        bleAdapter.disconnect()
    }

    suspend fun startSensor() {
        val result = bleAdapter.startSensor()

        result.onSuccess {
            _uiState.update { it.copy(isSensorActive = true) }
        }.onFailure { error ->
            _uiState.update {
                it.copy(errorMessage = "Ошибка запуска датчика: ${error.message}")
            }
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
