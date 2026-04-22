package ru.lopon.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.lopon.R
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponTypography

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val wolfScale = remember { Animatable(0.3f) }
    val wolfAlpha = remember { Animatable(0f) }
    val roadProgress = remember { Animatable(0f) }
    var showTitle by remember { mutableStateOf(false) }
    var showTagline by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        wolfAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        wolfScale.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        roadProgress.animateTo(1f, tween(800, easing = LinearEasing))
        showTitle = true
        delay(300)

        showTagline = true
        delay(800)

        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoponColors.black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_lopon_wolf),
                contentDescription = "LOPON",
                modifier = Modifier
                    .size(120.dp)
                    .scale(wolfScale.value)
                    .alpha(wolfAlpha.value)
            )

            val roadColor = LoponColors.primaryYellow
            Canvas(
                modifier = Modifier
                    .size(width = 160.dp, height = 60.dp)
                    .alpha(wolfAlpha.value)
            ) {
                val w = size.width
                val h = size.height
                val progress = roadProgress.value

                val fullPath = Path().apply {
                    moveTo(w * 0.1f, h * 0.9f)
                    lineTo(w * 0.3f, h * 0.1f)
                    lineTo(w * 0.5f, h * 0.7f)
                    lineTo(w * 0.7f, h * 0.2f)
                    lineTo(w * 0.9f, h * 0.1f)
                }

                val pathMeasure = android.graphics.PathMeasure(
                    android.graphics.Path().apply {
                        moveTo(w * 0.1f, h * 0.9f)
                        lineTo(w * 0.3f, h * 0.1f)
                        lineTo(w * 0.5f, h * 0.7f)
                        lineTo(w * 0.7f, h * 0.2f)
                        lineTo(w * 0.9f, h * 0.1f)
                    },
                    false
                )
                val totalLength = pathMeasure.length
                val drawLength = totalLength * progress

                val partialPath = android.graphics.Path()
                pathMeasure.getSegment(0f, drawLength, partialPath, true)

                drawPath(
                    path = Path().apply {
                        val pts = FloatArray(2)
                        val step = drawLength / 100f
                        var dist = 0f
                        var first = true
                        while (dist <= drawLength) {
                            pathMeasure.getPosTan(dist, pts, null)
                            if (first) {
                                moveTo(pts[0], pts[1])
                                first = false
                            } else {
                                lineTo(pts[0], pts[1])
                            }
                            dist += step.coerceAtLeast(1f)
                        }
                        pathMeasure.getPosTan(drawLength, pts, null)
                        lineTo(pts[0], pts[1])
                    },
                    color = roadColor,
                    style = Stroke(
                        width = 8f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn(tween(400)) + slideInVertically(
                    tween(400, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 2 }
                )
            ) {
                Text(
                    text = "LOPON",
                    style = LoponTypography.brandTitle.copy(
                        fontSize = 36.sp
                    ),
                    color = LoponColors.primaryYellow
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = showTagline,
                enter = fadeIn(tween(500))
            ) {
                Text(
                    text = "ваш проводник к цели",
                    style = LoponTypography.brandTagline,
                    color = LoponColors.white.copy(alpha = 0.7f)
                )
            }
        }
    }
}
