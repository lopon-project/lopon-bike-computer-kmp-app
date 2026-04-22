package ru.lopon.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.lopon.core.metrics.MetricsFormatter
import ru.lopon.core.metrics.TripMetrics
import ru.lopon.domain.state.TripState
import ru.lopon.ui.components.MetricCard
import ru.lopon.ui.components.PausedControls
import ru.lopon.ui.components.RecordingControls
import ru.lopon.ui.components.StartTripButton
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography
import kotlin.math.roundToInt

@Composable
internal fun MetricsGrid(
    metrics: TripMetrics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
        ) {
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
        ) {
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                label = "Общее время",
                value = MetricsFormatter.formatTimeCompact(metrics.elapsedTimeMs)
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                label = "Набор высоты",
                value = MetricsFormatter.formatElevationGain(metrics.elevationGainM),
                unit = "м"
            )
        }
    }
}

@Composable
internal fun RouteProgressCard(
    progressPercent: Double,
    distanceToEndM: Double,
    modifier: Modifier = Modifier
) {
    MetricCard(
        modifier = modifier.fillMaxWidth(),
        label = "Маршрут (Sensor)",
        value = "${progressPercent.roundToInt()}%",
        unit = "до финиша ${MetricsFormatter.formatDistanceKm(distanceToEndM)} км"
    )
}

@Composable
internal fun TripControlButtons(
    tripState: TripState,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    modifier: Modifier = Modifier,
    verticalStack: Boolean = false
) {
    if (verticalStack) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
        ) {
            when (tripState) {
                is TripState.Idle, is TripState.Finished -> {
                    StartTripButton(
                        onClick = onStartClick,
                        enabled = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is TripState.Recording -> {
                    VerticalPauseButton(onClick = onPauseClick)
                    VerticalStopButton(onClick = onStopClick)
                }

                is TripState.Paused -> {
                    VerticalResumeButton(onClick = onResumeClick)
                    VerticalStopButton(onClick = onStopClick)
                }
            }
        }
        return
    }

    when (tripState) {
        is TripState.Idle, is TripState.Finished -> {
            StartTripButton(
                onClick = onStartClick,
                enabled = true,
                modifier = modifier
            )
        }

        is TripState.Recording -> {
            RecordingControls(
                onPauseClick = onPauseClick,
                onStopClick = onStopClick,
                modifier = modifier
            )
        }

        is TripState.Paused -> {
            PausedControls(
                onResumeClick = onResumeClick,
                onStopClick = onStopClick,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun VerticalPauseButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(LoponDimens.buttonHeightMedium),
        shape = LoponShapes.button
    ) {
        Icon(Icons.Filled.Pause, contentDescription = null)
        Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
        Text(text = "Пауза", style = LoponTypography.button)
    }
}

@Composable
private fun VerticalResumeButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(LoponDimens.buttonHeightMedium),
        colors = ButtonDefaults.buttonColors(
            containerColor = LoponColors.primaryYellow,
            contentColor = LoponColors.black
        ),
        shape = LoponShapes.button
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
        Text(text = "Продолжить", style = LoponTypography.button)
    }
}

@Composable
private fun VerticalStopButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(LoponDimens.buttonHeightMedium),
        colors = ButtonDefaults.buttonColors(
            containerColor = LoponColors.error,
            contentColor = LoponColors.white
        ),
        shape = LoponShapes.button
    ) {
        Icon(Icons.Filled.Stop, contentDescription = null)
        Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
        Text(text = "Завершить", style = LoponTypography.button)
    }
}

@Composable
internal fun CompactTripBar(
    metrics: TripMetrics,
    isRecording: Boolean,
    tripState: TripState,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    modifier: Modifier = Modifier,
    compactVerticalControls: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LoponDimens.screenPadding, vertical = LoponDimens.spacerSmall),
        verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactMetricItem(
                label = "Скорость",
                value = MetricsFormatter.formatSpeedKmh(metrics.currentSpeedMs),
                unit = "км/ч",
                highlight = isRecording
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(LoponColors.divider)
            )

            CompactMetricItem(
                label = "Дистанция",
                value = MetricsFormatter.formatDistanceKm(metrics.totalDistanceM),
                unit = "км"
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(LoponColors.divider)
            )

            CompactMetricItem(
                label = "Время",
                value = MetricsFormatter.formatTimeCompact(metrics.movingTimeMs),
                unit = ""
            )
        }

        TripControlButtons(
            tripState = tripState,
            onStartClick = onStartClick,
            onStopClick = onStopClick,
            onPauseClick = onPauseClick,
            onResumeClick = onResumeClick,
            verticalStack = compactVerticalControls
        )
    }
}

@Composable
private fun CompactMetricItem(
    label: String,
    value: String,
    unit: String,
    highlight: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = LoponTypography.caption.copy(fontSize = 10.sp),
            color = LoponColors.onSurfaceSecondary,
            maxLines = 1
        )
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                style = LoponTypography.metricValue.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                color = if (highlight) LoponColors.primaryYellow else LoponColors.onSurfacePrimary,
                maxLines = 1
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = LoponTypography.metricUnit.copy(fontSize = 10.sp),
                    color = LoponColors.onSurfaceSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
