package ru.lopon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography

@Composable
fun ElevationProfileChart(
    elevations: List<Double>,
    modifier: Modifier = Modifier,
    chartHeight: Int = 100
) {
    if (elevations.size < 2) return

    val minElevation = elevations.min()
    val maxElevation = elevations.max()
    val range = (maxElevation - minElevation).coerceAtLeast(1.0)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = LoponShapes.card,
        colors = CardDefaults.cardColors(containerColor = LoponColors.surfaceCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Профиль высот",
                style = LoponTypography.metricLabel,
                color = LoponColors.onSurfaceSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            val colorPrimary = LoponColors.primaryYellow
            val colorSecondary = LoponColors.onSurfaceSecondary

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight.dp)
            ) {
                val w = size.width
                val h = size.height
                val padding = 4f
                val drawW = w - padding * 2
                val drawH = h - padding * 2

                val step = drawW / (elevations.size - 1).coerceAtLeast(1)

                val linePath = Path().apply {
                    elevations.forEachIndexed { index, elevation ->
                        val x = padding + index * step
                        val normalizedY = ((elevation - minElevation) / range).toFloat()
                        val y = padding + drawH * (1f - normalizedY)
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(padding + (elevations.size - 1) * step, padding + drawH)
                    lineTo(padding, padding + drawH)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colorPrimary.copy(alpha = 0.3f),
                            colorPrimary.copy(alpha = 0.05f)
                        )
                    )
                )

                drawPath(
                    path = linePath,
                    color = colorPrimary,
                    style = Stroke(
                        width = 2.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                val minIndex = elevations.indexOf(elevations.min())
                val maxIndex = elevations.indexOf(elevations.max())

                drawCircle(
                    color = colorPrimary,
                    radius = 4f,
                    center = Offset(
                        padding + maxIndex * step,
                        padding + drawH * (1f - ((elevations[maxIndex] - minElevation) / range).toFloat())
                    )
                )

                drawCircle(
                    color = colorSecondary,
                    radius = 4f,
                    center = Offset(
                        padding + minIndex * step,
                        padding + drawH * (1f - ((elevations[minIndex] - minElevation) / range).toFloat())
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "↑ ${maxElevation.toInt()} м  ↓ ${minElevation.toInt()} м  Δ ${(maxElevation - minElevation).toInt()} м",
                style = LoponTypography.caption,
                color = LoponColors.onSurfaceSecondary
            )
        }
    }
}
