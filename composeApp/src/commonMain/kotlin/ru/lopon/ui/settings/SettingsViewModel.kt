package ru.lopon.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.lopon.domain.model.AppLanguage
import ru.lopon.domain.model.NavigationMode
import ru.lopon.domain.model.Settings
import ru.lopon.domain.model.ThemeMode
import ru.lopon.domain.model.UnitSystem
import ru.lopon.domain.repository.SettingsRepository

data class SettingsUiState(
    val settings: Settings = Settings.DEFAULT,
    val wheelInputText: String = Settings.DEFAULT.wheelCircumferenceMm.toInt().toString(),
    val speedThresholdInputText: String = Settings.DEFAULT.movingSpeedThresholdKmh.toString(),
    val errorMessage: String? = null,
    val isLoading: Boolean = true
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.loadFromFile()
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
                _uiState.update {
                    it.copy(
                        settings = settings,
                        wheelInputText = settings.wheelCircumferenceMm.toInt().toString(),
                        speedThresholdInputText = settings.movingSpeedThresholdKmh.toString(),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateWheelCircumference(text: String) {
        _uiState.update { it.copy(wheelInputText = text) }
        val mm = text.toDoubleOrNull() ?: return
        viewModelScope.launch {
            settingsRepository.updateWheelCircumference(mm).onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    fun selectWheelPreset(mm: Double) {
        _uiState.update { it.copy(wheelInputText = mm.toInt().toString()) }
        viewModelScope.launch {
            settingsRepository.updateWheelCircumference(mm).onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    fun updateUnits(units: UnitSystem) {
        viewModelScope.launch {
            settingsRepository.updateUnits(units)
        }
    }

    fun updateDefaultMode(mode: NavigationMode) {
        viewModelScope.launch {
            settingsRepository.updateDefaultMode(mode)
        }
    }

    fun toggleAutoConnect() {
        viewModelScope.launch {
            val current = _uiState.value.settings
            settingsRepository.updateAutoConnectBle(!current.autoConnectBle)
        }
    }

    fun toggleKeepScreenOn() {
        viewModelScope.launch {
            val current = _uiState.value.settings
            settingsRepository.updateSettings(current.copy(keepScreenOn = !current.keepScreenOn))
        }
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            settingsRepository.updateSettings(current.copy(language = language))
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(mode)
        }
    }

    fun updateMovingSpeedThreshold(text: String) {
        _uiState.update { it.copy(speedThresholdInputText = text) }
        val kmh = text.toDoubleOrNull() ?: return
        if (kmh < 0 || kmh > 20) return
        viewModelScope.launch {
            val current = _uiState.value.settings
            settingsRepository.updateSettings(current.copy(movingSpeedThresholdKmh = kmh))
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
