package ru.lopon.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography

/**
 * Animated speed display with smooth transitions like a real speedometer.
 */
@Composable
fun SpeedDisplay(
    speedKmh: Double,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    label: String = "Скорость",
    speedFontSize: TextUnit = 72.sp,
    animationDurationMs: Int = 300
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speedKmh.toFloat(),
        animationSpec = tween(
            durationMillis = animationDurationMs,
            easing = FastOutSlowInEasing
        ),
        label = "speed"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = LoponTypography.metricLabel,
            color = if (isActive) LoponColors.black.copy(alpha = 0.7f)
            else LoponColors.onSurfaceSecondary
        )

        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = formatOneDecimal(animatedSpeed.toDouble()),
                style = LoponTypography.heroSpeed.copy(fontSize = speedFontSize),
                color = if (isActive) LoponColors.black else LoponColors.onSurfaceSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "км/ч",
                style = LoponTypography.metricUnit.copy(fontSize = 18.sp),
                color = if (isActive) LoponColors.black.copy(alpha = 0.7f)
                else LoponColors.onSurfaceSecondary
            )
        }
    }
}

/**
 * Speed display wrapped in a branded Card.
 * Yellow when recording, light gray when idle.
 */
@Composable
fun SpeedCard(
    speedKmh: Double,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) LoponColors.primaryYellow
            else LoponColors.backgroundLight
        ),
        shape = LoponShapes.card
    ) {
        SpeedDisplay(
            speedKmh = speedKmh,
            isActive = isRecording,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        )
    }
}

/**
 * Compact speed pill for map overlay — small semi-transparent chip.
 */
@Composable
fun SpeedPill(
    speedKmh: Double,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speedKmh.toFloat(),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "speedPill"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (isRecording) LoponColors.primaryYellow.copy(alpha = 0.92f)
        else LoponColors.black.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatOneDecimal(animatedSpeed.toDouble()),
                style = LoponTypography.metricValue.copy(fontSize = 22.sp),
                color = if (isRecording) LoponColors.black else LoponColors.white
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "км/ч",
                style = LoponTypography.metricUnit.copy(fontSize = 11.sp),
                color = if (isRecording) LoponColors.black.copy(alpha = 0.7f)
                else LoponColors.white.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatOneDecimal(value: Double): String {
    val rounded = (value * 10).roundToInt()
    return "${rounded / 10}.${rounded % 10}"
}
