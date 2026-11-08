package com.truequapp.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * =========================================================================================
 * 🎨 CONFIGURACIÓN CENTRAL DE LA PALETA DE COLORES - TRUEQUAPP (Warm Commerce Studio)
 * =========================================================================================
 * Basado en la especificación visual 'Warm Commerce Studio' y el tema táctil obsidian/gold:
 * - Tonos miel, ocre y ámbar para acentos de intercambio y balances de TruequeCoins.
 * - Terracota canela y chocolate tostado para jerarquías y contrastes de alta visibilidad.
 * - Superficies obsidian refinadas (#121214, #1A1A1E) para el modo oscuro y marfil suave (#F9F9F9, #FFFFFF)
 *   para el modo claro.
 * =========================================================================================
 */
object PaletteConfig {

    // -------------------------------------------------------------------------------------
    // 1. COLORES PRIMARIOS DE MARCA (Honey Ochre / Gold Champagne)
    // -------------------------------------------------------------------------------------
    val PrimaryDefault        = Color(0xFFD99F59) // Honey Ochre / Gold Champagne insignia
    val PrimaryDark           = Color(0xFF835414) // Deep Amber Ochre - Presionado / Sombras
    val PrimaryLight          = Color(0xFFF0C987) // Gold Light - Acentos brillantes y bordes
    val PrimaryContainerLight = Color(0xFFFFDDB9) // Ochre 100 - Contenedores claros
    val PrimaryContainerDark  = Color(0xFF5B3700) // Dark Ochre - Contenedores oscuros

    // -------------------------------------------------------------------------------------
    // 2. COLORES SECUNDARIOS (Cinnamon Terracotta / TruequeCoins & Balances)
    // -------------------------------------------------------------------------------------
    val SecondaryDefault      = Color(0xFFA67153) // Cinnamon Terracotta
    val SecondaryLight        = Color(0xFFFEBE9B) // Terracotta Light
    val SecondaryDark         = Color(0xFF835337) // Deep Terracotta
    val SecondaryContainerLight = Color(0xFFFFDBC9) // Soft Terracotta
    val SecondaryContainerDark  = Color(0xFF794B30) // Dark Terracotta Container

    // -------------------------------------------------------------------------------------
    // 3. COLORES TERCIARIOS (Roasted Chocolate / Tipografía y Límites Críticos)
    // -------------------------------------------------------------------------------------
    val TertiaryDefault       = Color(0xFF733F2D) // Roasted Chocolate
    val TertiaryLight         = Color(0xFFFEB59D)
    val TertiaryDark          = Color(0xFF360F03)
    val TertiaryContainerLight = Color(0xFFFFDBD0)
    val TertiaryContainerDark  = Color(0xFF623220)

    // -------------------------------------------------------------------------------------
    // 4. SUPERFICIES Y FONDOS - MODO OSCURO (Obsidian Luxury & Ambient Gold)
    // -------------------------------------------------------------------------------------
    val DarkBackground        = Color(0xFF121214) // Obsidian Canvas principal
    val DarkSurface           = Color(0xFF1A1A1E) // Obsidian Card - Tarjetas y componentes
    val DarkSurfaceVariant    = Color(0xFF212126) // Obsidian Elevated / Bordes (#2A2A30)
    val DarkOnBackground      = Color(0xFFF6F6F8) // Warm White - Texto sobre fondo
    val DarkOnSurface         = Color(0xFFF6F6F8) // Warm White - Texto sobre tarjetas
    val DarkOnSurfaceVariant  = Color(0xFF9E9AA2) // Smoky Bronze / Muted Warm - Texto secundario

    // -------------------------------------------------------------------------------------
    // 5. SUPERFICIES Y FONDOS - MODO CLARO (Warm Minimal / Ivory & White)
    // -------------------------------------------------------------------------------------
    val LightBackground       = Color(0xFFF9F9F9) // Soft Ivory / Bone White
    val LightSurface          = Color(0xFFFFFFFF) // Pure White para tarjetas elevadas
    val LightSurfaceVariant   = Color(0xFFEEEEEE) // Surface Container alterno
    val LightOnBackground     = Color(0xFF1A1C1C) // Deep Graphite
    val LightOnSurface        = Color(0xFF733F2D) // Roasted Chocolate - Lectura cálida
    val LightOnSurfaceVariant = Color(0xFF837567) // Smoky Bronze atenuado

    // -------------------------------------------------------------------------------------
    // 6. ESTADOS Y DISTINTIVOS SEMÁNTICOS ESPECÍFICOS DE TRUEQUAPP
    // -------------------------------------------------------------------------------------
    // Safe Zones (Puntos Seguros de Intercambio)
    val SafeZoneVerified      = Color(0xFF059669) // Verde Esmeralda: Vigilancia y Punto Oficial
    val SafeZoneUnverified    = Color(0xFFDC2626) // Rojo: Sin validación
    val SafeZonePending       = Color(0xFFEAB308) // Ámbar: En revisión

    // Niveles de Validación KYC
    val KycLevel1Phone        = Color(0xFFD99F59) // Gold Ochre: Teléfono verificado
    val KycLevel2OfficialId   = Color(0xFFA67153) // Terracotta: INE / Biométrico

    // Estado Físico del Artículo (Badges)
    val ConditionNew          = Color(0xFFD99F59) // Nuevo con empaque / Dorado
    val ConditionLikeNew      = Color(0xFFE5B869) // Como nuevo
    val ConditionUsedGood     = Color(0xFF8C7A6B) // Usado en buen estado / Smoky Bronze
    val ConditionFair         = Color(0xFF9E9AA2) // Aceptable / Regular

    // Alertas y Errores
    val ErrorDefault          = Color(0xFFBA1A1A) // Warm Red
    val ErrorContainerLight   = Color(0xFFFFDAD6)
    val ErrorContainerDark    = Color(0xFF93000A)
    val SuccessDefault        = Color(0xFF059669) // Green 600

    // Acentos de lujo dorado
    val GoldChampagne         = Color(0xFFE5B869)
    val GoldLight             = Color(0xFFF0C987)
    val GoldDim               = Color(0xFF9E7E45)
    val SmokyBronze           = Color(0xFF8C7A6B)
    val ObsidianBorder        = Color(0xFF2A2A30)
}
