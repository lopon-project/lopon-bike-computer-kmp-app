package ru.lopon.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.lopon.domain.state.TripState
import ru.lopon.ui.history.HistoryScreen
import ru.lopon.ui.history.HistoryViewModel
import ru.lopon.ui.more.MoreScreen
import ru.lopon.ui.offline.OfflineMapsScreen
import ru.lopon.ui.offline.OfflineMapsViewModel
import ru.lopon.ui.routes.RouteWizardScreen
import ru.lopon.ui.routes.RouteWizardViewModel
import ru.lopon.ui.routes.RoutesScreen
import ru.lopon.ui.routes.RoutesViewModel
import ru.lopon.ui.sensor.SensorScreen
import ru.lopon.ui.sensor.SensorTestScreen
import ru.lopon.ui.sensor.SensorTestViewModel
import ru.lopon.ui.sensor.SensorViewModel
import ru.lopon.ui.settings.SettingsScreen
import ru.lopon.ui.settings.SettingsViewModel
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.trip.TripScreen
import ru.lopon.ui.trip.TripViewModel
import ru.lopon.ui.util.rememberIsLandscape

private sealed class OverlayScreen {
    data object None : OverlayScreen()
    data object Settings : OverlayScreen()
    data object OfflineMaps : OverlayScreen()
    data object RouteWizard : OverlayScreen()
    data object Diagnostics : OverlayScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    sensorViewModel: SensorViewModel,
    tripViewModel: TripViewModel,
    historyViewModel: HistoryViewModel,
    routesViewModel: RoutesViewModel,
    settingsViewModel: SettingsViewModel,
    routeWizardViewModel: RouteWizardViewModel,
    offlineMapsViewModel: OfflineMapsViewModel,
    sensorTestViewModel: SensorTestViewModel,
    modifier: Modifier = Modifier
) {
    val screens = listOf(Screen.Home, Screen.History, Screen.Routes, Screen.More)
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var isSensorSheetOpen by remember { mutableStateOf(false) }
    var overlayScreen by remember { mutableStateOf<OverlayScreen>(OverlayScreen.None) }

    AnimatedContent(
        targetState = overlayScreen,
        transitionSpec = {
            if (targetState is OverlayScreen.None) {
                (fadeIn(tween(200)) togetherWith
                        slideOutHorizontally(tween(300)) { it / 2 } + fadeOut(tween(200)))
            } else {
                (slideInHorizontally(tween(300)) { it / 2 } + fadeIn(tween(200)) togetherWith
                        fadeOut(tween(200)))
            }
        },
        label = "overlay_transition"
    ) { overlay ->
        when (overlay) {
            is OverlayScreen.Settings -> {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { overlayScreen = OverlayScreen.None },
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                )
            }

            is OverlayScreen.OfflineMaps -> {
                OfflineMapsScreen(
                    viewModel = offlineMapsViewModel,
                    onBack = { overlayScreen = OverlayScreen.None },
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                )
            }

            is OverlayScreen.RouteWizard -> {
                RouteWizardScreen(
                    viewModel = routeWizardViewModel,
                    onBack = { overlayScreen = OverlayScreen.None },
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                )
            }

            is OverlayScreen.Diagnostics -> {
                SensorTestScreen(
                    viewModel = sensorTestViewModel,
                    onBack = { overlayScreen = OverlayScreen.None },
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                )
            }

            is OverlayScreen.None -> {
                val isLandscape = rememberIsLandscape()

                val tripUiState by tripViewModel.uiState.collectAsState()
                val isTripActive = tripUiState.tripState is TripState.Recording ||
                        tripUiState.tripState is TripState.Paused
                var isRailManuallyOpen by rememberSaveable { mutableStateOf(false) }
                val showRail = !isTripActive || isRailManuallyOpen

                LaunchedEffect(isTripActive) {
                    if (!isTripActive) isRailManuallyOpen = false
                }

                val tabContent: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit =
                    { paddingValues ->
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                            },
                            label = "tab_transition"
                        ) { screen ->
                            when (screen) {
                                Screen.Home -> TripScreen(
                                    viewModel = tripViewModel,
                                    onOpenSensor = { isSensorSheetOpen = true },
                                    modifier = Modifier.padding(paddingValues)
                                )

                                Screen.History -> HistoryScreen(
                                    viewModel = historyViewModel,
                                    modifier = Modifier.padding(paddingValues)
                                )

                                Screen.Routes -> RoutesScreen(
                                    viewModel = routesViewModel,
                                    onUseForStart = { routeId ->
                                        tripViewModel.openStartWizardWithRoute(routeId)
                                        currentScreen = Screen.Home
                                    },
                                    onOpenWizard = {
                                        routeWizardViewModel.resetState()
                                        overlayScreen = OverlayScreen.RouteWizard
                                    },
                                    modifier = Modifier.padding(paddingValues)
                                )

                                Screen.More -> MoreScreen(
                                    onOpenSensor = { isSensorSheetOpen = true },
                                    onOpenSettings = { overlayScreen = OverlayScreen.Settings },
                                    onOpenOfflineMaps = { overlayScreen = OverlayScreen.OfflineMaps },
                                    onOpenDiagnostics = { overlayScreen = OverlayScreen.Diagnostics },
                                    modifier = Modifier.padding(paddingValues)
                                )
                            }
                        }
                    }

                if (isLandscape) {
                    Box(modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxSize()) {
                            AnimatedVisibility(
                                visible = showRail,
                                enter = slideInHorizontally(tween(250)) { -it } + fadeIn(tween(200)),
                                exit = slideOutHorizontally(tween(250)) { -it } + fadeOut(tween(200))
                            ) {
                                NavigationRail(
                                    containerColor = LoponColors.navBarBackground,
                                    contentColor = LoponColors.navBarSelected,
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .systemBarsPadding()
                                        .displayCutoutPadding()
                                ) {
                                    if (isTripActive) {
                                        IconButton(onClick = { isRailManuallyOpen = false }) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Скрыть меню",
                                                tint = LoponColors.navBarUnselected
                                            )
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    screens.forEach { screen ->
                                        NavigationRailItem(
                                            icon = {
                                                Icon(
                                                    imageVector = screen.icon,
                                                    contentDescription = screen.title,
                                                    modifier = Modifier.size(LoponDimens.iconSizeNav)
                                                )
                                            },
                                            selected = currentScreen.route == screen.route,
                                            onClick = {
                                                currentScreen = screen
                                                if (isTripActive) isRailManuallyOpen = false
                                            },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = LoponColors.navBarSelected,
                                                selectedTextColor = LoponColors.navBarSelected,
                                                unselectedIconColor = LoponColors.navBarUnselected,
                                                unselectedTextColor = LoponColors.navBarUnselected,
                                                indicatorColor = LoponColors.navBarIndicator.copy(alpha = 0.22f)
                                            )
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                            Scaffold(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth()
                            ) { paddingValues ->
                                tabContent(paddingValues)
                            }
                        }

                        AnimatedVisibility(
                            visible = isTripActive && !isRailManuallyOpen,
                            enter = fadeIn(tween(200)) + slideInHorizontally(tween(250)) { -it },
                            exit = fadeOut(tween(150)),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .systemBarsPadding()
                                .padding(LoponDimens.spacerSmall)
                        ) {
                            SmallFloatingActionButton(
                                onClick = { isRailManuallyOpen = true },
                                containerColor = LoponColors.black.copy(alpha = 0.7f),
                                contentColor = LoponColors.white
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Меню"
                                )
                            }
                        }
                    }
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = LoponColors.navBarBackground,
                                contentColor = LoponColors.navBarSelected
                            ) {
                                screens.forEach { screen ->
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.title
                                            )
                                        },
                                        label = { Text(screen.title) },
                                        selected = currentScreen.route == screen.route,
                                        onClick = { currentScreen = screen },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = LoponColors.navBarSelected,
                                            selectedTextColor = LoponColors.navBarSelected,
                                            unselectedIconColor = LoponColors.navBarUnselected,
                                            unselectedTextColor = LoponColors.navBarUnselected,
                                            indicatorColor = LoponColors.navBarIndicator.copy(alpha = 0.22f)
                                        )
                                    )
                                }
                            }
                        },
                        modifier = modifier.fillMaxSize()
                    ) { paddingValues ->
                        tabContent(paddingValues)
                    }
                }
            }
        }
    }

    if (isSensorSheetOpen) {
        val isLandscapeSheet = rememberIsLandscape()
        ModalBottomSheet(
            onDismissRequest = { isSensorSheetOpen = false },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = if (isLandscapeSheet) Modifier.fillMaxWidth(0.8f) else Modifier
        ) {
            SensorScreen(viewModel = sensorViewModel)
        }
    }
}
