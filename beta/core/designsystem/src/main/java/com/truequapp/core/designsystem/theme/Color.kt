package com.truequapp.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Custom semantic colors designed specifically for Truequapp domain features.
 * Accessible from any Composable using:
 *
 * `TruequappTheme.colors.safeZoneVerified`
 * `TruequappTheme.colors.truequeCoins`
 * `TruequappTheme.colors.kycLevel2`
 */
@Immutable
data class TruequappSemanticColors(
    val primary: Color = PaletteConfig.PrimaryDefault,
    val truequeCoins: Color = PaletteConfig.SecondaryDefault,
    val triangularMatch: Color = PaletteConfig.TertiaryDefault,
    val safeZoneVerified: Color = PaletteConfig.SafeZoneVerified,
    val safeZoneUnverified: Color = PaletteConfig.SafeZoneUnverified,
    val safeZonePending: Color = PaletteConfig.SafeZonePending,
    val kycLevel1: Color = PaletteConfig.KycLevel1Phone,
    val kycLevel2: Color = PaletteConfig.KycLevel2OfficialId,
    val conditionNew: Color = PaletteConfig.ConditionNew,
    val conditionLikeNew: Color = PaletteConfig.ConditionLikeNew,
    val conditionUsed: Color = PaletteConfig.ConditionUsedGood,
    val conditionFair: Color = PaletteConfig.ConditionFair,
    val success: Color = PaletteConfig.SuccessDefault,
    val error: Color = PaletteConfig.ErrorDefault,
)

val LocalTruequappColors = staticCompositionLocalOf {
    TruequappSemanticColors()
}
