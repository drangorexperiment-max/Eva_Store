package com.evastore.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = PlayDarkGray,
    onPrimary = PlayWhite,
    primaryContainer = PlayContainerHighLight,
    onPrimaryContainer = PlayDarkGray,
    secondary = PlayGray,
    onSecondary = PlayWhite,
    secondaryContainer = PlayContainerLight,
    onSecondaryContainer = PlayDarkGray,
    background = PlaySurfaceLight,
    onBackground = PlayDarkGray,
    surface = PlaySurfaceLight,
    onSurface = PlayDarkGray,
    surfaceVariant = PlayContainerLight,
    onSurfaceVariant = PlayGray,
    surfaceContainer = PlayContainerLight,
    surfaceContainerHigh = PlayContainerHighLight,
    outline = PlayOutlineLight,
    outlineVariant = PlayOutlineLight,
    error = ErrorColor,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColors = darkColorScheme(
    primary = PlayLightGrayText,
    onPrimary = PlayDarkGray,
    primaryContainer = PlayContainerHighDark,
    onPrimaryContainer = PlayLightGrayText,
    secondary = PlayGrayTextDark,
    onSecondary = PlaySurfaceDark,
    secondaryContainer = PlayContainerDark,
    onSecondaryContainer = PlayLightGrayText,
    background = PlaySurfaceDark,
    onBackground = PlayLightGrayText,
    surface = PlaySurfaceDark,
    onSurface = PlayLightGrayText,
    surfaceVariant = PlayContainerDark,
    onSurfaceVariant = PlayGrayTextDark,
    surfaceContainer = PlayContainerDark,
    surfaceContainerHigh = PlayContainerHighDark,
    outline = PlayOutlineDark,
    outlineVariant = PlayOutlineDark,
    error = Color(0xFFF87171),
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFFECACA)
)

val EvaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun EvaStoreTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = EvaTypography,
        shapes = EvaShapes,
        content = content
    )
}
