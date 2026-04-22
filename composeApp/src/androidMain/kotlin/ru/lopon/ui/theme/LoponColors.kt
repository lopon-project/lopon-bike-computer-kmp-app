package ru.lopon.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class LoponColorPalette(
    val primaryYellow: Color,
    val primaryYellowDark: Color,
    val black: Color,
    val white: Color,

    val backgroundLight: Color,
    val backgroundDark: Color,
    val surfaceCard: Color,
    val surfaceCardElevated: Color,

    val divider: Color,
    val dividerStrong: Color,

    val success: Color,
    val successLight: Color,
    val error: Color,
    val errorLight: Color,
    val warning: Color,
    val warningLight: Color,
    val info: Color,

    val onSurfacePrimary: Color,
    val onSurfaceSecondary: Color,
    val onSurfaceDisabled: Color,
    val onDark: Color,
    val onDarkSecondary: Color,
    val onYellow: Color,

    val buttonPrimary: Color,
    val buttonPrimaryPressed: Color,
    val buttonSecondary: Color,
    val buttonDisabled: Color,
    val ripple: Color,

    val statusActive: Color,
    val statusSearching: Color,
    val statusInactive: Color,
    val statusError: Color,

    val mapRoute: Color,
    val mapTrack: Color,
    val mapPosition: Color,
    val mapPositionHalo: Color,
    val mapPositionStroke: Color,

    val navBarBackground: Color,
    val navBarSelected: Color,
    val navBarUnselected: Color,
    val navBarIndicator: Color,

    val topBarBackground: Color,
    val topBarContent: Color
)

fun lightLoponColors(): LoponColorPalette = LoponColorPalette(
    primaryYellow = Color(0xFFFFD400),
    primaryYellowDark = Color(0xFFE5BE00),
    black = Color(0xFF111111),
    white = Color(0xFFFFFFFF),

    backgroundLight = Color(0xFFF5F5F0),
    backgroundDark = Color(0xFF1A1A1A),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceCardElevated = Color(0xFFFAFAFA),

    divider = Color(0xFFD9D9D9),
    dividerStrong = Color(0xFFB0B0B0),

    success = Color(0xFF1B8A2A),
    successLight = Color(0xFFD4EDDA),
    error = Color(0xFFC62828),
    errorLight = Color(0xFFFDECEC),
    warning = Color(0xFFF57F17),
    warningLight = Color(0xFFFFF3E0),
    info = Color(0xFF1565C0),

    onSurfacePrimary = Color(0xFF111111),
    onSurfaceSecondary = Color(0xFF555555),
    onSurfaceDisabled = Color(0xFF9E9E9E),
    onDark = Color(0xFFFFFFFF),
    onDarkSecondary = Color(0xFFCCCCCC),
    onYellow = Color(0xFF111111),

    buttonPrimary = Color(0xFFFFD400),
    buttonPrimaryPressed = Color(0xFFE5BE00),
    buttonSecondary = Color(0xFF111111),
    buttonDisabled = Color(0xFFBDBDBD),
    ripple = Color(0x33FFD400),

    statusActive = Color(0xFF1B8A2A),
    statusSearching = Color(0xFFF57F17),
    statusInactive = Color(0xFF9E9E9E),
    statusError = Color(0xFFC62828),

    mapRoute = Color(0xFFFFD400),
    mapTrack = Color(0xFFFFD400),
    mapPosition = Color(0xFFFFD400),
    mapPositionHalo = Color(0x55FFD400),
    mapPositionStroke = Color(0xFF111111),

    navBarBackground = Color(0xFFFFFFFF),
    navBarSelected = Color(0xFF111111),
    navBarUnselected = Color(0xFF888888),
    navBarIndicator = Color(0xFFFFD400),

    topBarBackground = Color(0xFFFFFFFF),
    topBarContent = Color(0xFF111111)
)

fun darkLoponColors(): LoponColorPalette = LoponColorPalette(
    primaryYellow = Color(0xFFFFD400),
    primaryYellowDark = Color(0xFFE5BE00),
    black = Color(0xFF111111),
    white = Color(0xFFFFFFFF),

    backgroundLight = Color(0xFF1A1A1A),
    backgroundDark = Color(0xFF111111),
    surfaceCard = Color(0xFF252525),
    surfaceCardElevated = Color(0xFF2C2C2C),

    divider = Color(0xFF3A3A3A),
    dividerStrong = Color(0xFF555555),

    success = Color(0xFF4CAF50),
    successLight = Color(0xFF1B3A1F),
    error = Color(0xFFEF5350),
    errorLight = Color(0xFF3E1A1A),
    warning = Color(0xFFFFA726),
    warningLight = Color(0xFF3E2E10),
    info = Color(0xFF42A5F5),

    onSurfacePrimary = Color(0xFFE8E8E8),
    onSurfaceSecondary = Color(0xFFAAAAAA),
    onSurfaceDisabled = Color(0xFF666666),
    onDark = Color(0xFFFFFFFF),
    onDarkSecondary = Color(0xFFCCCCCC),
    onYellow = Color(0xFF111111),

    buttonPrimary = Color(0xFFFFD400),
    buttonPrimaryPressed = Color(0xFFE5BE00),
    buttonSecondary = Color(0xFF333333),
    buttonDisabled = Color(0xFF555555),
    ripple = Color(0x33FFD400),

    statusActive = Color(0xFF4CAF50),
    statusSearching = Color(0xFFFFA726),
    statusInactive = Color(0xFF666666),
    statusError = Color(0xFFEF5350),

    mapRoute = Color(0xFFFFD400),
    mapTrack = Color(0xFFFFD400),
    mapPosition = Color(0xFFFFD400),
    mapPositionHalo = Color(0x55FFD400),
    mapPositionStroke = Color(0xFFFFFFFF),

    navBarBackground = Color(0xFF111111),
    navBarSelected = Color(0xFFFFD400),
    navBarUnselected = Color(0xFF888888),
    navBarIndicator = Color(0xFFFFD400),

    topBarBackground = Color(0xFF111111),
    topBarContent = Color(0xFFFFFFFF)
)

val LocalLoponColors = staticCompositionLocalOf { lightLoponColors() }

object LoponColors {
    val primaryYellow: Color @Composable get() = LocalLoponColors.current.primaryYellow
    val primaryYellowDark: Color @Composable get() = LocalLoponColors.current.primaryYellowDark
    val black: Color @Composable get() = LocalLoponColors.current.black
    val white: Color @Composable get() = LocalLoponColors.current.white

    val backgroundLight: Color @Composable get() = LocalLoponColors.current.backgroundLight
    val backgroundDark: Color @Composable get() = LocalLoponColors.current.backgroundDark
    val surfaceCard: Color @Composable get() = LocalLoponColors.current.surfaceCard
    val surfaceCardElevated: Color @Composable get() = LocalLoponColors.current.surfaceCardElevated

    val divider: Color @Composable get() = LocalLoponColors.current.divider
    val dividerStrong: Color @Composable get() = LocalLoponColors.current.dividerStrong

    val success: Color @Composable get() = LocalLoponColors.current.success
    val successLight: Color @Composable get() = LocalLoponColors.current.successLight
    val error: Color @Composable get() = LocalLoponColors.current.error
    val errorLight: Color @Composable get() = LocalLoponColors.current.errorLight
    val warning: Color @Composable get() = LocalLoponColors.current.warning
    val warningLight: Color @Composable get() = LocalLoponColors.current.warningLight
    val info: Color @Composable get() = LocalLoponColors.current.info

    val onSurfacePrimary: Color @Composable get() = LocalLoponColors.current.onSurfacePrimary
    val onSurfaceSecondary: Color @Composable get() = LocalLoponColors.current.onSurfaceSecondary
    val onSurfaceDisabled: Color @Composable get() = LocalLoponColors.current.onSurfaceDisabled
    val onDark: Color @Composable get() = LocalLoponColors.current.onDark
    val onDarkSecondary: Color @Composable get() = LocalLoponColors.current.onDarkSecondary
    val onYellow: Color @Composable get() = LocalLoponColors.current.onYellow

    val buttonPrimary: Color @Composable get() = LocalLoponColors.current.buttonPrimary
    val buttonPrimaryPressed: Color @Composable get() = LocalLoponColors.current.buttonPrimaryPressed
    val buttonSecondary: Color @Composable get() = LocalLoponColors.current.buttonSecondary
    val buttonDisabled: Color @Composable get() = LocalLoponColors.current.buttonDisabled
    val ripple: Color @Composable get() = LocalLoponColors.current.ripple

    val statusActive: Color @Composable get() = LocalLoponColors.current.statusActive
    val statusSearching: Color @Composable get() = LocalLoponColors.current.statusSearching
    val statusInactive: Color @Composable get() = LocalLoponColors.current.statusInactive
    val statusError: Color @Composable get() = LocalLoponColors.current.statusError

    val mapRoute: Color @Composable get() = LocalLoponColors.current.mapRoute
    val mapTrack: Color @Composable get() = LocalLoponColors.current.mapTrack
    val mapPosition: Color @Composable get() = LocalLoponColors.current.mapPosition
    val mapPositionHalo: Color @Composable get() = LocalLoponColors.current.mapPositionHalo
    val mapPositionStroke: Color @Composable get() = LocalLoponColors.current.mapPositionStroke

    val navBarBackground: Color @Composable get() = LocalLoponColors.current.navBarBackground
    val navBarSelected: Color @Composable get() = LocalLoponColors.current.navBarSelected
    val navBarUnselected: Color @Composable get() = LocalLoponColors.current.navBarUnselected
    val navBarIndicator: Color @Composable get() = LocalLoponColors.current.navBarIndicator

    val topBarBackground: Color @Composable get() = LocalLoponColors.current.topBarBackground
    val topBarContent: Color @Composable get() = LocalLoponColors.current.topBarContent
}
