package com.clockinreminder.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PetrolTealLight,
    onPrimary = SlateDark,
    primaryContainer = PetrolTealContainer,
    onPrimaryContainer = TextPrimaryDark,
    secondary = WarmCopper,
    onSecondary = SlateDark,
    secondaryContainer = WarmCopperDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = PetrolTeal,
    onTertiary = TextPrimaryDark,
    background = SlateDark,
    onBackground = TextPrimaryDark,
    surface = SlateDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = SlateBorder,
    error = ControlledRed,
    onError = TextPrimaryDark,
    errorContainer = ControlledRedContainer,
    onErrorContainer = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = PetrolTeal,
    onPrimary = SandLight,
    primaryContainer = PetrolTealLight.copy(alpha = 0.2f),
    onPrimaryContainer = PetrolTealDark,
    secondary = WarmCopperDark,
    onSecondary = SandLight,
    secondaryContainer = WarmCopper.copy(alpha = 0.2f),
    onSecondaryContainer = WarmCopperDark,
    tertiary = PetrolTealDark,
    onTertiary = SandLight,
    background = SandLight,
    onBackground = TextPrimaryLight,
    surface = SandLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = SandLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = SandBorder,
    error = ControlledRed,
    onError = SandLight,
    errorContainer = ControlledRed.copy(alpha = 0.15f),
    onErrorContainer = ControlledRed
)

@Composable
fun ClockInReminderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TabularTypography,
        content = content
    )
}
