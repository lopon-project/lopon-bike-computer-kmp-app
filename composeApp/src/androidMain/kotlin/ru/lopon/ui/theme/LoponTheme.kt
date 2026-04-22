package ru.lopon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import ru.lopon.domain.model.ThemeMode

@Composable
private fun loponLightColorScheme() = lightColorScheme(
    primary = LoponColors.primaryYellow,
    onPrimary = LoponColors.onYellow,
    primaryContainer = LoponColors.primaryYellow,
    onPrimaryContainer = LoponColors.onYellow,
    secondary = LoponColors.black,
    onSecondary = LoponColors.onDark,
    secondaryContainer = LoponColors.black,
    onSecondaryContainer = LoponColors.onDark,
    tertiary = LoponColors.info,
    onTertiary = LoponColors.white,
    background = LoponColors.backgroundLight,
    onBackground = LoponColors.onSurfacePrimary,
    surface = LoponColors.white,
    onSurface = LoponColors.onSurfacePrimary,
    surfaceVariant = LoponColors.surfaceCardElevated,
    onSurfaceVariant = LoponColors.onSurfaceSecondary,
    error = LoponColors.error,
    onError = LoponColors.white,
    errorContainer = LoponColors.errorLight,
    onErrorContainer = LoponColors.error,
    outline = LoponColors.divider,
    outlineVariant = LoponColors.dividerStrong,
)

@Composable
private fun loponDarkColorScheme() = darkColorScheme(
    primary = LoponColors.primaryYellow,
    onPrimary = LoponColors.onYellow,
    primaryContainer = LoponColors.primaryYellow,
    onPrimaryContainer = LoponColors.onYellow,
    secondary = LoponColors.buttonSecondary,
    onSecondary = LoponColors.onDark,
    secondaryContainer = LoponColors.buttonSecondary,
    onSecondaryContainer = LoponColors.onDark,
    tertiary = LoponColors.info,
    onTertiary = LoponColors.white,
    background = LoponColors.backgroundLight,
    onBackground = LoponColors.onSurfacePrimary,
    surface = LoponColors.surfaceCard,
    onSurface = LoponColors.onSurfacePrimary,
    surfaceVariant = LoponColors.surfaceCardElevated,
    onSurfaceVariant = LoponColors.onSurfaceSecondary,
    error = LoponColors.error,
    onError = LoponColors.white,
    errorContainer = LoponColors.errorLight,
    onErrorContainer = LoponColors.error,
    outline = LoponColors.divider,
    outlineVariant = LoponColors.dividerStrong,
)

private val LoponMaterialTypography = Typography(
    displayLarge = LoponTypography.heroSpeed,
    headlineLarge = LoponTypography.screenTitle,
    headlineMedium = LoponTypography.sectionTitle,
    titleLarge = LoponTypography.sectionTitle,
    titleMedium = LoponTypography.body,
    bodyLarge = LoponTypography.body,
    bodyMedium = LoponTypography.caption,
    labelLarge = LoponTypography.button,
    labelMedium = LoponTypography.navLabel,
    labelSmall = LoponTypography.metricLabel,
)

@Composable
fun LoponTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val loponColors = if (darkTheme) darkLoponColors() else lightLoponColors()

    CompositionLocalProvider(LocalLoponColors provides loponColors) {
        val colorScheme = if (darkTheme) loponDarkColorScheme() else loponLightColorScheme()

        MaterialTheme(
            colorScheme = colorScheme,
            typography = LoponMaterialTypography,
            content = content
        )
    }
}
