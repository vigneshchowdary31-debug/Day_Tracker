package com.example.daytracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGradientEnd,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onPrimary = TextPrimaryDark, // Off-white instead of pure white
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = TextSecondaryDark.copy(alpha = 0.5f),
    outlineVariant = SurfaceVariantDark,
    errorContainer = PriorityHigh,
    secondaryContainer = PriorityMedium,
    tertiaryContainer = PriorityLow
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceLight,
    onPrimary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    errorContainer = PriorityHigh,
    secondaryContainer = PriorityMedium,
    tertiaryContainer = PriorityLow
)

@Composable
fun DayTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic colors by default so our custom palette is used
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}