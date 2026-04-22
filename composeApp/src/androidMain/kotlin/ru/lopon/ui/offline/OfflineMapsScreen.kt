package ru.lopon.ui.offline

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ru.lopon.platform.OfflineRegionInfo
import ru.lopon.ui.components.ErrorCard
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography

@Composable
fun OfflineMapsScreen(
    viewModel: OfflineMapsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        OfflineMapsHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(LoponDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
        ) {
            SearchSection(
                query = uiState.searchQuery,
                isSearching = uiState.isSearching,
                isDownloading = uiState.isDownloading,
                onQueryChanged = { viewModel.updateSearchQuery(it) },
                onSearch = { viewModel.searchRegion() }
            )

            if (uiState.searchResults.isNotEmpty()) {
                Text(
                    text = "Результаты поиска",
                    style = LoponTypography.sectionTitle,
                    color = LoponColors.onSurfacePrimary
                )
                uiState.searchResults.forEach { result ->
                    SearchResultCard(
                        result = result,
                        isDownloading = uiState.isDownloading,
                        onDownload = { viewModel.downloadSearchResult(result) }
                    )
                }
            }

            if (uiState.isDownloading) {
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
                            text = "Загрузка...",
                            style = LoponTypography.body,
                            fontWeight = FontWeight.SemiBold,
                            color = LoponColors.onSurfacePrimary
                        )
                        val progress = uiState.downloadProgress?.percentage ?: 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = LoponColors.primaryYellow
                        )
                        val downloadedMb = (uiState.downloadProgress?.completedSize ?: 0L) / 1024.0 / 1024.0
                        Text(
                            text = "${(progress * 100).toInt()}% · ${"%.1f".format(downloadedMb)} МБ загружено",
                            style = LoponTypography.caption,
                            color = LoponColors.onSurfaceSecondary
                        )
                    }
                }
            }

            Text(
                text = "Загруженные регионы",
                style = LoponTypography.sectionTitle,
                color = LoponColors.onSurfacePrimary
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LoponColors.primaryYellow)
                }
            } else if (uiState.regions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = LoponShapes.card
                ) {
                    Text(
                        text = "Нет загруженных регионов",
                        modifier = Modifier.padding(LoponDimens.cardPadding),
                        style = LoponTypography.body,
                        color = LoponColors.onSurfaceSecondary
                    )
                }
            } else {
                uiState.regions.forEach { region ->
                    RegionCard(
                        region = region,
                        onDelete = { viewModel.deleteRegion(region.id) }
                    )
                }
            }

            uiState.errorMessage?.let { error ->
                ErrorCard(message = error, onDismiss = { viewModel.clearError() })
            }

            Spacer(modifier = Modifier.height(LoponDimens.spacerLarge))
        }
    }
}

@Composable
private fun SearchSection(
    query: String,
    isSearching: Boolean,
    isDownloading: Boolean,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = "Поиск региона",
                    style = LoponTypography.body,
                    fontWeight = FontWeight.SemiBold,
                    color = LoponColors.onSurfacePrimary
                )
                Text(
                    text = "Найдите город или регион России для оффлайн-загрузки",
                    style = LoponTypography.caption,
                    color = LoponColors.onSurfaceSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChanged,
                        placeholder = { Text("Москва, Сочи, Казань...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LoponColors.primaryYellow,
                            cursorColor = LoponColors.primaryYellow
                        )
                    )
                    Button(
                        onClick = onSearch,
                        enabled = query.isNotBlank() && !isSearching && !isDownloading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LoponColors.primaryYellow,
                            contentColor = LoponColors.black
                        ),
                        shape = LoponShapes.button
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .height(20.dp)
                                    .width(20.dp),
                                color = LoponColors.black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Search, contentDescription = "Поиск")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    result: SearchResult,
    isDownloading: Boolean,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LoponShapes.card,
        colors = CardDefaults.cardColors(containerColor = LoponColors.surfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LoponDimens.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier
                .weight(1f)
                .padding(end = LoponDimens.spacerSmall)) {
                Text(
                    text = result.name,
                    style = LoponTypography.body,
                    color = LoponColors.onSurfacePrimary,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (result.estimatedSizeBytes > 0) {
                    val sizeMb = result.estimatedSizeBytes / 1024.0 / 1024.0
                    Text(
                        text = "~${"%.0f".format(sizeMb)} МБ",
                        style = LoponTypography.caption,
                        color = LoponColors.onSurfaceSecondary
                    )
                }
            }
            Button(
                onClick = onDownload,
                enabled = !isDownloading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoponColors.primaryYellow,
                    contentColor = LoponColors.black
                ),
                shape = LoponShapes.button
            ) {
                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Скачать", style = LoponTypography.buttonSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RegionCard(
    region: OfflineRegionInfo,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LoponShapes.card,
        colors = CardDefaults.cardColors(containerColor = LoponColors.surfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LoponDimens.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = region.name,
                    style = LoponTypography.body,
                    fontWeight = FontWeight.SemiBold,
                    color = LoponColors.onSurfacePrimary
                )
                val sizeText = if (region.completedSize > 0) {
                    "${region.completedSize / 1024 / 1024} МБ"
                } else {
                    "Размер неизвестен"
                }
                Text(
                    text = sizeText,
                    style = LoponTypography.caption,
                    color = LoponColors.onSurfaceSecondary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Удалить",
                    tint = LoponColors.error
                )
            }
        }
    }
}

@Composable
private fun OfflineMapsHeader(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LoponColors.topBarBackground)
            .padding(
                horizontal = LoponDimens.spacerSmall,
                vertical = LoponDimens.spacerMedium
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = LoponColors.topBarContent
                )
            }
            Text(
                text = "Оффлайн-карты",
                style = LoponTypography.screenTitle,
                color = LoponColors.topBarContent,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(LoponColors.primaryYellow)
        )
    }
}
