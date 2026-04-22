package ru.lopon.ui.routes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.lopon.core.metrics.MetricsFormatter
import ru.lopon.domain.model.Route
import ru.lopon.ui.components.RoutePreviewMiniMap
import ru.lopon.ui.components.ScreenHeader
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography

@Composable
fun RoutesScreen(
    viewModel: RoutesViewModel,
    onUseForStart: (String) -> Unit,
    onOpenWizard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedRoute = uiState.routes.firstOrNull { it.id == uiState.selectedRouteId }
    val routePendingDelete = uiState.routes.firstOrNull { it.id == uiState.pendingDeleteRouteId }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = "Маршруты")

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = LoponDimens.contentMaxWidthCapped)
                    .fillMaxSize()
                    .padding(LoponDimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = LoponShapes.card,
                    colors = CardDefaults.cardColors(containerColor = LoponColors.surfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(LoponDimens.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
                    ) {
                        Text(
                            text = "Планирование маршрута",
                            style = LoponTypography.body,
                            fontWeight = FontWeight.SemiBold,
                            color = LoponColors.onSurfacePrimary
                        )
                        Text(
                            text = "Создание маршрута, импорт GPX, предпросмотр и сохранение.",
                            style = LoponTypography.caption,
                            color = LoponColors.onSurfaceSecondary
                        )
                        Button(
                            onClick = onOpenWizard,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LoponColors.primaryYellow,
                                contentColor = LoponColors.black
                            ),
                            shape = LoponShapes.button
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Создать маршрут", style = LoponTypography.button)
                        }
                    }
                }

                if (selectedRoute != null) {
                    RouteDetailsCard(
                        route = selectedRoute,
                        onBack = viewModel::closeRouteDetails,
                        onDelete = { viewModel.requestDeleteRoute(selectedRoute.id) },
                        onUseForStart = { onUseForStart(selectedRoute.id) }
                    )
                } else if (uiState.routes.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = LoponShapes.card
                    ) {
                        Text(
                            text = "Маршруты пока не созданы",
                            modifier = Modifier.padding(LoponDimens.cardPadding),
                            style = LoponTypography.body,
                            color = LoponColors.onSurfaceSecondary
                        )
                    }
                } else {
                    uiState.routes.forEach { route ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openRouteDetails(route.id) },
                            shape = LoponShapes.card,
                            colors = CardDefaults.cardColors(containerColor = LoponColors.surfaceCard)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(LoponColors.primaryYellow)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(LoponDimens.cardPadding),
                                    verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
                                ) {
                                    Text(
                                        text = route.name,
                                        style = LoponTypography.body,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LoponColors.onSurfacePrimary
                                    )
                                    Text(
                                        text = MetricsFormatter.formatDistanceAdaptive(route.distanceMeters),
                                        style = LoponTypography.caption,
                                        color = LoponColors.onSurfaceSecondary
                                    )
                                }
                                if (route.points.size >= 2) {
                                    RoutePreviewMiniMap(
                                        points = route.points,
                                        modifier = Modifier.padding(LoponDimens.spacerSmall)
                                    )
                                }
                            }
                        }
                    }
                }

                uiState.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LoponColors.error.copy(alpha = 0.08f)),
                        shape = LoponShapes.card
                    ) {
                        Column(
                            modifier = Modifier.padding(LoponDimens.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
                        ) {
                            Text(text = error, color = LoponColors.error, style = LoponTypography.body)
                            TextButton(onClick = viewModel::clearError) {
                                Text("Скрыть")
                            }
                        }
                    }
                }
            }
        }
    }

    if (routePendingDelete != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text("Удалить маршрут?", style = LoponTypography.sectionTitle) },
            text = { Text("Маршрут будет удалён из локального списка.", style = LoponTypography.body) },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDeleteRoute,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoponColors.error,
                        contentColor = LoponColors.white
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun RouteDetailsCard(
    route: Route,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onUseForStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LoponShapes.card,
        colors = CardDefaults.cardColors(containerColor = LoponColors.surfaceCard)
    ) {
        Column(
            modifier = Modifier.padding(LoponDimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
        ) {
            Text(
                text = "Детали маршрута",
                style = LoponTypography.sectionTitle,
                color = LoponColors.onSurfacePrimary
            )
            Text("Название: ${route.name}", style = LoponTypography.body, color = LoponColors.onSurfacePrimary)
            Text(
                "Дистанция: ${MetricsFormatter.formatDistanceAdaptive(route.distanceMeters)}",
                style = LoponTypography.body,
                color = LoponColors.onSurfacePrimary
            )
            Text("Точек: ${route.pointCount}", style = LoponTypography.caption, color = LoponColors.onSurfaceSecondary)
            Button(
                onClick = onUseForStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoponColors.primaryYellow,
                    contentColor = LoponColors.black
                ),
                shape = LoponShapes.button
            ) {
                Text("Использовать для старта", style = LoponTypography.button)
            }
            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoponColors.error,
                    contentColor = LoponColors.white
                ),
                shape = LoponShapes.button
            ) {
                Text("Удалить маршрут", style = LoponTypography.button)
            }
            TextButton(onClick = onBack) {
                Text("Назад к списку")
            }
        }
    }
}
