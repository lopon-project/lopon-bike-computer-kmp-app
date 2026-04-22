package ru.lopon.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.lopon.core.metrics.MetricsFormatter
import ru.lopon.core.metrics.TripMetrics
import ru.lopon.domain.state.TripState
import ru.lopon.ui.components.MetricCard
import ru.lopon.ui.components.SpeedDisplay
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography
import ru.lopon.ui.util.rememberIsLandscape

@Composable
internal fun SensorDashboard(
    metrics: TripMetrics,
    isConnected: Boolean,
    isRecording: Boolean,
    isPaused: Boolean,
    tripState: TripState,
    onOpenSensor: () -> Unit,
    onStartTrip: () -> Unit,
    onQuickStart: () -> Unit,
    onStopTrip: () -> Unit,
    onPauseTrip: () -> Unit,
    onResumeTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLandscape = rememberIsLandscape()

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(LoponDimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
            ) {
                BleStatusRow(isConnected = isConnected, onOpenSensor = onOpenSensor)

                SpeedDisplay(
                    speedKmh = metrics.currentSpeedKmh,
                    isActive = isConnected || isRecording,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    speedFontSize = 100.sp
                )

                TripControlsRow(
                    tripState = tripState,
                    onStartTrip = onStartTrip,
                    onQuickStart = onQuickStart,
                    onStopTrip = onStopTrip,
                    onPauseTrip = onPauseTrip,
                    onResumeTrip = onResumeTrip
                )
            }

            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
            ) {
                MetricsGridRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        label = "Дистанция",
                        value = MetricsFormatter.formatDistanceKm(metrics.totalDistanceM),
                        unit = "км"
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        label = "Время в движении",
                        value = MetricsFormatter.formatTimeCompact(metrics.movingTimeMs)
                    )
                }
                MetricsGridRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        label = "Средняя скорость",
                        value = MetricsFormatter.formatSpeedKmh(metrics.averageSpeedMs),
                        unit = "км/ч"
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        label = "Макс. скорость",
                        value = MetricsFormatter.formatSpeedKmh(metrics.maxSpeedMs),
                        unit = "км/ч"
                    )
                }
                MetricsGridRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        label = "Общее время",
                        value = MetricsFormatter.formatTimeCompact(metrics.elapsedTimeMs)
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        label = "Каденс",
                        value = metrics.currentCadenceRpm?.let { "${it.toInt()}" } ?: "—",
                        unit = if (metrics.currentCadenceRpm != null) "об/мин" else ""
                    )
                }
                MetricsGridRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        label = "Набор высоты",
                        value = MetricsFormatter.formatElevationGain(metrics.elevationGainM),
                        unit = if (metrics.elevationGainM != null) "м" else ""
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        label = "Ср. каденс",
                        value = metrics.averageCadenceRpm?.let { "${it.toInt()}" } ?: "—",
                        unit = if (metrics.averageCadenceRpm != null) "об/мин" else ""
                    )
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(LoponDimens.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
        ) {
            BleStatusRow(isConnected = isConnected, onOpenSensor = onOpenSensor)

            SpeedDisplay(
                speedKmh = metrics.currentSpeedKmh,
                isActive = isConnected || isRecording,
                modifier = Modifier.fillMaxWidth(),
                speedFontSize = 80.sp
            )

            MetricsGridRow(modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Дистанция",
                    value = MetricsFormatter.formatDistanceKm(metrics.totalDistanceM),
                    unit = "км"
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Время в движении",
                    value = MetricsFormatter.formatTimeCompact(metrics.movingTimeMs)
                )
            }
            MetricsGridRow(modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Средняя скорость",
                    value = MetricsFormatter.formatSpeedKmh(metrics.averageSpeedMs),
                    unit = "км/ч"
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Макс. скорость",
                    value = MetricsFormatter.formatSpeedKmh(metrics.maxSpeedMs),
                    unit = "км/ч"
                )
            }
            MetricsGridRow(modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Общее время",
                    value = MetricsFormatter.formatTimeCompact(metrics.elapsedTimeMs)
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Каденс",
                    value = metrics.currentCadenceRpm?.let { "${it.toInt()}" } ?: "—",
                    unit = if (metrics.currentCadenceRpm != null) "об/мин" else ""
                )
            }
            MetricsGridRow(modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Набор высоты",
                    value = MetricsFormatter.formatElevationGain(metrics.elevationGainM),
                    unit = if (metrics.elevationGainM != null) "м" else ""
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Ср. каденс",
                    value = metrics.averageCadenceRpm?.let { "${it.toInt()}" } ?: "—",
                    unit = if (metrics.averageCadenceRpm != null) "об/мин" else ""
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            TripControlsRow(
                tripState = tripState,
                onStartTrip = onStartTrip,
                onQuickStart = onQuickStart,
                onStopTrip = onStopTrip,
                onPauseTrip = onPauseTrip,
                onResumeTrip = onResumeTrip
            )
        }
    }
}

@Composable
private fun BleStatusRow(
    isConnected: Boolean,
    onOpenSensor: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        if (isConnected) LoponColors.success else LoponColors.error,
                        CircleShape
                    )
            )
            Text(
                text = if (isConnected) "Датчик подключён" else "Датчик не подключён",
                style = LoponTypography.caption,
                color = if (isConnected) LoponColors.success else LoponColors.onSurfaceSecondary
            )
        }
        OutlinedButton(
            onClick = onOpenSensor,
            shape = LoponShapes.button
        ) {
            Icon(
                if (isConnected) Icons.Filled.Bluetooth else Icons.Filled.BluetoothDisabled,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (isConnected) "Датчик" else "Подключить",
                style = LoponTypography.caption
            )
        }
    }
}

@Composable
private fun MetricsGridRow(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium),
        content = content
    )
}

@Composable
private fun TripControlsRow(
    tripState: TripState,
    onStartTrip: () -> Unit,
    onQuickStart: () -> Unit,
    onStopTrip: () -> Unit,
    onPauseTrip: () -> Unit,
    onResumeTrip: () -> Unit
) {
    when (tripState) {
        is TripState.Idle, is TripState.Finished -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
            ) {
                OutlinedButton(
                    onClick = onStartTrip,
                    modifier = Modifier
                        .weight(1f)
                        .height(LoponDimens.buttonHeightMedium),
                    shape = LoponShapes.button
                ) {
                    Text("Настроить старт", style = LoponTypography.button)
                }
                Button(
                    onClick = onQuickStart,
                    modifier = Modifier
                        .weight(1f)
                        .height(LoponDimens.buttonHeightMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoponColors.primaryYellow,
                        contentColor = LoponColors.black
                    ),
                    shape = LoponShapes.button
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Старт", style = LoponTypography.button)
                }
            }
        }
        is TripState.Recording -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
            ) {
                Button(
                    onClick = onPauseTrip,
                    modifier = Modifier
                        .weight(1f)
                        .height(LoponDimens.buttonHeightMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoponColors.primaryYellow,
                        contentColor = LoponColors.black
                    ),
                    shape = LoponShapes.button
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Пауза", style = LoponTypography.button)
                }
                Button(
                    onClick = onStopTrip,
                    modifier = Modifier
                        .weight(1f)
                        .height(LoponDimens.buttonHeightMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoponColors.error,
                        contentColor = LoponColors.white
                    ),
                    shape = LoponShapes.button
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Стоп", style = LoponTypography.button)
                }
            }
        }
        is TripState.Paused -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
            ) {
                Button(
                    onClick = onResumeTrip,
                    modifier = Modifier
                        .weight(1f)
                        .height(LoponDimens.buttonHeightMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoponColors.primaryYellow,
                        contentColor = LoponColors.black
                    ),
                    shape = LoponShapes.button
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Продолжить", style = LoponTypography.button)
                }
                Button(
                    onClick = onStopTrip,
                    modifier = Modifier
                        .weight(1f)
                        .height(LoponDimens.buttonHeightMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoponColors.error,
                        contentColor = LoponColors.white
                    ),
                    shape = LoponShapes.button
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Стоп", style = LoponTypography.button)
                }
            }
        }
    }
}
