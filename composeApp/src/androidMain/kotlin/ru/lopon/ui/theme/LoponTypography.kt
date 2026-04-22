package ru.lopon.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object LoponTypography {

    val heroSpeed = TextStyle(
        fontSize = 80.sp, fontWeight = FontWeight.Black, lineHeight = 84.sp,
        letterSpacing = (-1).sp
    )

    val screenTitle = TextStyle(
        fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp
    )
    val sectionTitle = TextStyle(
        fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp
    )

    val metricValue = TextStyle(
        fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp
    )
    val metricValueLarge = TextStyle(
        fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 48.sp
    )
    val metricLabel = TextStyle(
        fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp
    )
    val metricUnit = TextStyle(
        fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp
    )

    val body = TextStyle(
        fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp
    )
    val caption = TextStyle(
        fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp
    )

    val button = TextStyle(
        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp
    )
    val buttonLarge = TextStyle(
        fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp
    )
    val buttonSmall = TextStyle(
        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp
    )

    val navLabel = TextStyle(
        fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp
    )

    val brandTitle = TextStyle(
        fontSize = 20.sp, fontWeight = FontWeight.Black,
        letterSpacing = 3.sp, lineHeight = 24.sp
    )
    val brandTagline = TextStyle(
        fontSize = 11.sp, fontWeight = FontWeight.Normal,
        letterSpacing = 1.sp, lineHeight = 14.sp
    )

    val mapOverlay = TextStyle(
        fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp
    )
    val mapOverlaySmall = TextStyle(
        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp
    )
}
