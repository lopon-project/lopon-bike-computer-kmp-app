package ru.lopon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.lopon.domain.model.GeoCoordinate
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponShapes

@Composable
fun RoutePreviewMiniMap(
    points: List<GeoCoordinate>,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    if (points.size < 2) return

    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLon = points.minOf { it.longitude }
    val maxLon = points.maxOf { it.longitude }
    val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
    val lonRange = (maxLon - minLon).coerceAtLeast(0.0001)

    val colorPrimary = LoponColors.primaryYellow
    val colorWhite = LoponColors.white
    val colorBg = LoponColors.black

    Box(
        modifier = modifier
            .size(size)
            .clip(LoponShapes.small)
            .background(colorBg.copy(alpha = 0.9f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val pad = 8f
            val drawW = w - pad * 2
            val drawH = h - pad * 2

            val path = Path().apply {
                points.forEachIndexed { index, point ->
                    val x = pad + ((point.longitude - minLon) / lonRange * drawW).toFloat()
                    val y = pad + ((maxLat - point.latitude) / latRange * drawH).toFloat()
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = colorPrimary,
                style = Stroke(
                    width = 2.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            val startX = pad + ((points.first().longitude - minLon) / lonRange * drawW).toFloat()
            val startY = pad + ((maxLat - points.first().latitude) / latRange * drawH).toFloat()
            drawCircle(
                color = colorPrimary,
                radius = 4f,
                center = androidx.compose.ui.geometry.Offset(startX, startY)
            )

            val endX = pad + ((points.last().longitude - minLon) / lonRange * drawW).toFloat()
            val endY = pad + ((maxLat - points.last().latitude) / latRange * drawH).toFloat()
            drawCircle(
                color = colorWhite,
                radius = 4f,
                center = androidx.compose.ui.geometry.Offset(endX, endY)
            )
        }
    }
}
