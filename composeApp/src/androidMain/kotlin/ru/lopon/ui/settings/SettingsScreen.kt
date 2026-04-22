package ru.lopon.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.lopon.domain.model.AppLanguage
import ru.lopon.domain.model.NavigationMode
import ru.lopon.domain.model.ThemeMode
import ru.lopon.domain.model.UnitSystem
import ru.lopon.ui.components.ErrorCard
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography

private data class WheelPreset(val label: String, val mm: Double)

private val wheelPresets = listOf(
    WheelPreset("700x23c", 2096.0),
    WheelPreset("700x25c", 2105.0),
    WheelPreset("700x28c", 2136.0),
    WheelPreset("700x32c", 2155.0),
    WheelPreset("700x42c", 2224.0),
    WheelPreset("700x45c", 2242.0),
    WheelPreset("26x2.0", 2055.0),
    WheelPreset("27.5x2.1", 2148.0),
    WheelPreset("29x2.1", 2288.0)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings

    Column(modifier = modifier.fillMaxSize()) {
        SettingsHeader(onBack = onBack)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = LoponDimens.contentMaxWidthCapped)
                    .fillMaxWidth()
                    .padding(LoponDimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
            ) {
                SettingsCard(title = "Окружность колеса") {
                    OutlinedTextField(
                        value = uiState.wheelInputText,
                        onValueChange = { viewModel.updateWheelCircumference(it) },
                        label = { Text("мм") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LoponColors.primaryYellow,
                            cursorColor = LoponColors.primaryYellow
                        )
                    )
                    Spacer(modifier = Modifier.height(LoponDimens.spacerSmall))
                    Text(
                        text = "Популярные размеры:",
                        style = LoponTypography.caption,
                        color = LoponColors.onSurfaceSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        wheelPresets.forEach { preset ->
                            FilterChip(
                                selected = settings.wheelCircumferenceMm == preset.mm,
                                onClick = { viewModel.selectWheelPreset(preset.mm) },
                                label = { Text(preset.label, style = LoponTypography.caption) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LoponColors.primaryYellow,
                                    selectedLabelColor = LoponColors.black
                                )
                            )
                        }
                    }
                }

                SettingsCard(title = "Единицы измерения") {
                    SegmentedSelector(
                        options = listOf("Метрическая" to UnitSystem.METRIC, "Имперская" to UnitSystem.IMPERIAL),
                        selected = settings.units,
                        onSelected = { viewModel.updateUnits(it) }
                    )
                }

                SettingsCard(title = "Режим по умолчанию") {
                    SegmentedSelector(
                        options = listOf(
                            "Датчик" to NavigationMode.Sensor,
                            "Гибрид" to NavigationMode.Hybrid,
                            "GPS" to NavigationMode.Gps
                        ),
                        selected = settings.defaultMode,
                        onSelected = { viewModel.updateDefaultMode(it) }
                    )
                }

                SettingsCard(title = "Автоподключение BLE") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Подключаться к последнему датчику при запуске",
                            style = LoponTypography.caption,
                            color = LoponColors.onSurfacePrimary,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = LoponDimens.spacerSmall)
                        )
                        Switch(
                            checked = settings.autoConnectBle,
                            onCheckedChange = { viewModel.toggleAutoConnect() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LoponColors.white,
                                checkedTrackColor = LoponColors.primaryYellow
                            )
                        )
                    }
                }

                SettingsCard(title = "Экран всегда включён") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Не гасить экран во время поездки",
                            style = LoponTypography.caption,
                            color = LoponColors.onSurfacePrimary,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = LoponDimens.spacerSmall)
                        )
                        Switch(
                            checked = settings.keepScreenOn,
                            onCheckedChange = { viewModel.toggleKeepScreenOn() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LoponColors.white,
                                checkedTrackColor = LoponColors.primaryYellow
                            )
                        )
                    }
                }

                SettingsCard(title = "Язык") {
                    SegmentedSelector(
                        options = listOf(
                            "Системный" to AppLanguage.SYSTEM,
                            "Русский" to AppLanguage.RU,
                            "English" to AppLanguage.EN
                        ),
                        selected = settings.language,
                        onSelected = { viewModel.updateLanguage(it) }
                    )
                }

                SettingsCard(title = "Тема оформления") {
                    SegmentedSelector(
                        options = listOf(
                            "Система" to ThemeMode.SYSTEM,
                            "Светлая" to ThemeMode.LIGHT,
                            "Тёмная" to ThemeMode.DARK
                        ),
                        selected = settings.themeMode,
                        onSelected = { viewModel.updateThemeMode(it) }
                    )
                }

                SettingsCard(title = "Порог скорости движения") {
                    OutlinedTextField(
                        value = uiState.speedThresholdInputText,
                        onValueChange = { viewModel.updateMovingSpeedThreshold(it) },
                        label = { Text("км/ч") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LoponColors.primaryYellow,
                            cursorColor = LoponColors.primaryYellow
                        )
                    )
                    Text(
                        text = "Скорость ниже этого значения не учитывается как движение",
                        style = LoponTypography.caption,
                        color = LoponColors.onSurfaceSecondary
                    )
                }

                uiState.errorMessage?.let { error ->
                    ErrorCard(message = error, onDismiss = { viewModel.clearError() })
                }

                Spacer(modifier = Modifier.height(LoponDimens.spacerLarge))
            }
        }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LoponColors.topBarBackground)
            .padding(
                horizontal = LoponDimens.spacerSmall,
                vertical = LoponDimens.spacerMedium
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = LoponColors.topBarContent
                )
            }
            Text(
                text = "Настройки",
                style = LoponTypography.screenTitle,
                color = LoponColors.topBarContent,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(LoponColors.primaryYellow)
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LoponShapes.card,
        colors = CardDefaults.cardColors(containerColor = LoponColors.surfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(LoponColors.primaryYellow)
            )
            Column(
                modifier = Modifier.padding(LoponDimens.cardPadding),
                verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
            ) {
                Text(
                    text = title,
                    style = LoponTypography.body,
                    fontWeight = FontWeight.SemiBold,
                    color = LoponColors.onSurfacePrimary
                )
                content()
            }
        }
    }
}

@Composable
private fun <T> SegmentedSelector(
    options: List<Pair<String, T>>,
    selected: T,
    onSelected: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
    ) {
        options.forEach { (label, value) ->
            val isSelected = value == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(value) },
                label = {
                    Text(
                        text = label,
                        style = LoponTypography.buttonSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LoponColors.primaryYellow,
                    selectedLabelColor = LoponColors.black
                )
            )
        }
    }
}
