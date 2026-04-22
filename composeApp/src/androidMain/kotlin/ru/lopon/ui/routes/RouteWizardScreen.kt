package ru.lopon.ui.routes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.lopon.data.geocoding.PhotonSearchResult
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography
import ru.lopon.ui.util.rememberIsLandscape

@Composable
fun RouteWizardScreen(
    viewModel: RouteWizardViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isLandscape = rememberIsLandscape()

    LaunchedEffect(Unit) {
        if (!uiState.locationPermissionGranted) {
            viewModel.requestLocationPermission()
        }
    }

    val gpxFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val content = inputStream?.bufferedReader()?.readText() ?: ""
                inputStream?.close()
                if (content.isNotEmpty()) {
                    viewModel.importGpx(content)
                }
            } catch (_: Exception) {
            }
        }
    }

    if (uiState.savedSuccessfully) {
        onBack()
        return
    }

    val onImportGpxClick: () -> Unit = {
        gpxFilePicker.launch(
            arrayOf("application/gpx+xml", "application/xml", "text/xml", "*/*")
        )
    }

    if (isLandscape) {
        Row(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
            ) {
                MapPaneContent(
                    viewModel = viewModel,
                    uiState = uiState,
                    onBack = onBack,
                    onImportGpx = onImportGpxClick,
                    fabAlignment = Alignment.TopEnd,
                    fabTopPadding = 120.dp
                )
            }
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .widthIn(min = LoponDimens.sidePanelMinWidthLandscape)
                    .fillMaxHeight()
                    .background(LoponColors.black.copy(alpha = 0.96f))
                    .verticalScroll(rememberScrollState())
                    .padding(LoponDimens.spacerMedium),
                verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
            ) {
                WizardFormBody(
                    uiState = uiState,
                    onUpdateName = { viewModel.updateRouteName(it) },
                    onCalculate = { viewModel.calculateRoute() },
                    onSave = { viewModel.saveRoute() }
                )
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            MapPaneContent(
                viewModel = viewModel,
                uiState = uiState,
                onBack = onBack,
                onImportGpx = onImportGpxClick,
                fabAlignment = Alignment.CenterEnd,
                fabTopPadding = 0.dp
            )

            WizardBottomPanel(
                uiState = uiState,
                onUpdateName = { viewModel.updateRouteName(it) },
                onCalculate = { viewModel.calculateRoute() },
                onSave = { viewModel.saveRoute() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun BoxScope.MapPaneContent(
    viewModel: RouteWizardViewModel,
    uiState: RouteWizardUiState,
    onBack: () -> Unit,
    onImportGpx: () -> Unit,
    fabAlignment: Alignment,
    fabTopPadding: Dp
) {
    RouteWizardMapView(
        waypoints = uiState.waypoints,
        calculatedRoute = uiState.calculatedRoute,
        currentPosition = uiState.currentLocation,
        selectedWaypointIndex = uiState.selectedWaypointIndex,
        onMapTap = { coord -> viewModel.addWaypoint(coord) },
        onInsertWaypoint = { index, coord -> viewModel.insertWaypoint(index, coord) },
        modifier = Modifier.fillMaxSize()
    )

    WizardTopBar(
        waypointCount = uiState.waypoints.size,
        searchQuery = uiState.searchQuery,
        onSearchQueryChanged = { viewModel.updateSearchQuery(it) },
        onBack = onBack,
        onImportGpx = onImportGpx,
        onDismissSearch = { viewModel.dismissSearch() }
    )

    AnimatedVisibility(
        visible = uiState.showSearchResults && uiState.searchResults.isNotEmpty(),
        enter = fadeIn() + slideInVertically { -it / 4 },
        exit = fadeOut() + slideOutVertically { -it / 4 },
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 110.dp)
            .padding(horizontal = LoponDimens.spacerSmall)
    ) {
        SearchResultsList(
            results = uiState.searchResults,
            isSearching = uiState.isSearching,
            onResultSelected = { viewModel.selectSearchResult(it) }
        )
    }

    if (uiState.selectedWaypointIndex != null) {
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 120.dp)
                .padding(horizontal = 48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = LoponColors.primaryYellow)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Нажмите новую позицию для точки",
                    style = LoponTypography.caption,
                    color = LoponColors.black
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.cancelWaypointEdit() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Отмена", modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .align(fabAlignment)
            .padding(
                top = fabTopPadding,
                end = LoponDimens.spacerSmall
            ),
        verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
    ) {
        SmallFloatingActionButton(
            onClick = {
                if (!uiState.locationPermissionGranted) {
                    viewModel.requestLocationPermission()
                }
                uiState.currentLocation?.let { viewModel.addWaypoint(it) }
            },
            containerColor = LoponColors.white,
            contentColor = LoponColors.black,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            Icon(
                Icons.Filled.MyLocation,
                contentDescription = "Моя позиция",
                modifier = Modifier.size(20.dp),
                tint = if (uiState.currentLocation != null) LoponColors.black
                else LoponColors.onSurfaceSecondary
            )
        }

        if (uiState.waypoints.isNotEmpty()) {
            SmallFloatingActionButton(
                onClick = { viewModel.removeLastWaypoint() },
                containerColor = LoponColors.white,
                contentColor = LoponColors.black,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Отменить точку",
                    modifier = Modifier.size(20.dp)
                )
            }
            SmallFloatingActionButton(
                onClick = { viewModel.clearAll() },
                containerColor = LoponColors.error,
                contentColor = LoponColors.white,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Очистить всё",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    uiState.errorMessage?.let { error ->
        ErrorOverlay(
            message = error,
            onDismiss = { viewModel.clearError() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 120.dp)
        )
    }

    if (uiState.isCalculating) {
        LoadingOverlay(
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun WizardTopBar(
    waypointCount: Int,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onBack: () -> Unit,
    onImportGpx: () -> Unit,
    onDismissSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LoponColors.black.copy(alpha = 0.88f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LoponDimens.spacerSmall, vertical = LoponDimens.spacerSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = LoponColors.white
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Создание маршрута",
                    style = LoponTypography.body,
                    fontWeight = FontWeight.SemiBold,
                    color = LoponColors.white
                )
                Text(
                    text = if (waypointCount == 0) "Нажмите на карту или найдите место"
                    else "Точек: $waypointCount",
                    style = LoponTypography.caption,
                    color = LoponColors.primaryYellow
                )
            }

            IconButton(
                onClick = onImportGpx,
                colors = IconButtonDefaults.iconButtonColors(contentColor = LoponColors.white)
            ) {
                Icon(Icons.Filled.FileOpen, contentDescription = "Импорт GPX")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = {
                Text(
                    "Поиск места, адреса, POI...",
                    style = LoponTypography.caption,
                    color = LoponColors.onSurfaceSecondary
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = LoponColors.onSurfaceSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = onDismissSearch) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Очистить",
                            tint = LoponColors.onSurfaceSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LoponDimens.spacerSmall)
                .padding(bottom = LoponDimens.spacerSmall),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LoponColors.primaryYellow,
                unfocusedBorderColor = LoponColors.onSurfaceSecondary.copy(alpha = 0.4f),
                cursorColor = LoponColors.primaryYellow,
                focusedTextColor = LoponColors.white,
                unfocusedTextColor = LoponColors.white
            ),
            textStyle = LoponTypography.body,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun SearchResultsList(
    results: List<PhotonSearchResult>,
    isSearching: Boolean,
    onResultSelected: (PhotonSearchResult) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LoponColors.black.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            items(results) { result ->
                SearchResultItem(
                    result = result,
                    onClick = { onResultSelected(result) }
                )
            }
            if (isSearching) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(LoponDimens.spacerMedium),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = LoponColors.primaryYellow,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Поиск...", style = LoponTypography.caption, color = LoponColors.onSurfaceSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    result: PhotonSearchResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = LoponDimens.cardPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(LoponColors.primaryYellow)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.name,
                style = LoponTypography.body,
                fontWeight = FontWeight.Medium,
                color = LoponColors.white,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = result.displayName,
                style = LoponTypography.caption,
                color = LoponColors.onSurfaceSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = localizePoiType(result.osmKey, result.osmValue),
            style = LoponTypography.caption,
            color = LoponColors.primaryYellow.copy(alpha = 0.7f),
            maxLines = 1
        )
    }
}

private fun localizePoiType(osmKey: String, osmValue: String): String {
    return when (osmKey) {
        "place" -> when (osmValue) {
            "city" -> "Город"
            "town" -> "Город"
            "village" -> "Деревня"
            "hamlet" -> "Посёлок"
            "suburb" -> "Район"
            else -> "Место"
        }

        "highway" -> "Дорога"
        "railway" -> "Станция"
        "amenity" -> when (osmValue) {
            "cafe" -> "Кафе"
            "restaurant" -> "Ресторан"
            "fuel" -> "АЗС"
            "bicycle_repair_station" -> "Веломастерская"
            "bicycle_rental" -> "Прокат вело"
            "parking" -> "Парковка"
            "shelter" -> "Укрытие"
            else -> "Объект"
        }

        "tourism" -> when (osmValue) {
            "viewpoint" -> "Обзорная"
            "camp_site" -> "Кемпинг"
            "hotel" -> "Отель"
            "hostel" -> "Хостел"
            "information" -> "Инфо"
            "attraction" -> "Достопр."
            else -> "Туризм"
        }

        "shop" -> "Магазин"
        "natural" -> "Природа"
        "leisure" -> "Отдых"
        "building" -> "Здание"
        else -> osmValue.take(10)
    }
}

@Composable
private fun WizardBottomPanel(
    uiState: RouteWizardUiState,
    onUpdateName: (String) -> Unit,
    onCalculate: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasRoute = uiState.calculatedRoute != null

    AnimatedVisibility(
        visible = uiState.waypoints.isNotEmpty() || hasRoute,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LoponDimens.spacerSmall),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LoponColors.black.copy(alpha = 0.92f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(LoponDimens.cardPadding),
                verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
            ) {
                WizardFormBody(
                    uiState = uiState,
                    onUpdateName = onUpdateName,
                    onCalculate = onCalculate,
                    onSave = onSave
                )
            }
        }
    }
}

@Composable
private fun WizardFormBody(
    uiState: RouteWizardUiState,
    onUpdateName: (String) -> Unit,
    onCalculate: () -> Unit,
    onSave: () -> Unit
) {
    val hasRoute = uiState.calculatedRoute != null
    val canCalculate = uiState.waypoints.size >= 2 && !uiState.isCalculating

    OutlinedTextField(
        value = uiState.routeName,
        onValueChange = onUpdateName,
        label = { Text("Название маршрута", color = LoponColors.onSurfaceSecondary) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LoponColors.primaryYellow,
            unfocusedBorderColor = LoponColors.onSurfaceSecondary.copy(alpha = 0.5f),
            cursorColor = LoponColors.primaryYellow,
            focusedTextColor = LoponColors.white,
            unfocusedTextColor = LoponColors.white
        ),
        textStyle = LoponTypography.body
    )

    if (hasRoute) {
        val details = uiState.routeDetails

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RouteStatChip(
                label = "Дистанция",
                value = details?.let { formatDistance(it.distanceMeters) }
                    ?: "${uiState.calculatedRoute?.pointCount ?: 0} тч."
            )
            if (details != null) {
                RouteStatChip(
                    label = "Время",
                    value = formatDuration(details.durationSeconds)
                )
                details.elevationGainMeters?.let { gain ->
                    RouteStatChip(
                        label = "Набор",
                        value = "${gain.toInt()} м"
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
    ) {
        if (!hasRoute) {
            Button(
                onClick = onCalculate,
                enabled = canCalculate,
                modifier = Modifier
                    .weight(1f)
                    .height(LoponDimens.buttonHeightMedium),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoponColors.primaryYellow,
                    contentColor = LoponColors.black,
                    disabledContainerColor = LoponColors.primaryYellow.copy(alpha = 0.3f),
                    disabledContentColor = LoponColors.black.copy(alpha = 0.5f)
                ),
                shape = LoponShapes.button
            ) {
                Text(
                    text = if (uiState.waypoints.size < 2) "Нужно ≥ 2 точки"
                    else "Построить маршрут",
                    style = LoponTypography.button
                )
            }
        } else {
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .weight(1f)
                    .height(LoponDimens.buttonHeightMedium),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoponColors.success,
                    contentColor = LoponColors.white
                ),
                shape = LoponShapes.button
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (uiState.isSaving) "Сохранение..." else "Сохранить",
                    style = LoponTypography.button
                )
            }
        }
    }
}

@Composable
private fun RouteStatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = LoponTypography.body,
            fontWeight = FontWeight.Bold,
            color = LoponColors.primaryYellow
        )
        Text(
            text = label,
            style = LoponTypography.caption,
            color = LoponColors.onSurfaceSecondary
        )
    }
}

@Composable
private fun ErrorOverlay(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LoponDimens.screenPadding),
        shape = LoponShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = LoponColors.error.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(LoponDimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = LoponTypography.caption,
                color = LoponColors.white,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Закрыть",
                    tint = LoponColors.white
                )
            }
        }
    }
}

@Composable
private fun LoadingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(LoponColors.black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = LoponColors.primaryYellow,
            modifier = Modifier.size(36.dp)
        )
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        "%.1f км".format(meters / 1000)
    } else {
        "${meters.toInt()} м"
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}ч ${minutes}мин" else "${minutes} мин"
}
