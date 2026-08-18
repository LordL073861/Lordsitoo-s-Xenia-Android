package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val XeniaDarkColorScheme = darkColorScheme(
    primary = XeniaGreen,
    onPrimary = Color(0xFF00391A),
    primaryContainer = XeniaGreenContainer,
    onPrimaryContainer = OnXeniaGreenContainer,
    secondary = TechCyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = TechCyanContainer,
    onSecondaryContainer = Color(0xFF80F2FF),
    tertiary = Color(0xFFE2E8F0),
    onTertiary = Color(0xFF0F172A),
    background = DarkBackground,
    onBackground = TechTextPrimary,
    surface = DarkSurface,
    onSurface = TechTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TechTextSecondary,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = TechBorder,
    outlineVariant = Color(0xFF21262D),
    error = ErrorRed,
    onError = Color(0xFF5F000B)
)

@Composable
fun XeniaAndroidTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MyApplicationTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Xenia emulator UI is engineered around a high-contrast dark aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = XeniaDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

