package ru.lopon.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.lopon.domain.model.NavigationMode
import ru.lopon.domain.model.Route
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography

@Composable
internal fun StartTripWizardSheet(
    uiState: TripUiState,
    onModeSelected: (NavigationMode) -> Unit,
    onRouteSelected: (String) -> Unit,
    onCreateQuickRoute: () -> Unit,
    isBleConnected: Boolean,
    onOpenSensor: () -> Unit,
    onDismiss: () -> Unit,
    onStart: () -> Unit
) {
    var step by rememberSaveable { mutableStateOf(1) }
    val selectedRoute = uiState.availableRoutes.firstOrNull { it.id == uiState.selectedRouteId }
    val canStart = uiState.selectedMode !is NavigationMode.Sensor || selectedRoute != null
    val needsRoute = uiState.selectedMode is NavigationMode.Sensor
    val needsBle = uiState.selectedMode is NavigationMode.Sensor || uiState.selectedMode is NavigationMode.Hybrid

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LoponDimens.screenPadding, vertical = LoponDimens.spacerSmall),
        verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
    ) {
        Text(
            text = "Подготовка поездки",
            style = LoponTypography.sectionTitle,
            color = LoponColors.onSurfacePrimary
        )

        WizardStepIndicator(currentStep = step)

        when (step) {
            1 -> WizardStepRoute(
                routes = uiState.availableRoutes,
                selectedRouteId = uiState.selectedRouteId,
                isCreatingRoute = uiState.isCreatingRoute,
                onRouteSelected = onRouteSelected,
                onCreateQuickRoute = onCreateQuickRoute
            )
            2 -> WizardStepMode(
                selectedMode = uiState.selectedMode,
                isBleConnected = isBleConnected,
                needsBle = needsBle,
                onModeSelected = onModeSelected,
                onOpenSensor = onOpenSensor
            )
            else -> WizardStepConfirm(
                selectedMode = uiState.selectedMode,
                selectedRoute = selectedRoute,
                needsRoute = needsRoute,
                needsBle = needsBle,
                isBleConnected = isBleConnected,
                canStart = canStart
            )
        }

        WizardButtons(
            step = step,
            canStart = canStart,
            onBack = { step -= 1 },
            onNext = { step += 1 },
            onDismiss = onDismiss,
            onStart = onStart
        )

        Spacer(modifier = Modifier.height(LoponDimens.spacerSmall))
    }
}

@Composable
private fun WizardStepIndicator(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..3) {
            Box(
                modifier = Modifier
                    .size(if (i == currentStep) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (i <= currentStep) LoponColors.primaryYellow
                        else LoponColors.divider
                    )
            )
            if (i < 3) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(
                            if (i < currentStep) LoponColors.primaryYellow
                            else LoponColors.divider
                        )
                )
            }
        }
    }
}

@Composable
private fun WizardStepRoute(
    routes: List<Route>,
    selectedRouteId: String?,
    isCreatingRoute: Boolean,
    onRouteSelected: (String) -> Unit,
    onCreateQuickRoute: () -> Unit
) {
    Text("Выбор маршрута", style = LoponTypography.body, color = LoponColors.onSurfacePrimary)

    if (routes.isEmpty()) {
        Text(
            "Маршрутов пока нет. Создайте базовый маршрут:",
            style = LoponTypography.caption,
            color = LoponColors.onSurfaceSecondary
        )
        Button(
            onClick = onCreateQuickRoute,
            enabled = !isCreatingRoute,
            colors = ButtonDefaults.buttonColors(
                containerColor = LoponColors.primaryYellow,
                contentColor = LoponColors.black
            ),
            shape = LoponShapes.button
        ) {
            Text(
                if (isCreatingRoute) "Построение..." else "Создать демо-маршрут",
                style = LoponTypography.button
            )
        }
    }

    if (routes.isNotEmpty()) {
        RouteSelectionList(
            routes = routes,
            selectedRouteId = selectedRouteId,
            onRouteSelected = onRouteSelected
        )
    }

    Text(
        text = routes.firstOrNull { it.id == selectedRouteId }
            ?.let { "Выбран: ${it.name} (${it.points.size} точек)" }
            ?: "Маршрут не выбран",
        style = LoponTypography.caption,
        color = LoponColors.onSurfaceSecondary
    )
}

@Composable
private fun RouteSelectionList(
    routes: List<Route>,
    selectedRouteId: String?,
    onRouteSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 150.dp),
        verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
    ) {
        items(routes) { route ->
            val isSelected = route.id == selectedRouteId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(LoponShapes.small)
                    .background(
                        if (isSelected) LoponColors.primaryYellow.copy(alpha = 0.12f)
                        else LoponColors.surfaceCard
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) LoponColors.primaryYellow else LoponColors.divider,
                        shape = LoponShapes.small
                    )
                    .clickable { onRouteSelected(route.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) LoponColors.primaryYellow else LoponColors.divider)
                )
                Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
                Text(
                    "${route.name} (${route.points.size} точек)",
                    style = LoponTypography.body,
                    color = LoponColors.onSurfacePrimary
                )
            }
        }
    }
}

@Composable
private fun WizardStepMode(
    selectedMode: NavigationMode,
    isBleConnected: Boolean,
    needsBle: Boolean,
    onModeSelected: (NavigationMode) -> Unit,
    onOpenSensor: () -> Unit
) {
    Text("Выбор режима", style = LoponTypography.body, color = LoponColors.onSurfacePrimary)

    Row(
        horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall),
        modifier = Modifier.fillMaxWidth()
    ) {
        ModeChip(
            title = "Sensor",
            icon = Icons.Filled.Sensors,
            selected = selectedMode is NavigationMode.Sensor,
            onClick = { onModeSelected(NavigationMode.Sensor) },
            modifier = Modifier.weight(1f)
        )
        ModeChip(
            title = "Hybrid",
            icon = Icons.Filled.Bluetooth,
            selected = selectedMode is NavigationMode.Hybrid,
            onClick = { onModeSelected(NavigationMode.Hybrid) },
            modifier = Modifier.weight(1f)
        )
        ModeChip(
            title = "GPS",
            icon = Icons.Filled.GpsFixed,
            selected = selectedMode is NavigationMode.Gps,
            onClick = { onModeSelected(NavigationMode.Gps) },
            modifier = Modifier.weight(1f)
        )
    }

    Text(
        text = when (selectedMode) {
            is NavigationMode.Sensor -> "Sensor: требуется маршрут и BLE"
            is NavigationMode.Hybrid -> "Hybrid: BLE желателен, маршрут опционален"
            is NavigationMode.Gps -> "GPS: работает без BLE, маршрут опционален"
        },
        style = LoponTypography.caption,
        color = LoponColors.onSurfaceSecondary
    )

    if (needsBle && !isBleConnected) {
        OutlinedButton(
            onClick = onOpenSensor,
            shape = LoponShapes.button
        ) {
            Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
            Text("Подключить датчик", style = LoponTypography.button)
        }
    }
}

@Composable
private fun ModeChip(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, style = LoponTypography.caption)
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = LoponColors.primaryYellow,
            selectedLabelColor = LoponColors.black,
            selectedLeadingIconColor = LoponColors.black
        ),
        modifier = modifier
    )
}

@Composable
private fun WizardStepConfirm(
    selectedMode: NavigationMode,
    selectedRoute: Route?,
    needsRoute: Boolean,
    needsBle: Boolean,
    isBleConnected: Boolean,
    canStart: Boolean
) {
    Text("Подтверждение", style = LoponTypography.body, color = LoponColors.onSurfacePrimary)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Режим: ${
                when (selectedMode) {
                    is NavigationMode.Sensor -> "Sensor"
                    is NavigationMode.Hybrid -> "Hybrid"
                    is NavigationMode.Gps -> "GPS"
                }
            }",
            style = LoponTypography.body,
            color = LoponColors.onSurfacePrimary
        )
        Text(
            text = selectedRoute?.let { "Маршрут: ${it.name}" } ?: "Маршрут: не выбран",
            style = LoponTypography.body,
            color = LoponColors.onSurfacePrimary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)) {
            StatusChip(
                label = "Маршрут",
                ok = !needsRoute || selectedRoute != null
            )
            StatusChip(
                label = "BLE",
                ok = !needsBle || isBleConnected
            )
        }
        Text(
            text = if (canStart) "Готово к старту" else "Для Sensor режима сначала выберите маршрут",
            style = LoponTypography.caption,
            color = if (canStart) LoponColors.success else LoponColors.warning
        )
    }
}

@Composable
private fun StatusChip(label: String, ok: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(LoponShapes.small)
            .background(if (ok) LoponColors.success.copy(alpha = 0.1f) else LoponColors.warning.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (ok) LoponColors.success else LoponColors.warning)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "$label: ${if (ok) "OK" else "нужен"}",
            style = LoponTypography.caption,
            color = if (ok) LoponColors.success else LoponColors.warning
        )
    }
}

@Composable
private fun WizardButtons(
    step: Int,
    canStart: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    onStart: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = LoponDimens.spacerSmall),
        horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            shape = LoponShapes.button
        ) {
            Text("Отмена", style = LoponTypography.button)
        }
        if (step > 1) {
            OutlinedButton(
                onClick = onBack,
                shape = LoponShapes.button
            ) {
                Text("Назад", style = LoponTypography.button)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (step < 3) {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoponColors.primaryYellow,
                    contentColor = LoponColors.black
                ),
                shape = LoponShapes.button
            ) {
                Text("Далее", style = LoponTypography.button)
            }
        } else {
            Button(
                onClick = onStart,
                enabled = canStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoponColors.primaryYellow,
                    contentColor = LoponColors.black
                ),
                shape = LoponShapes.button
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Старт", style = LoponTypography.buttonLarge)
            }
        }
    }
}
