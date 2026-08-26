package com.focusbyrj.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.focusbyrj.app.util.AppThemeManager
import com.focusbyrj.app.util.ThemeMode

@Composable
fun FocusByRjTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val currentAppTheme by AppThemeManager.themeFlow.collectAsState()
    val currentThemeMode by AppThemeManager.themeModeFlow.collectAsState()
    val systemInDark = isSystemInDarkTheme()

    val effectiveDarkTheme = when (currentThemeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = if (effectiveDarkTheme) {
        darkColorScheme(
            primary = currentAppTheme.primary,
            secondary = currentAppTheme.secondary,
            tertiary = currentAppTheme.tertiary,
            background = MidnightBlack,
            surface = SurfaceDark,
            surfaceVariant = SurfaceVariantDark,
            onPrimary = MidnightBlack,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
            onSurfaceVariant = TextSecondary,
            outline = BorderGlass,
            outlineVariant = Color(0x0DFFFFFF)
        )
    } else {
        lightColorScheme(
            primary = currentAppTheme.tertiary,
            secondary = currentAppTheme.primary,
            tertiary = currentAppTheme.secondary,
            background = CanvasLight,
            surface = SurfaceLight,
            surfaceVariant = SurfaceVariantLight,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.Black,
            onBackground = TextPrimaryLight,
            onSurface = TextPrimaryLight,
            onSurfaceVariant = TextSecondaryLight,
            outline = BorderLight,
            outlineVariant = BorderLightVariant
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !effectiveDarkTheme
            insetsController.isAppearanceLightNavigationBars = !effectiveDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
