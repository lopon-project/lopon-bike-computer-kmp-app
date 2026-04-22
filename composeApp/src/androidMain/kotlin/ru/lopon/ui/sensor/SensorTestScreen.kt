package ru.lopon.ui.sensor

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.lopon.platform.BleConnectionState
import ru.lopon.ui.components.MetricCard
import ru.lopon.ui.components.SpeedDisplay
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SensorTestScreen(
    viewModel: SensorTestViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LoponColors.backgroundLight)
    ) {
        DiagnosticsHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(LoponDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerLarge)
        ) {
            ConnectionStatusCard(state)

            ActionButtons(
                state = state,
                onScan = { viewModel.startScan() },
                onConnect = { id -> scope.launch { viewModel.connectToDevice(id) } },
                onDisconnect = { scope.launch { viewModel.disconnect() } },
                onStartSensor = { scope.launch { viewModel.startSensor() } },
                onStopSensor = { scope.launch { viewModel.stopSensor() } }
            )

            // Device list (when scanning or devices found)
            if (state.isScanning || state.devices.isNotEmpty()) {
                DeviceList(
                    devices = state.devices,
                    isScanning = state.isScanning,
                    onDeviceClick = { device ->
                        scope.launch { viewModel.connectToDevice(device.id) }
                    }
                )
            }

            if (state.connectionState is BleConnectionState.Connected && state.isSensorActive) {
                LiveDataSection(state)
            }

            BleLogSection(
                entries = state.logEntries,
                onClear = { viewModel.clearLog() }
            )

            state.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LoponColors.errorLight),
                    shape = LoponShapes.card
                ) {
                    Text(
                        text = error,
                        style = LoponTypography.body,
                        color = LoponColors.error,
                        modifier = Modifier.padding(LoponDimens.cardPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(state: SensorTestUiState) {
    val statusColor = when (state.connectionState) {
        is BleConnectionState.Connected -> LoponColors.success
        is BleConnectionState.Connecting -> LoponColors.warning
        is BleConnectionState.Error -> LoponColors.error
        else -> LoponColors.onSurfaceDisabled
    }
    val statusText = when (state.connectionState) {
        is BleConnectionState.Connected -> "Подключено: ${state.connectedDevice?.name ?: "—"}"
        is BleConnectionState.Connecting -> "Подключение..."
        is BleConnectionState.Error -> "Ошибка подключения"
        else -> "Не подключено"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LoponColors.surfaceCard),
        shape = LoponShapes.card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LoponDimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(LoponDimens.statusDotSize)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(LoponDimens.spacerMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Статус соединения",
                    style = LoponTypography.metricLabel,
                    color = LoponColors.onSurfaceSecondary
                )
                Text(
                    text = statusText,
                    style = LoponTypography.sectionTitle,
                    color = LoponColors.onSurfacePrimary
                )
            }
            if (state.isSensorActive) {
                SensorActiveIndicator()
            }
        }
    }
}

@Composable
private fun SensorActiveIndicator() {
    val transition = rememberInfiniteTransition(label = "sensorPulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .alpha(alpha)
                .clip(CircleShape)
                .background(LoponColors.success)
        )
        Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
        Text(
            text = "LIVE",
            style = LoponTypography.caption,
            color = LoponColors.success
        )
    }
}

@Composable
private fun ActionButtons(
    state: SensorTestUiState,
    onScan: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onStartSensor: () -> Unit,
    onStopSensor: () -> Unit
) {
    val isConnected = state.connectionState is BleConnectionState.Connected

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
    ) {
        if (isConnected) {
            if (state.isSensorActive) {
                Button(
                    onClick = onStopSensor,
                    modifier = Modifier
                        .weight(1f)
                        .height(LoponDimens.buttonHeightMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoponColors.error,
                        contentColor = LoponColors.white
                    ),
                    shape = LoponShapes.button
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Стоп", style = LoponTypography.button)
                }
            } else {
                Button(
                    onClick = onStartSensor,
                    modifier = Modifier
                        .weight(1f)
                        .height(LoponDimens.buttonHeightMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoponColors.success,
                        contentColor = LoponColors.white
                    ),
                    shape = LoponShapes.button
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Старт", style = LoponTypography.button)
                }
            }
            Button(
                onClick = onDisconnect,
                modifier = Modifier
                    .weight(1f)
                    .height(LoponDimens.buttonHeightMedium),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoponColors.black,
                    contentColor = LoponColors.white
                ),
                shape = LoponShapes.button
            ) {
                Icon(Icons.Filled.BluetoothDisabled, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Отключить", style = LoponTypography.button)
            }
        } else {
            Button(
                onClick = onScan,
                enabled = !state.isScanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LoponDimens.buttonHeightMedium),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoponColors.primaryYellow,
                    contentColor = LoponColors.black
                ),
                shape = LoponShapes.button
            ) {
                if (state.isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = LoponColors.black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.BluetoothSearching,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (state.isScanning) "Поиск..." else "Поиск датчиков",
                    style = LoponTypography.button
                )
            }
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<ru.lopon.platform.BleDevice>,
    isScanning: Boolean,
    onDeviceClick: (ru.lopon.platform.BleDevice) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LoponColors.surfaceCard),
        shape = LoponShapes.card
    ) {
        Column(modifier = Modifier.padding(LoponDimens.cardPadding)) {
            Text(
                text = if (isScanning) "Поиск устройств..." else "Найденные устройства",
                style = LoponTypography.sectionTitle,
                color = LoponColors.onSurfacePrimary
            )
            Spacer(modifier = Modifier.height(LoponDimens.spacerMedium))
            devices.forEach { device ->
                Button(
                    onClick = { onDeviceClick(device) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoponColors.backgroundLight,
                        contentColor = LoponColors.onSurfacePrimary
                    ),
                    shape = LoponShapes.cardSmall
                ) {
                    Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.name ?: "Неизвестное устройство",
                            style = LoponTypography.body
                        )
                        Text(
                            text = device.id,
                            style = LoponTypography.caption,
                            color = LoponColors.onSurfaceSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveDataSection(state: SensorTestUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LoponColors.primaryYellow),
        shape = LoponShapes.card
    ) {
        SpeedDisplay(
            speedKmh = state.calculatedSpeedKmh,
            isActive = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(LoponDimens.cardPadding)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
    ) {
        MetricCard(
            label = "Обороты",
            value = "${state.lastRevolutions}",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "Время события",
            value = "${state.lastEventTime}",
            unit = "ед.",
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
    ) {
        MetricCard(
            label = "Каденс",
            value = state.cadenceRpm?.let { "%.0f".format(it) } ?: "—",
            unit = "об/мин",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "Пакетов",
            value = "${state.readingsCount}",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BleLogSection(
    entries: List<LogEntry>,
    onClear: () -> Unit
) {
    val listState = rememberLazyListState()
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) true
            else visibleItems.last().index >= layoutInfo.totalItemsCount - 1
        }
    }

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty() && isAtBottom) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LoponColors.surfaceCard),
        shape = LoponShapes.card
    ) {
        Column(modifier = Modifier.padding(LoponDimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Журнал BLE",
                    style = LoponTypography.sectionTitle,
                    color = LoponColors.onSurfacePrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${entries.size}",
                    style = LoponTypography.caption,
                    color = LoponColors.onSurfaceSecondary
                )
                Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
                TextButton(
                    onClick = onClear,
                    enabled = entries.isNotEmpty()
                ) {
                    Text(
                        text = "Очистить",
                        style = LoponTypography.button,
                        color = LoponColors.primaryYellow
                    )
                }
            }
            Spacer(modifier = Modifier.height(LoponDimens.spacerSmall))

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет событий",
                        style = LoponTypography.caption,
                        color = LoponColors.onSurfaceSecondary
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(items = entries, key = { it.timestamp }) { entry ->
                        LogEntryRow(entry)
                    }
                }
            }
        }
    }
}

private val logTimeFormatter: SimpleDateFormat by lazy {
    SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    val typeColor = when (entry.type) {
        LogEntryType.ERROR -> LoponColors.error
        LogEntryType.CONNECTION -> LoponColors.info
        LogEntryType.SENSOR_DATA -> LoponColors.primaryYellow
        LogEntryType.CONFIG -> LoponColors.onSurfaceSecondary
        LogEntryType.INFO -> LoponColors.onSurfaceSecondary
    }
    val typeTag = when (entry.type) {
        LogEntryType.ERROR -> "ERR"
        LogEntryType.CONNECTION -> "CON"
        LogEntryType.SENSOR_DATA -> "DAT"
        LogEntryType.CONFIG -> "CFG"
        LogEntryType.INFO -> "INF"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = logTimeFormatter.format(Date(entry.timestamp)),
            style = LoponTypography.caption,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = LoponColors.onSurfaceSecondary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = typeTag,
            style = LoponTypography.caption,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = typeColor
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = entry.message,
            style = LoponTypography.caption,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = LoponColors.onSurfacePrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DiagnosticsHeader(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LoponColors.topBarBackground)
            .padding(
                horizontal = LoponDimens.spacerSmall,
                vertical = LoponDimens.spacerMedium
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = LoponColors.topBarContent
                )
            }
            Text(
                text = "Диагностика датчика",
                style = LoponTypography.screenTitle,
                color = LoponColors.topBarContent
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
