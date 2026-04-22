package ru.lopon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography

/**
 * Start trip button.
 */
@Composable
fun StartTripButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(LoponDimens.buttonHeightLarge),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = LoponColors.primaryYellow,
            contentColor = LoponColors.black
        ),
        shape = LoponShapes.button
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
        Text(
            text = if (enabled) "Начать поездку" else "Подключите датчик",
            style = LoponTypography.buttonLarge
        )
    }
}

/**
 * Control buttons for recording state (Pause + Stop).
 */
@Composable
fun RecordingControls(
    onPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
    ) {
        OutlinedButton(
            onClick = onPauseClick,
            modifier = Modifier
                .weight(1f)
                .height(LoponDimens.buttonHeightLarge),
            shape = LoponShapes.button
        ) {
            Icon(Icons.Filled.Pause, contentDescription = null)
            Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
            Text(
                text = "Пауза",
                style = LoponTypography.button
            )
        }
        Button(
            onClick = onStopClick,
            modifier = Modifier
                .weight(1f)
                .height(LoponDimens.buttonHeightLarge),
            colors = ButtonDefaults.buttonColors(
                containerColor = LoponColors.error,
                contentColor = LoponColors.white
            ),
            shape = LoponShapes.button
        ) {
            Icon(Icons.Filled.Stop, contentDescription = null)
            Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
            Text(
                text = "Завершить",
                style = LoponTypography.button
            )
        }
    }
}

/**
 * Control buttons for paused state (Resume + Stop).
 */
@Composable
fun PausedControls(
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
    ) {
        Button(
            onClick = onResumeClick,
            modifier = Modifier
                .weight(1f)
                .height(LoponDimens.buttonHeightLarge),
            colors = ButtonDefaults.buttonColors(
                containerColor = LoponColors.primaryYellow,
                contentColor = LoponColors.black
            ),
            shape = LoponShapes.button
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
            Text(
                text = "Продолжить",
                style = LoponTypography.button
            )
        }
        Button(
            onClick = onStopClick,
            modifier = Modifier
                .weight(1f)
                .height(LoponDimens.buttonHeightLarge),
            colors = ButtonDefaults.buttonColors(
                containerColor = LoponColors.error,
                contentColor = LoponColors.white
            ),
            shape = LoponShapes.button
        ) {
            Icon(Icons.Filled.Stop, contentDescription = null)
            Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
            Text(
                text = "Завершить",
                style = LoponTypography.button
            )
        }
    }
}
