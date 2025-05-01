// AppTheme.kt
package com.wavehitech.aptracker

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import com.wavehitech.aptracker.ui.theme.PrimaryDark
import com.wavehitech.aptracker.ui.theme.OnPrimaryDark
import com.wavehitech.aptracker.ui.theme.SecondaryDark
import com.wavehitech.aptracker.ui.theme.OnSecondaryDark
import com.wavehitech.aptracker.ui.theme.TertiaryDark
import com.wavehitech.aptracker.ui.theme.OnTertiaryDark
import com.wavehitech.aptracker.ui.theme.BackgroundDark
import com.wavehitech.aptracker.ui.theme.SurfaceDark
import com.wavehitech.aptracker.ui.theme.PrimaryLight
import com.wavehitech.aptracker.ui.theme.OnPrimaryLight
import com.wavehitech.aptracker.ui.theme.SecondaryLight
import com.wavehitech.aptracker.ui.theme.OnSecondaryLight
import com.wavehitech.aptracker.ui.theme.TertiaryLight
import com.wavehitech.aptracker.ui.theme.OnTertiaryLight
import com.wavehitech.aptracker.ui.theme.BackgroundLight
import com.wavehitech.aptracker.ui.theme.SurfaceLight
import com.wavehitech.aptracker.ui.theme.Typography

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryDark.copy(alpha = 0.8f),
    onPrimaryContainer = OnPrimaryDark,

    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryDark.copy(alpha = 0.8f),
    onSecondaryContainer = OnSecondaryDark,

    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryDark.copy(alpha = 0.8f),
    onTertiaryContainer = OnTertiaryDark,

    background = BackgroundDark,
    onBackground = Color(0xFFE1E1E1),
    surface = SurfaceDark,
    onSurface = Color(0xFFE1E1E1),

    surfaceVariant = SurfaceDark.copy(alpha = 0.7f),
    onSurfaceVariant = Color(0xFFE1E1E1),
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryLight.copy(alpha = 0.8f),
    onPrimaryContainer = OnPrimaryLight,

    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryLight.copy(alpha = 0.8f),
    onSecondaryContainer = OnSecondaryLight,

    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryLight.copy(alpha = 0.8f),
    onTertiaryContainer = OnTertiaryLight,

    background = BackgroundLight,
    onBackground = Color(0xFF1A1C1E),
    surface = SurfaceLight,
    onSurface = Color(0xFF1A1C1E),

    surfaceVariant = SurfaceLight.copy(alpha = 0.7f),
    onSurfaceVariant = Color(0xFF1A1C1E),
)

@Composable
fun AIAPTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false by default to use our brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Optional: Set system bars to match theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}