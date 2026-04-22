package ru.lopon.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.lopon.domain.model.Trip
import ru.lopon.domain.model.displayName
import ru.lopon.ui.components.ScreenHeader
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTrip = uiState.trips.firstOrNull { it.id == uiState.selectedTripId }
    val tripPendingDelete = uiState.trips.firstOrNull { it.id == uiState.pendingDeleteTripId }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = "История поездок")

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
                if (selectedTrip != null) {
                    TripDetailsCard(
                        trip = selectedTrip,
                        onBack = viewModel::closeTripDetails,
                        onDelete = { viewModel.requestDeleteTrip(selectedTrip.id) }
                    )
                } else if (uiState.trips.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = LoponShapes.card
                    ) {
                        Column(
                            modifier = Modifier.padding(LoponDimens.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
                        ) {
                            Text(
                                text = "Поездок пока нет",
                                style = LoponTypography.body,
                                fontWeight = FontWeight.SemiBold,
                                color = LoponColors.onSurfacePrimary
                            )
                            Text(
                                text = "Завершите первую поездку на вкладке Главная, и она появится здесь.",
                                style = LoponTypography.caption,
                                color = LoponColors.onSurfaceSecondary
                            )
                        }
                    }
                } else {
                    uiState.trips.forEach { trip ->
                        val distanceKm = trip.distanceMeters / 1000.0
                        TripCard(
                            title = "${trip.mode.displayName()} • %.2f км".format(distanceKm),
                            subtitle = "Старт: ${trip.startTimeUtc}${if (trip.endTimeUtc != null) " • Завершена" else " • Активна"}",
                            onClick = { viewModel.openTripDetails(trip.id) }
                        )
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

    if (tripPendingDelete != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text("Удалить поездку?", style = LoponTypography.sectionTitle) },
            text = { Text("Действие необратимо. Поездка будет удалена из истории.", style = LoponTypography.body) },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDeleteTrip,
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
private fun TripCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                modifier = Modifier.padding(LoponDimens.cardPadding),
                verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
            ) {
                Text(
                    text = title,
                    style = LoponTypography.body,
                    fontWeight = FontWeight.SemiBold,
                    color = LoponColors.onSurfacePrimary
                )
                Text(
                    text = subtitle,
                    style = LoponTypography.caption,
                    color = LoponColors.onSurfaceSecondary
                )
            }
        }
    }
}

@Composable
private fun TripDetailsCard(
    trip: Trip,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val distanceKm = trip.distanceMeters / 1000.0
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
                text = "Детали поездки",
                style = LoponTypography.sectionTitle,
                color = LoponColors.onSurfacePrimary
            )
            Text(
                "Режим: ${trip.mode.displayName()}",
                style = LoponTypography.body,
                color = LoponColors.onSurfacePrimary
            )
            Text(
                "Дистанция: %.2f км".format(distanceKm),
                style = LoponTypography.body,
                color = LoponColors.onSurfacePrimary
            )
            Text("Старт: ${trip.startTimeUtc}", style = LoponTypography.body, color = LoponColors.onSurfacePrimary)
            Text("Финиш: ${trip.endTimeUtc ?: "-"}", style = LoponTypography.body, color = LoponColors.onSurfacePrimary)
            Text(
                "Маршрут: ${trip.routeId ?: "не задан"}",
                style = LoponTypography.body,
                color = LoponColors.onSurfacePrimary
            )
            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoponColors.error,
                    contentColor = LoponColors.white
                ),
                shape = LoponShapes.button
            ) {
                Text("Удалить поездку", style = LoponTypography.button)
            }
            TextButton(onClick = onBack) {
                Text("Назад к списку")
            }
        }
    }
}
