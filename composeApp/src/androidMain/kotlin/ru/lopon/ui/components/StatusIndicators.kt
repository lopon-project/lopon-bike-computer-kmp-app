package ru.lopon.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponTypography

/**
 * A small connection status indicator with dot and label.
 */
@Composable
fun ConnectionIndicator(
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    connectedLabel: String = "Датчик",
    disconnectedLabel: String = "Нет связи"
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(LoponDimens.statusDotSize)
                .clip(CircleShape)
                .background(if (isConnected) LoponColors.success else LoponColors.divider)
        )
        Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
        Text(
            text = if (isConnected) connectedLabel else disconnectedLabel,
            style = LoponTypography.caption,
            color = LoponColors.onSurfaceSecondary
        )
    }
}

/**
 * Recording status indicator with pulsating REC dot animation.
 */
@Composable
fun RecordingIndicator(
    isRecording: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isRecording && !isPaused) return

    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")

    // Pulsating scale for REC dot
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording && !isPaused) 1.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_scale"
    )

    // Pulsating alpha for REC dot
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording && !isPaused) 0.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_alpha"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(LoponDimens.statusDotSize)
                .scale(if (!isPaused) pulseScale else 1f)
                .alpha(if (!isPaused) pulseAlpha else 1f)
                .clip(CircleShape)
                .background(if (isPaused) LoponColors.warning else LoponColors.error)
        )
        Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
        Text(
            text = if (isPaused) "Пауза" else "REC",
            style = LoponTypography.caption,
            color = if (isPaused) LoponColors.warning else LoponColors.error,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Combined status row with connection and recording indicators.
 */
@Composable
fun StatusIndicatorRow(
    isConnected: Boolean,
    isRecording: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ConnectionIndicator(isConnected = isConnected)
        RecordingIndicator(isRecording = isRecording, isPaused = isPaused)
    }
}
