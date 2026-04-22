package ru.lopon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ru.lopon.domain.model.Settings
import androidx.compose.ui.Modifier
import ru.lopon.ui.theme.LoponTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.maplibre.android.MapLibre
import ru.lopon.di.AndroidAppContainer
import ru.lopon.ui.history.HistoryViewModel
import ru.lopon.ui.navigation.AppNavigation
import ru.lopon.ui.offline.OfflineMapsViewModel
import ru.lopon.ui.routes.RouteWizardViewModel
import ru.lopon.ui.routes.RoutesViewModel
import ru.lopon.ui.sensor.SensorTestViewModel
import ru.lopon.ui.sensor.SensorViewModel
import ru.lopon.ui.settings.SettingsViewModel
import ru.lopon.ui.splash.SplashScreen
import ru.lopon.ui.trip.TripViewModel

class MainActivity : ComponentActivity() {

    private lateinit var container: AndroidAppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(this)

        container = AndroidAppContainer.getInstance(applicationContext)
        container.permissionsManager.registerActivity(this)

        setContent {
            val settings by container.settingsRepository.getSettings()
                .collectAsState(initial = Settings.DEFAULT)
            LoponTheme(themeMode = settings.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { }

                    LaunchedEffect(showSplash) {
                        if (!showSplash) {
                            permissionLauncher.launch(
                                container.permissionsManager.getAllRequiredPermissions()
                            )
                        }
                    }

                    AnimatedContent(
                        targetState = showSplash,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "splash"
                    ) { isSplash ->
                        if (isSplash) {
                            SplashScreen(onFinished = { showSplash = false })
                        } else {
                            val sensorViewModel: SensorViewModel = viewModel(
                                factory = SensorViewModel.Factory(
                                    application = application,
                                    container = container
                                )
                            )

                            val tripViewModel: TripViewModel = viewModel(
                                factory = TripViewModel.Factory(
                                    application = application,
                                    container = container
                                )
                            )

                            val historyViewModel: HistoryViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer { HistoryViewModel(container.tripRepository) }
                                }
                            )

                            val routesViewModel: RoutesViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer {
                                        RoutesViewModel(
                                            routeRepository = container.routeRepository,
                                            createRouteUseCase = container.createRouteUseCase
                                        )
                                    }
                                }
                            )

                            val settingsViewModel: SettingsViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer { SettingsViewModel(container.settingsRepository) }
                                }
                            )

                            val routeWizardViewModel: RouteWizardViewModel = viewModel(
                                factory = RouteWizardViewModel.Factory(
                                    application = application,
                                    container = container
                                )
                            )

                            val offlineMapsViewModel: OfflineMapsViewModel = viewModel(
                                factory = OfflineMapsViewModel.Factory(
                                    application = application,
                                    container = container
                                )
                            )

                            val sensorTestViewModel: SensorTestViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer {
                                        SensorTestViewModel(
                                            bleAdapter = container.bleAdapter,
                                            permissionsManager = container.permissionsManager,
                                            settingsRepository = container.settingsRepository
                                        )
                                    }
                                }
                            )

                            AppNavigation(
                                sensorViewModel = sensorViewModel,
                                tripViewModel = tripViewModel,
                                historyViewModel = historyViewModel,
                                routesViewModel = routesViewModel,
                                settingsViewModel = settingsViewModel,
                                routeWizardViewModel = routeWizardViewModel,
                                offlineMapsViewModel = offlineMapsViewModel,
                                sensorTestViewModel = sensorTestViewModel
                            )
                        }
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        container.permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        container.permissionsManager.unregisterActivity()
        super.onDestroy()
    }
}
