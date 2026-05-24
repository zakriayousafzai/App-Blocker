package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SkyAccent,
    secondary = Slate500,
    tertiary = EmeraldSuccess,
    background = ObsidianMain,
    surface = ObsidianSurface,
    onPrimary = ObsidianMain,
    onSecondary = Slate50,
    onTertiary = ObsidianMain,
    onBackground = Slate50,
    onSurface = Slate50,
    error = RoseErrorLight,
    onError = ObsidianMain,
    primaryContainer = ObsidianCard,
    secondaryContainer = ObsidianBorder,
    outlineVariant = ObsidianBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Slate900,
    secondary = Slate600,
    tertiary = EmeraldSuccess,
    background = Slate50,
    surface = androidx.compose.ui.graphics.Color.White,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = Slate900,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    onBackground = Slate900,
    onSurface = Slate900,
    error = RoseError,
    onError = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Slate100,
    secondaryContainer = Slate200,
    outlineVariant = Slate200
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamic color flag
    dynamicColor: Boolean = false, // Set default to false to prioritize our gorgeous custom design
    content: @Composable () -> Unit,
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
