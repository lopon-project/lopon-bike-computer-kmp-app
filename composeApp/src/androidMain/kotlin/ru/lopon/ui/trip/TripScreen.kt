package ru.lopon.ui.trip

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.lopon.R
import ru.lopon.domain.state.TripState
import ru.lopon.ui.components.ErrorCard
import ru.lopon.ui.components.SpeedPill
import ru.lopon.ui.components.StatusIndicatorRow
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography
import ru.lopon.ui.util.rememberIsLandscape

internal enum class TripDisplayMode {
    MAP,
    METRICS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripScreen(
    viewModel: TripViewModel,
    onOpenSensor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val activeRoute = when (val state = uiState.tripState) {
        is TripState.Recording -> state.route
        is TripState.Paused -> state.route
        else -> null
    }
    val activePosition = when (val state = uiState.tripState) {
        is TripState.Recording -> state.currentPosition ?: uiState.lastKnownLocation
        is TripState.Paused -> state.currentPosition ?: uiState.lastKnownLocation
        else -> uiState.lastKnownLocation
    }

    var autoCenterEnabled by rememberSaveable { mutableStateOf(true) }
    var centerNowToken by rememberSaveable { mutableStateOf(0) }
    var cameraMode by rememberSaveable { mutableStateOf(TripMapCameraMode.FOLLOW_POSITION) }
    var centerButtonEnabled by rememberSaveable { mutableStateOf(true) }
    var displayMode by rememberSaveable { mutableStateOf(TripDisplayMode.MAP) }
    val scope = rememberCoroutineScope()
    val isLandscape = rememberIsLandscape()

    LaunchedEffect(activeRoute) {
        if (activeRoute == null && cameraMode == TripMapCameraMode.ROUTE_OVERVIEW) {
            cameraMode = TripMapCameraMode.FOLLOW_POSITION
        }
    }

    val onToggleCameraMode: () -> Unit = {
        cameraMode = if (cameraMode == TripMapCameraMode.FOLLOW_POSITION)
            TripMapCameraMode.ROUTE_OVERVIEW else TripMapCameraMode.FOLLOW_POSITION
    }
    val onCenterNow: () -> Unit = {
        if (centerButtonEnabled) {
            centerButtonEnabled = false
            centerNowToken += 1
            scope.launch {
                delay(900)
                centerButtonEnabled = true
            }
        }
    }
    val onToggleAutoCenter: () -> Unit = { autoCenterEnabled = !autoCenterEnabled }

    if (isLandscape) {
        var isSidePanelOpen by rememberSaveable { mutableStateOf(true) }

        Row(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                TripMapCard(
                    route = activeRoute,
                    currentPosition = activePosition,
                    autoCenterEnabled = autoCenterEnabled,
                    centerNowToken = centerNowToken,
                    cameraMode = cameraMode,
                    recordedTrack = uiState.recordedTrackPoints,
                    modifier = Modifier.fillMaxSize()
                )

                CameraModeChip(
                    cameraMode = cameraMode,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(LoponDimens.spacerSmall)
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(LoponDimens.spacerSmall),
                    horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TripStatusChip(
                        isConnected = uiState.isConnected,
                        isRecording = uiState.isRecording,
                        isPaused = uiState.isPaused
                    )
                    TogglePanelButton(
                        isOpen = isSidePanelOpen,
                        onToggle = { isSidePanelOpen = !isSidePanelOpen }
                    )
                }

                MapOverlayControls(
                    cameraMode = cameraMode,
                    autoCenterEnabled = autoCenterEnabled,
                    centerButtonEnabled = centerButtonEnabled,
                    hasRoute = activeRoute != null,
                    hasPosition = activePosition != null,
                    onToggleMode = onToggleCameraMode,
                    onCenterNow = onCenterNow,
                    onToggleAutoCenter = onToggleAutoCenter,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )

                SpeedPill(
                    speedKmh = uiState.metrics.currentSpeedKmh,
                    isRecording = uiState.isRecording,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = LoponDimens.spacerMedium,
                            bottom = LoponDimens.spacerMedium
                        )
                )

                if (uiState.tripState is TripState.Idle) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.quickStartTrip() },
                        containerColor = LoponColors.primaryYellow,
                        contentColor = LoponColors.black,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(LoponDimens.spacerMedium)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            "Просто ехать",
                            style = LoponTypography.button,
                            color = LoponColors.black
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isSidePanelOpen,
                enter = slideInHorizontally(tween(250)) { it } + fadeIn(tween(200)),
                exit = slideOutHorizontally(tween(250)) { it } + fadeOut(tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .width(LoponDimens.sidePanelMinWidthLandscape)
                        .fillMaxHeight()
                        .background(LoponColors.backgroundLight)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = LoponDimens.spacerSmall,
                            vertical = LoponDimens.spacerSmall
                        ),
                    verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
                ) {
                    CompactMetricsRow(
                        metrics = uiState.metrics,
                        isRecording = uiState.isRecording,
                        modifier = Modifier.padding(vertical = LoponDimens.spacerSmall)
                    )

                    SecondaryMetricsGrid(metrics = uiState.metrics)

                    val routeProgressPercent = uiState.routeProgressPercent
                    val distanceToRouteEndM = uiState.distanceToRouteEndM
                    if (routeProgressPercent != null && distanceToRouteEndM != null) {
                        RouteProgressCard(
                            progressPercent = routeProgressPercent,
                            distanceToEndM = distanceToRouteEndM
                        )
                    }

                    TripControlButtons(
                        tripState = uiState.tripState,
                        onStartClick = { viewModel.openStartWizard() },
                        onStopClick = { viewModel.stopTrip() },
                        onPauseClick = { viewModel.pauseTrip() },
                        onResumeClick = { viewModel.resumeTrip() },
                        verticalStack = true
                    )

                    uiState.errorMessage?.let { error ->
                        ErrorCard(message = error, onDismiss = { viewModel.clearError() })
                    }
                }
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            TripHeader(
                isConnected = uiState.isConnected,
                isRecording = uiState.isRecording,
                isPaused = uiState.isPaused,
                displayMode = displayMode,
                onDisplayModeChanged = { displayMode = it },
                compact = false
            )

            if (displayMode == TripDisplayMode.MAP) {
                Box(modifier = Modifier.weight(1f)) {
                    TripMapCard(
                        route = activeRoute,
                        currentPosition = activePosition,
                        autoCenterEnabled = autoCenterEnabled,
                        centerNowToken = centerNowToken,
                        cameraMode = cameraMode,
                        recordedTrack = uiState.recordedTrackPoints,
                        modifier = Modifier.fillMaxSize()
                    )

                    CameraModeChip(
                        cameraMode = cameraMode,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(LoponDimens.spacerSmall)
                    )

                    MapOverlayControls(
                        cameraMode = cameraMode,
                        autoCenterEnabled = autoCenterEnabled,
                        centerButtonEnabled = centerButtonEnabled,
                        hasRoute = activeRoute != null,
                        hasPosition = activePosition != null,
                        onToggleMode = onToggleCameraMode,
                        onCenterNow = onCenterNow,
                        onToggleAutoCenter = onToggleAutoCenter,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )

                    SpeedPill(
                        speedKmh = uiState.metrics.currentSpeedKmh,
                        isRecording = uiState.isRecording,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(
                                start = LoponDimens.spacerMedium,
                                bottom = LoponDimens.spacerMedium
                            )
                    )

                    if (uiState.tripState is TripState.Idle) {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.quickStartTrip() },
                            containerColor = LoponColors.primaryYellow,
                            contentColor = LoponColors.black,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = LoponDimens.spacerMedium)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                "Просто ехать",
                                style = LoponTypography.button,
                                color = LoponColors.black
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LoponColors.backgroundLight)
                        .padding(
                            horizontal = LoponDimens.screenPadding,
                            vertical = LoponDimens.spacerSmall
                        ),
                    verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
                ) {
                    CompactTripBar(
                        metrics = uiState.metrics,
                        isRecording = uiState.isRecording,
                        tripState = uiState.tripState,
                        onStartClick = { viewModel.openStartWizard() },
                        onStopClick = { viewModel.stopTrip() },
                        onPauseClick = { viewModel.pauseTrip() },
                        onResumeClick = { viewModel.resumeTrip() }
                    )

                    val routeProgressPercent = uiState.routeProgressPercent
                    val distanceToRouteEndM = uiState.distanceToRouteEndM
                    if (routeProgressPercent != null && distanceToRouteEndM != null) {
                        RouteProgressCard(
                            progressPercent = routeProgressPercent,
                            distanceToEndM = distanceToRouteEndM
                        )
                    }

                    uiState.errorMessage?.let { error ->
                        ErrorCard(message = error, onDismiss = { viewModel.clearError() })
                    }
                }
            } else {
                SensorDashboard(
                    metrics = uiState.metrics,
                    isConnected = uiState.isConnected,
                    isRecording = uiState.isRecording,
                    isPaused = uiState.isPaused,
                    tripState = uiState.tripState,
                    onOpenSensor = onOpenSensor,
                    onStartTrip = { viewModel.openStartWizard() },
                    onQuickStart = { viewModel.quickStartTrip() },
                    onStopTrip = { viewModel.stopTrip() },
                    onPauseTrip = { viewModel.pauseTrip() },
                    onResumeTrip = { viewModel.resumeTrip() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    val summaryMetrics = uiState.summaryMetrics
    if (uiState.showTripSummary && summaryMetrics != null) {
        TripSummaryScreen(
            metrics = summaryMetrics,
            modeName = uiState.summaryModeName,
            onDismiss = { viewModel.dismissTripSummary() },
            onExportGpx = null // TODO: добавить экспорт
        )
    }

    if (uiState.isWizardOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeStartWizard() },
            modifier = if (isLandscape) Modifier.fillMaxWidth(0.8f) else Modifier
        ) {
            StartTripWizardSheet(
                uiState = uiState,
                onModeSelected = { viewModel.selectMode(it) },
                onRouteSelected = { viewModel.selectRoute(it) },
                onCreateQuickRoute = { viewModel.createQuickRoute() },
                isBleConnected = uiState.isConnected,
                onOpenSensor = onOpenSensor,
                onDismiss = { viewModel.closeStartWizard() },
                onStart = { viewModel.startTripFromWizard() }
            )
        }
    }
}

@Composable
private fun TripHeader(
    isConnected: Boolean,
    isRecording: Boolean,
    isPaused: Boolean,
    displayMode: TripDisplayMode,
    onDisplayModeChanged: (TripDisplayMode) -> Unit,
    compact: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LoponColors.black)
            .padding(
                horizontal = if (compact) LoponDimens.spacerSmall else LoponDimens.screenPadding,
                vertical = if (compact) LoponDimens.spacerSmall else LoponDimens.spacerMedium
            )
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
                Image(
                    painter = painterResource(id = R.drawable.ic_lopon_wolf),
                    contentDescription = "LOPON",
                    modifier = Modifier
                        .height(if (compact) 24.dp else 28.dp)
                        .padding(end = 2.dp)
                )
                Text(
                    text = "LOPON",
                    style = LoponTypography.brandTitle,
                    color = LoponColors.primaryYellow
                )
            }
            StatusIndicatorRow(
                isConnected = isConnected,
                isRecording = isRecording,
                isPaused = isPaused
            )
        }
        Spacer(modifier = Modifier.height(LoponDimens.spacerSmall))
        DisplayModeToggle(
            displayMode = displayMode,
            onModeChanged = onDisplayModeChanged
        )
        if (!compact) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(LoponColors.primaryYellow)
            )
        }
    }
}

@Composable
private fun SensorOnlyHint(
    isConnected: Boolean,
    onOpenSensor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = LoponDimens.spacerMedium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isConnected) "Режим без карты: показатели датчика"
            else "Подключите датчик для полноценного режима",
            style = LoponTypography.caption,
            color = LoponColors.onSurfaceSecondary
        )
        if (!isConnected) {
            OutlinedButton(
                onClick = onOpenSensor,
                shape = LoponShapes.button
            ) {
                Icon(Icons.Filled.Bluetooth, contentDescription = null)
                Text("Датчик", style = LoponTypography.button)
            }
        }
    }
}

@Composable
private fun TripStatusChip(
    isConnected: Boolean,
    isRecording: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = LoponColors.black.copy(alpha = 0.7f),
                shape = LoponShapes.small
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        StatusIndicatorRow(
            isConnected = isConnected,
            isRecording = isRecording,
            isPaused = isPaused
        )
    }
}

@Composable
private fun TogglePanelButton(
    isOpen: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledIconButton(
        onClick = onToggle,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = LoponColors.white.copy(alpha = 0.9f),
            contentColor = LoponColors.black
        ),
        modifier = modifier.size(LoponDimens.mapControlSize)
    ) {
        Icon(
            imageVector = if (isOpen) Icons.AutoMirrored.Filled.KeyboardArrowRight
            else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = if (isOpen) "Скрыть метрики" else "Показать метрики",
            modifier = Modifier.size(LoponDimens.iconSizeAction)
        )
    }
}
