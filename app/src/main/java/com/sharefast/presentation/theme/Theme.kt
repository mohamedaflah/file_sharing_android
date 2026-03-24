package com.sharefast.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = BrandSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = BrandTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE4F1),
    onTertiaryContainer = Color(0xFF831843),
    background = LightBackground,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurfaceContainer,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFFCBD5E1),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFE2E8F0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF6EE7B7),
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFFF9A8D4),
    onTertiary = Color(0xFF500724),
    tertiaryContainer = Color(0xFF831843),
    onTertiaryContainer = Color(0xFFFFE4F1),
    background = DarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurfaceContainer,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFF334155),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155),
)

private val AmoledColors = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF6EE7B7),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFFF9A8D4),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF831843),
    onTertiaryContainer = Color(0xFFFFE4F1),
    background = AmoledBlack,
    onBackground = Color(0xFFF8FAFC),
    surface = AmoledBlack,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF0A0A0A),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = AmoledBlack,
    surfaceContainerLow = Color(0xFF050505),
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerHigh = Color(0xFF121212),
    surfaceContainerHighest = Color(0xFF1C1C1C),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF1E293B),
)

@Composable
fun ShareFastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoled: Boolean = false,
    accentPresetIndex: Int = 0,
    content: @Composable () -> Unit,
) {
    val base = when {
        amoled -> AmoledColors
        darkTheme -> DarkColors
        else -> LightColors
    }
    val colors = base.withAccent(accentPresetIndex, useDarkPalette = darkTheme || amoled)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme && !amoled
        }
    }
    val typography = remember {
        runCatching { ShareFastTypography }.getOrElse { ShareFastTypographySans }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        content = content,
    )
}
