package ru.lopon.ui.trip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.lopon.R
import ru.lopon.core.metrics.MetricsFormatter
import ru.lopon.core.metrics.TripMetrics
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography

@Composable
fun TripSummaryScreen(
    metrics: TripMetrics,
    modeName: String,
    onDismiss: () -> Unit,
    onExportGpx: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showHeader by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showHeader = true
        delay(300)
        showStats = true
        delay(300)
        showActions = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LoponColors.black)
            .verticalScroll(rememberScrollState())
            .padding(LoponDimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(
            visible = showHeader,
            enter = fadeIn(tween(500)) + slideInVertically(
                tween(500),
                initialOffsetY = { -it / 3 }
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.ic_lopon_wolf),
                    contentDescription = "LOPON",
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Поездка завершена!",
                    style = LoponTypography.screenTitle,
                    color = LoponColors.primaryYellow,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Режим: $modeName",
                    style = LoponTypography.caption,
                    color = LoponColors.white.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = showStats,
            enter = fadeIn(tween(400)) + slideInVertically(
                tween(400),
                initialOffsetY = { it / 4 }
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = LoponShapes.card,
                    colors = CardDefaults.cardColors(
                        containerColor = LoponColors.primaryYellow.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = MetricsFormatter.formatDistanceKm(metrics.totalDistanceM),
                            style = LoponTypography.heroSpeed.copy(fontSize = 56.sp),
                            color = LoponColors.primaryYellow
                        )
                        Text(
                            text = "километров",
                            style = LoponTypography.body,
                            color = LoponColors.primaryYellow.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
                ) {
                    SummaryStatCard(
                        label = "Время в движении",
                        value = MetricsFormatter.formatTimeCompact(metrics.movingTimeMs),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        label = "Общее время",
                        value = MetricsFormatter.formatTimeCompact(metrics.elapsedTimeMs),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
                ) {
                    SummaryStatCard(
                        label = "Средняя скорость",
                        value = MetricsFormatter.formatSpeedKmh(metrics.averageSpeedMs),
                        unit = "км/ч",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        label = "Макс. скорость",
                        value = MetricsFormatter.formatSpeedKmh(metrics.maxSpeedMs),
                        unit = "км/ч",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
                ) {
                    SummaryStatCard(
                        label = "Набор высоты",
                        value = MetricsFormatter.formatElevationGain(metrics.elevationGainM),
                        unit = "м",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        label = "Каденс (ср.)",
                        value = metrics.averageCadenceRpm?.let { "${it.toInt()}" } ?: "—",
                        unit = if (metrics.averageCadenceRpm != null) "об/мин" else "",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = showActions,
            enter = fadeIn(tween(400)) + slideInVertically(
                tween(400),
                initialOffsetY = { it / 3 }
            )
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
            ) {
                if (onExportGpx != null) {
                    OutlinedButton(
                        onClick = onExportGpx,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(LoponDimens.buttonHeightMedium),
                        shape = LoponShapes.button
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = null,
                            tint = LoponColors.primaryYellow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
                        Text(
                            "Экспорт GPX",
                            style = LoponTypography.button,
                            color = LoponColors.primaryYellow
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LoponDimens.buttonHeightLarge),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoponColors.primaryYellow,
                        contentColor = LoponColors.black
                    ),
                    shape = LoponShapes.button
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
                    Text("Готово", style = LoponTypography.buttonLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SummaryStatCard(
    label: String,
    value: String,
    unit: String = "",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = LoponShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = LoponColors.white.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LoponDimens.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = LoponTypography.caption,
                color = LoponColors.white.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = LoponTypography.metricValue.copy(fontSize = 22.sp),
                    color = LoponColors.white,
                    maxLines = 1
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = unit,
                        style = LoponTypography.metricUnit.copy(fontSize = 11.sp),
                        color = LoponColors.white.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
