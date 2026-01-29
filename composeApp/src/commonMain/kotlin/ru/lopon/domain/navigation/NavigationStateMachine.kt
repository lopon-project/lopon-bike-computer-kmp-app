package ru.lopon.domain.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.lopon.core.IdGenerator
import ru.lopon.core.TimeProvider
import ru.lopon.core.metrics.MetricsAggregator
import ru.lopon.domain.model.NavigationMode
import ru.lopon.platform.BleAdapter
import ru.lopon.platform.LocationProvider

class NavigationStateMachine(
    private val bleAdapter: BleAdapter,
    private val locationProvider: LocationProvider,
    private val metricsAggregator: MetricsAggregator,
    private val timeProvider: TimeProvider,
    private val idGenerator: IdGenerator,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<NavigationState>(NavigationState.Idle)
    val state: StateFlow<NavigationState> = _state.asStateFlow()

    private val _sideEffects = MutableSharedFlow<SideEffect>()
    val sideEffects: SharedFlow<SideEffect> = _sideEffects.asSharedFlow()

    private val mutex = Mutex()

    private var lastValidState: NavigationState? = null
    private var pendingEvent: NavigationEvent? = null


    suspend fun process(event: NavigationEvent): Result<NavigationState> = mutex.withLock {
        val currentState = _state.value
        if (!NavigationTransitions.isValidTransition(currentState, event)) {
            return Result.failure(
                InvalidTransitionException(
                    NavigationTransitions.describeInvalidTransition(currentState, event)
                )
            )
        }

        val (newState, effects) = reduce(currentState, event)

        if (currentState !is NavigationState.Error) {
            lastValidState = currentState
        }

        effects.forEach { effect ->
            try {
                _sideEffects.emit(effect)
            } catch (e: Exception) {
            }
        }

        _state.value = newState
        return Result.success(newState)
    }

    private fun reduce(
        state: NavigationState,
        event: NavigationEvent
    ): Pair<NavigationState, List<SideEffect>> {
        return when {
            state is NavigationState.Idle && event is NavigationEvent.StartRequested -> {
                val initState = NavigationState.Initializing(
                    mode = event.mode,
                    route = event.route,
                    wheelCircumferenceMm = event.wheelCircumferenceMm,
                    progress = InitProgress.STARTING,
                    bleStatus = if (event.mode != NavigationMode.Gps)
                        BleInitStatus.PENDING else BleInitStatus.NOT_REQUIRED,
                    gpsStatus = if (event.mode != NavigationMode.Sensor)
                        GpsInitStatus.PENDING else GpsInitStatus.NOT_REQUIRED
                )

                val effects = mutableListOf<SideEffect>()
                if (event.mode != NavigationMode.Gps) {
                    effects.add(SideEffect.ConnectBle())
                }
                if (event.mode != NavigationMode.Sensor) {
                    effects.add(SideEffect.StartGps)
                }

                initState to effects
            }

            state is NavigationState.Initializing && event is NavigationEvent.BleConnected -> {
                val newState = state.copy(
                    bleStatus = BleInitStatus.CONNECTED,
                    progress = calculateProgress(state.copy(bleStatus = BleInitStatus.CONNECTED))
                )
                newState to emptyList()
            }

            state is NavigationState.Initializing && event is NavigationEvent.BleConnectionFailed -> {
                val newState = state.copy(
                    bleStatus = BleInitStatus.FAILED,
                    progress = InitProgress.CONNECTING_BLE
                )
                val effects = listOf(
                    SideEffect.ShowErrorDialog(
                        error = NavigationError.BleConnectionError("Failed to connect to sensor"),
                        actions = listOf(
                            ErrorAction("Retry", NavigationEvent.RetryRequested),
                            ErrorAction("Continue without sensor", NavigationEvent.ContinueWithoutFeature),
                            ErrorAction("Cancel", NavigationEvent.AbortRequested)
                        )
                    )
                )
                newState to effects
            }

            state is NavigationState.Initializing && event is NavigationEvent.GpsAcquired -> {
                val newState = state.copy(
                    gpsStatus = GpsInitStatus.ACQUIRED,
                    progress = calculateProgress(state.copy(gpsStatus = GpsInitStatus.ACQUIRED))
                )
                newState to emptyList()
            }

            state is NavigationState.Initializing && event is NavigationEvent.InitializationComplete -> {
                val tripId = idGenerator.generateId()
                val recordingState = NavigationState.Recording(
                    tripId = tripId,
                    mode = state.mode,
                    route = state.route,
                    startTimeUtc = timeProvider.currentTimeMillis()
                )
                recordingState to listOf(
                    SideEffect.CreateTrip(tripId, state.mode),
                    SideEffect.StartMetrics
                )
            }

            state is NavigationState.Initializing && event is NavigationEvent.AbortRequested -> {
                val effects = mutableListOf<SideEffect>()
                if (state.bleStatus == BleInitStatus.CONNECTED) {
                    effects.add(SideEffect.DisconnectBle)
                }
                if (state.gpsStatus == GpsInitStatus.ACQUIRED) {
                    effects.add(SideEffect.StopGps)
                }
                NavigationState.Idle to effects
            }

            state is NavigationState.Recording && event is NavigationEvent.PauseRequested -> {
                val pausedState = NavigationState.Paused(
                    tripId = state.tripId,
                    mode = state.mode,
                    route = state.route,
                    pausedAtUtc = timeProvider.currentTimeMillis(),
                    accumulatedMetrics = metricsAggregator.currentMetrics
                )
                pausedState to listOf(SideEffect.PauseMetrics)
            }

            state is NavigationState.Paused && event is NavigationEvent.ResumeRequested -> {
                val recordingState = NavigationState.Recording(
                    tripId = state.tripId,
                    mode = state.mode,
                    route = state.route,
                    startTimeUtc = timeProvider.currentTimeMillis()
                )
                recordingState to listOf(SideEffect.ResumeMetrics)
            }

            (state is NavigationState.Recording || state is NavigationState.Paused)
                    && event is NavigationEvent.StopRequested -> {
                val tripId = when (state) {
                    is NavigationState.Recording -> state.tripId
                    is NavigationState.Paused -> state.tripId
                }
                val finishedState = NavigationState.Finished(
                    tripId = tripId,
                    finalMetrics = metricsAggregator.currentMetrics,
                    savedSuccessfully = true
                )
                finishedState to listOf(
                    SideEffect.StopMetrics,
                    SideEffect.SaveTrip(tripId),
                    SideEffect.DisconnectBle,
                    SideEffect.StopGps
                )
            }

            state is NavigationState.Recording && event is NavigationEvent.ModeChangeRequested -> {
                val newState = state.copy(mode = event.newMode)
                val effects = mutableListOf<SideEffect>()

                when (event.newMode) {
                    NavigationMode.Sensor -> {
                        effects.add(SideEffect.StopGps)
                        if (bleAdapter.connectionState.value !is ru.lopon.platform.BleConnectionState.Connected) {
                            effects.add(SideEffect.ConnectBle())
                        }
                    }

                    NavigationMode.Gps -> {
                        effects.add(SideEffect.DisconnectBle)
                        effects.add(SideEffect.StartGps)
                    }

                    NavigationMode.Hybrid -> {
                        effects.add(SideEffect.StartGps)
                        if (bleAdapter.connectionState.value !is ru.lopon.platform.BleConnectionState.Connected) {
                            effects.add(SideEffect.ConnectBle())
                        }
                    }
                }

                newState to effects
            }

            event is NavigationEvent.ErrorOccurred -> {
                val errorState = NavigationState.Error(
                    error = event.error,
                    canRetry = true,
                    canContinueWithoutFeature = event.error is NavigationError.BleConnectionError
                            || event.error is NavigationError.BleDisconnected,
                    affectedFeature = when (event.error) {
                        is NavigationError.BleConnectionError -> "BLE Sensor"
                        is NavigationError.BleDisconnected -> "BLE Sensor"
                        is NavigationError.GpsUnavailable -> "GPS"
                        is NavigationError.GpsLost -> "GPS"
                        else -> null
                    }
                )
                errorState to listOf(
                    SideEffect.ShowErrorDialog(
                        error = event.error,
                        actions = buildErrorActions(errorState)
                    )
                )
            }

            state is NavigationState.Finished && event is NavigationEvent.StartRequested -> {
                reduce(NavigationState.Idle, event)
            }

            else -> state to emptyList()
        }
    }

    private fun calculateProgress(state: NavigationState.Initializing): InitProgress {
        val bleReady = state.bleStatus in setOf(BleInitStatus.CONNECTED, BleInitStatus.NOT_REQUIRED)
        val gpsReady = state.gpsStatus in setOf(GpsInitStatus.ACQUIRED, GpsInitStatus.NOT_REQUIRED)

        return when {
            bleReady && gpsReady -> InitProgress.READY
            !bleReady -> InitProgress.CONNECTING_BLE
            !gpsReady -> InitProgress.ACQUIRING_GPS
            else -> InitProgress.STARTING
        }
    }

    private fun buildErrorActions(errorState: NavigationState.Error): List<ErrorAction> {
        val actions = mutableListOf<ErrorAction>()

        if (errorState.canRetry) {
            actions.add(ErrorAction("Retry", NavigationEvent.RetryRequested))
        }
        if (errorState.canContinueWithoutFeature) {
            actions.add(
                ErrorAction(
                    "Continue without ${errorState.affectedFeature}",
                    NavigationEvent.ContinueWithoutFeature
                )
            )
        }
        actions.add(ErrorAction("Cancel", NavigationEvent.AbortRequested))

        return actions
    }


    suspend fun reset() = mutex.withLock {
        _state.value = NavigationState.Idle
        lastValidState = null
        pendingEvent = null
    }

    fun canProcess(event: NavigationEvent): Boolean {
        return NavigationTransitions.isValidTransition(_state.value, event)
    }
}
