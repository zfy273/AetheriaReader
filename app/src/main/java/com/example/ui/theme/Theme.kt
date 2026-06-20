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
    primary = CoralPrimary,
    onPrimary = MidnightBackground,
    primaryContainer = CoralPrimary,
    onPrimaryContainer = MidnightInkText,
    secondary = DarkMutedSlate,
    onSecondary = MidnightInkText,
    background = MidnightBackground,
    onBackground = MidnightInkText,
    surface = DarkGreySurface,
    onSurface = MidnightInkText,
    surfaceVariant = DarkGreySurface,
    onSurfaceVariant = DarkMutedSlate,
    outline = DarkGreyBorder
)

private val LightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = PureWhiteSurface,
    primaryContainer = TerracottaPrimary,
    onPrimaryContainer = InkBlack,
    secondary = MutedSlate,
    onSecondary = InkBlack,
    background = PaperBackground,
    onBackground = InkBlack,
    surface = PureWhiteSurface,
    onSurface = InkBlack,
    surfaceVariant = PaperBackground,
    onSurfaceVariant = MutedSlate,
    outline = WarmGreyBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamicColor to false by default to ensure we display our bespoke Claude Paper / Midnight Ink design rather than device colors
    dynamicColor: Boolean = false,
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
