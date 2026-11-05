package com.truequapp.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PaletteConfig.PrimaryDefault,
    onPrimary = PaletteConfig.DarkOnBackground,
    primaryContainer = PaletteConfig.PrimaryContainerDark,
    onPrimaryContainer = PaletteConfig.PrimaryLight,

    secondary = PaletteConfig.SecondaryDefault,
    onSecondary = PaletteConfig.DarkOnBackground,
    secondaryContainer = PaletteConfig.SecondaryContainerDark,
    onSecondaryContainer = PaletteConfig.SecondaryLight,

    tertiary = PaletteConfig.TertiaryDefault,
    onTertiary = PaletteConfig.DarkOnBackground,
    tertiaryContainer = PaletteConfig.TertiaryContainerDark,
    onTertiaryContainer = PaletteConfig.TertiaryLight,

    background = PaletteConfig.DarkBackground,
    onBackground = PaletteConfig.DarkOnBackground,
    surface = PaletteConfig.DarkSurface,
    onSurface = PaletteConfig.DarkOnSurface,
    surfaceVariant = PaletteConfig.DarkSurfaceVariant,
    onSurfaceVariant = PaletteConfig.DarkOnSurfaceVariant,

    error = PaletteConfig.ErrorDefault,
    onError = PaletteConfig.DarkOnBackground,
    errorContainer = PaletteConfig.ErrorContainerDark,
    onErrorContainer = PaletteConfig.ErrorDefault
)

private val LightColorScheme = lightColorScheme(
    primary = PaletteConfig.PrimaryDefault,
    onPrimary = PaletteConfig.LightSurface,
    primaryContainer = PaletteConfig.PrimaryContainerLight,
    onPrimaryContainer = PaletteConfig.PrimaryDark,

    secondary = PaletteConfig.SecondaryDefault,
    onSecondary = PaletteConfig.LightSurface,
    secondaryContainer = PaletteConfig.SecondaryContainerLight,
    onSecondaryContainer = PaletteConfig.SecondaryDark,

    tertiary = PaletteConfig.TertiaryDefault,
    onTertiary = PaletteConfig.LightSurface,
    tertiaryContainer = PaletteConfig.TertiaryContainerLight,
    onTertiaryContainer = PaletteConfig.TertiaryDark,

    background = PaletteConfig.LightBackground,
    onBackground = PaletteConfig.LightOnBackground,
    surface = PaletteConfig.LightSurface,
    onSurface = PaletteConfig.LightOnSurface,
    surfaceVariant = PaletteConfig.LightSurfaceVariant,
    onSurfaceVariant = PaletteConfig.LightOnSurfaceVariant,

    error = PaletteConfig.ErrorDefault,
    onError = PaletteConfig.LightSurface,
    errorContainer = PaletteConfig.ErrorContainerLight,
    onErrorContainer = PaletteConfig.ErrorDefault
)

/**
 * Truequapp Main Theme entry point.
 * Wraps MaterialTheme and provides high-accessibility semantic design tokens.
 */
@Composable
fun TruequappTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val semanticColors = TruequappSemanticColors(
        primary = PaletteConfig.PrimaryDefault,
        truequeCoins = PaletteConfig.SecondaryDefault,
        triangularMatch = PaletteConfig.TertiaryDefault,
        safeZoneVerified = PaletteConfig.SafeZoneVerified,
        safeZoneUnverified = PaletteConfig.SafeZoneUnverified,
        safeZonePending = PaletteConfig.SafeZonePending,
        kycLevel1 = PaletteConfig.KycLevel1Phone,
        kycLevel2 = PaletteConfig.KycLevel2OfficialId,
        conditionNew = PaletteConfig.ConditionNew,
        conditionLikeNew = PaletteConfig.ConditionLikeNew,
        conditionUsed = PaletteConfig.ConditionUsedGood,
        conditionFair = PaletteConfig.ConditionFair,
        success = PaletteConfig.SuccessDefault,
        error = PaletteConfig.ErrorDefault
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalTruequappColors provides semanticColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

/**
 * Global accessor for TruequappTheme design tokens.
 *
 * Example usage:
 * ```
 * Text(
 *     text = "Zona Segura",
 *     color = TruequappTheme.colors.safeZoneVerified
 * )
 * ```
 */
object TruequappTheme {
    val colors: TruequappSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTruequappColors.current

    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes
}
