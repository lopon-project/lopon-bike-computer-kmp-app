package ru.lopon.ui.sensor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.lopon.platform.BleConnectionState
import ru.lopon.platform.BleDevice
import ru.lopon.ui.components.ScreenHeader
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography

@Composable
fun SensorScreen(
    viewModel: SensorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        ScreenHeader(title = "Датчик скорости")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LoponDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerLarge)
        ) {
            ConnectionStatusCard(
                connectionState = uiState.connectionState,
                connectedDevice = uiState.connectedDevice,
                isSensorActive = uiState.isSensorActive
            )

            ActionButtonsRow(
                connectionState = uiState.connectionState,
                isScanning = uiState.isScanning,
                isSensorActive = uiState.isSensorActive,
                onScanClick = { viewModel.startScan() },
                onStopScanClick = { viewModel.stopScan() },
                onDisconnectClick = { scope.launch { viewModel.disconnect() } },
                onStartSensorClick = { scope.launch { viewModel.startSensor() } },
                onStopSensorClick = { scope.launch { viewModel.stopSensor() } }
            )

            when {
                uiState.connectionState is BleConnectionState.Connected -> {
                    ConnectedInfoCard(
                        device = uiState.connectedDevice,
                        isSensorActive = uiState.isSensorActive
                    )
                }
                uiState.isScanning || uiState.devices.isNotEmpty() -> {
                    DeviceListCard(
                        devices = uiState.devices,
                        isScanning = uiState.isScanning,
                        onDeviceClick = { device ->
                            scope.launch { viewModel.connectToDevice(device.id) }
                        }
                    )
                }
                else -> {
                    SensorEmptyState()
                }
            }

            uiState.errorMessage?.let { error ->
                SensorErrorCard(message = error, onDismiss = { viewModel.clearError() })
            }

            if (!uiState.hasPermissions) {
                PermissionCard(onRequestClick = { scope.launch { viewModel.requestPermissions() } })
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: BleConnectionState,
    connectedDevice: BleDevice?,
    isSensorActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                is BleConnectionState.Connected -> LoponColors.success.copy(alpha = 0.08f)
                is BleConnectionState.Connecting -> LoponColors.warning.copy(alpha = 0.08f)
                is BleConnectionState.Error -> LoponColors.error.copy(alpha = 0.08f)
                else -> LoponColors.backgroundLight
            }
        ),
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
                    .background(
                        when (connectionState) {
                            is BleConnectionState.Connected -> LoponColors.success
                            is BleConnectionState.Connecting -> LoponColors.warning
                            is BleConnectionState.Error -> LoponColors.error
                            else -> LoponColors.divider
                        }
                    )
            )

            Spacer(modifier = Modifier.width(LoponDimens.spacerMedium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (connectionState) {
                        is BleConnectionState.Connected -> "Подключено"
                        is BleConnectionState.Connecting -> "Подключение..."
                        is BleConnectionState.Error -> "Ошибка"
                        else -> "Отключено"
                    },
                    style = LoponTypography.body,
                    fontWeight = FontWeight.Medium,
                    color = LoponColors.onSurfacePrimary
                )

                connectedDevice?.let {
                    Text(
                        text = it.name ?: "Unknown",
                        style = LoponTypography.caption,
                        color = LoponColors.onSurfaceSecondary
                    )
                }

                if (connectionState is BleConnectionState.Error) {
                    Text(
                        text = connectionState.message,
                        style = LoponTypography.caption,
                        color = LoponColors.error
                    )
                }
            }

            if (connectionState is BleConnectionState.Connected) {
                Icon(
                    imageVector = if (isSensorActive) Icons.Filled.PlayArrow else Icons.Filled.Bluetooth,
                    contentDescription = if (isSensorActive) "Активен" else "Подключён",
                    tint = if (isSensorActive) LoponColors.success else LoponColors.onSurfaceSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    connectionState: BleConnectionState,
    isScanning: Boolean,
    isSensorActive: Boolean,
    onScanClick: () -> Unit,
    onStopScanClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onStartSensorClick: () -> Unit,
    onStopSensorClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
    ) {
        when (connectionState) {
            is BleConnectionState.Connected -> {
                Button(
                    onClick = onDisconnectClick,
                    modifier = Modifier.weight(1f).height(LoponDimens.buttonHeightMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoponColors.error,
                        contentColor = LoponColors.white
                    ),
                    shape = LoponShapes.button
                ) {
                    Icon(Icons.Filled.BluetoothDisabled, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Отключить", style = LoponTypography.button)
                }

                if (isSensorActive) {
                    Button(
                        onClick = onStopSensorClick,
                        modifier = Modifier.weight(1f).height(LoponDimens.buttonHeightMedium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LoponColors.warning,
                            contentColor = LoponColors.black
                        ),
                        shape = LoponShapes.button
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Стоп", style = LoponTypography.button)
                    }
                } else {
                    Button(
                        onClick = onStartSensorClick,
                        modifier = Modifier.weight(1f).height(LoponDimens.buttonHeightMedium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LoponColors.primaryYellow,
                            contentColor = LoponColors.black
                        ),
                        shape = LoponShapes.button
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Старт", style = LoponTypography.button)
                    }
                }
            }
            is BleConnectionState.Connecting -> {
                Button(
                    onClick = onDisconnectClick,
                    modifier = Modifier.weight(1f).height(LoponDimens.buttonHeightMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoponColors.error,
                        contentColor = LoponColors.white
                    ),
                    shape = LoponShapes.button
                ) {
                    Text("Отмена", style = LoponTypography.button)
                }
            }
            else -> {
                if (isScanning) {
                    Button(
                        onClick = onStopScanClick,
                        modifier = Modifier.weight(1f).height(LoponDimens.buttonHeightMedium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LoponColors.warning,
                            contentColor = LoponColors.black
                        ),
                        shape = LoponShapes.button
                    ) {
                        Text("Остановить поиск", style = LoponTypography.button)
                    }
                } else {
                    Button(
                        onClick = onScanClick,
                        modifier = Modifier.weight(1f).height(LoponDimens.buttonHeightMedium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LoponColors.primaryYellow,
                            contentColor = LoponColors.black
                        ),
                        shape = LoponShapes.button
                    ) {
                        Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Поиск датчиков", style = LoponTypography.button)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedInfoCard(
    device: BleDevice?,
    isSensorActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LoponShapes.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Подключено",
                tint = LoponColors.success,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(LoponDimens.spacerLarge))

            Text(
                text = device?.name ?: "Датчик подключён",
                style = LoponTypography.sectionTitle,
                color = LoponColors.onSurfacePrimary
            )

            Spacer(modifier = Modifier.height(LoponDimens.spacerSmall))

            Text(
                text = if (isSensorActive) {
                    "Датчик активен. Перейдите на вкладку «Поездка» для просмотра данных."
                } else {
                    "Нажмите «Старт» для активации датчика"
                },
                style = LoponTypography.body,
                color = LoponColors.onSurfaceSecondary
            )
        }
    }
}

@Composable
private fun DeviceListCard(
    devices: List<BleDevice>,
    isScanning: Boolean,
    onDeviceClick: (BleDevice) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LoponShapes.card
    ) {
        Column(modifier = Modifier.padding(LoponDimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Найденные устройства",
                    style = LoponTypography.body,
                    fontWeight = FontWeight.Medium,
                    color = LoponColors.onSurfacePrimary
                )
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LoponColors.primaryYellow
                    )
                }
            }

            Spacer(modifier = Modifier.height(LoponDimens.spacerSmall))

            if (devices.isEmpty() && isScanning) {
                Text(
                    text = "Поиск устройств...",
                    style = LoponTypography.body,
                    color = LoponColors.onSurfaceSecondary
                )
            } else if (devices.isEmpty()) {
                Text(
                    text = "Устройства не найдены",
                    style = LoponTypography.body,
                    color = LoponColors.onSurfaceSecondary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
                ) {
                    items(devices) { device ->
                        DeviceItem(device = device, onClick = { onDeviceClick(device) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(
    device: BleDevice,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LoponShapes.small)
            .clickable(onClick = onClick),
        color = LoponColors.backgroundLight
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LoponDimens.spacerMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = LoponTypography.body,
                    fontWeight = FontWeight.Medium,
                    color = LoponColors.onSurfacePrimary
                )
                Text(
                    text = device.id,
                    style = LoponTypography.caption,
                    color = LoponColors.onSurfaceSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${device.rssi} dBm",
                    style = LoponTypography.caption,
                    color = LoponColors.onSurfaceSecondary
                )
                if (device.isPreviouslyConnected) {
                    Text(
                        text = "Ранее подключён",
                        style = LoponTypography.caption,
                        color = LoponColors.success
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorEmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LoponShapes.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.AutoMirrored.Filled.BluetoothSearching,
                contentDescription = "Поиск",
                tint = LoponColors.onSurfaceSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(LoponDimens.spacerSmall))
            Text(
                text = "Нажмите «Поиск датчиков»",
                style = LoponTypography.body,
                color = LoponColors.onSurfaceSecondary
            )
            Text(
                text = "для поиска Lopon HSS",
                style = LoponTypography.caption,
                color = LoponColors.onSurfaceSecondary
            )
        }
    }
}

@Composable
private fun SensorErrorCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LoponColors.error.copy(alpha = 0.08f)),
        shape = LoponShapes.card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LoponDimens.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = LoponColors.error,
                style = LoponTypography.body,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    }
}

@Composable
private fun PermissionCard(onRequestClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LoponColors.warning.copy(alpha = 0.08f)),
        shape = LoponShapes.card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LoponDimens.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Требуются разрешения",
                    style = LoponTypography.body,
                    fontWeight = FontWeight.Medium,
                    color = LoponColors.onSurfacePrimary
                )
                Text(
                    text = "Bluetooth и геолокация для поиска датчиков",
                    style = LoponTypography.caption,
                    color = LoponColors.onSurfaceSecondary
                )
            }
            Button(
                onClick = onRequestClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoponColors.primaryYellow,
                    contentColor = LoponColors.black
                ),
                shape = LoponShapes.button
            ) {
                Text("Разрешить", style = LoponTypography.button)
            }
        }
    }
}
