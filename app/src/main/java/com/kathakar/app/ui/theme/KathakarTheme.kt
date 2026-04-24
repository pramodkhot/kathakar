package com.kathakar.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Kathakar brand colors
private val KathakarOrange = Color(0xFFE05C00)
private val KathakarOrangeLight = Color(0xFFFF8A40)

private val DarkColorScheme = darkColorScheme(
    primary         = KathakarOrangeLight,
    onPrimary       = Color(0xFF4A1500),
    primaryContainer = Color(0xFF6B2D00),
    onPrimaryContainer = Color(0xFFFFDBCA),
    secondary       = Color(0xFFFFB783),
    onSecondary     = Color(0xFF4A2500),
    background      = Color(0xFF1C1B1F),
    surface         = Color(0xFF1C1B1F),
    onBackground    = Color(0xFFE6E1E5),
    onSurface       = Color(0xFFE6E1E5),
)

private val LightColorScheme = lightColorScheme(
    primary         = KathakarOrange,
    onPrimary       = Color.White,
    primaryContainer = Color(0xFFFFDBCA),
    onPrimaryContainer = Color(0xFF3A0D00),
    secondary       = Color(0xFF995300),
    onSecondary     = Color.White,
    secondaryContainer = Color(0xFFFFDCBD),
    onSecondaryContainer = Color(0xFF301700),
    background      = Color(0xFFFFFBFF),
    surface         = Color(0xFFFFFBFF),
    onBackground    = Color(0xFF1C1B1F),
    onSurface       = Color(0xFF1C1B1F),
    surfaceVariant  = Color(0xFFF4DED5),
    onSurfaceVariant = Color(0xFF52443D),
)

@Composable
fun KathakarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // keep brand colors consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
