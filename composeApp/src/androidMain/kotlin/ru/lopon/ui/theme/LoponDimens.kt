package ru.lopon.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object LoponDimens {

    val buttonHeightLarge = 56.dp
    val buttonHeightMedium = 48.dp
    val minTapTarget = 52.dp

    val cornerRadiusCard = 16.dp
    val cornerRadiusButton = 12.dp
    val cornerRadiusSmall = 8.dp

    val screenPadding = 16.dp
    val cardPadding = 16.dp
    val spacerSmall = 8.dp
    val spacerMedium = 12.dp
    val spacerLarge = 16.dp
    val spacerXLarge = 24.dp
    val spacerXXLarge = 32.dp

    val iconSizeNav = 26.dp
    val iconSizeAction = 24.dp
    val iconSizeLarge = 32.dp

    val statusDotSize = 12.dp
    val statusDotSizeLarge = 16.dp

    val mapControlSize = 48.dp
    val mapControlSpacing = 12.dp


    val speedPillMinWidth = 80.dp


    val accentBarWidth = 4.dp
    val accentLineHeight = 2.dp


    val iconSizeInline = 18.dp


    val navRailWidth = 72.dp
    val sidePanelMinWidthLandscape = 280.dp
    val landscapeMapWeight = 0.62f
    val contentMaxWidthCapped = 600.dp
}

object LoponShapes {
    val card = RoundedCornerShape(LoponDimens.cornerRadiusCard)
    val cardSmall = RoundedCornerShape(LoponDimens.cornerRadiusSmall)
    val button = RoundedCornerShape(LoponDimens.cornerRadiusButton)
    val small = RoundedCornerShape(LoponDimens.cornerRadiusSmall)
    val pill = RoundedCornerShape(50)
}
