package ru.lopon.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography

@Composable
internal fun MapOverlayControls(
    cameraMode: TripMapCameraMode,
    autoCenterEnabled: Boolean,
    centerButtonEnabled: Boolean,
    hasRoute: Boolean,
    hasPosition: Boolean,
    onToggleMode: () -> Unit,
    onCenterNow: () -> Unit,
    onToggleAutoCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(LoponDimens.spacerSmall),
        verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall),
        horizontalAlignment = Alignment.End
    ) {
        FilledIconButton(
            onClick = onToggleMode,
            enabled = hasRoute,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = LoponColors.white.copy(alpha = 0.9f),
                contentColor = LoponColors.black
            )
        ) {
            Icon(
                Icons.Filled.Layers,
                contentDescription = if (cameraMode == TripMapCameraMode.FOLLOW_POSITION)
                    "Обзор маршрута" else "Следовать за позицией",
                modifier = Modifier.size(22.dp)
            )
        }

        FilledIconButton(
            onClick = onCenterNow,
            enabled = centerButtonEnabled && (hasRoute || hasPosition),
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = LoponColors.white.copy(alpha = 0.9f),
                contentColor = LoponColors.black
            )
        ) {
            Icon(
                Icons.Filled.CenterFocusStrong,
                contentDescription = "Центрировать",
                modifier = Modifier.size(22.dp)
            )
        }

        FilledIconButton(
            onClick = onToggleAutoCenter,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (autoCenterEnabled) LoponColors.primaryYellow
                else LoponColors.white.copy(alpha = 0.9f),
                contentColor = LoponColors.black
            )
        ) {
            Icon(
                Icons.Filled.MyLocation,
                contentDescription = if (autoCenterEnabled) "Автоцентр вкл" else "Автоцентр выкл",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
internal fun CameraModeChip(
    cameraMode: TripMapCameraMode,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = LoponColors.black.copy(alpha = 0.7f),
                shape = LoponShapes.small
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            if (cameraMode == TripMapCameraMode.FOLLOW_POSITION)
                Icons.Filled.MyLocation else Icons.Filled.Layers,
            contentDescription = null,
            tint = LoponColors.white,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = if (cameraMode == TripMapCameraMode.FOLLOW_POSITION)
                "Следовать" else "Обзор",
            style = LoponTypography.caption,
            color = LoponColors.white
        )
    }
}

@Composable
internal fun DisplayModeToggle(
    displayMode: TripDisplayMode,
    onModeChanged: (TripDisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerSmall)
    ) {
        FilterChip(
            selected = displayMode == TripDisplayMode.MAP,
            onClick = { onModeChanged(TripDisplayMode.MAP) },
            label = { Text("Карта", style = LoponTypography.buttonSmall, maxLines = 1) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LoponColors.primaryYellow,
                selectedLabelColor = LoponColors.black
            )
        )
        FilterChip(
            selected = displayMode == TripDisplayMode.METRICS,
            onClick = { onModeChanged(TripDisplayMode.METRICS) },
            label = { Text("Метрики", style = LoponTypography.buttonSmall, maxLines = 1) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LoponColors.primaryYellow,
                selectedLabelColor = LoponColors.black
            )
        )
    }
}
