package com.truequapp.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * =========================================================================================
 * 🎨 CONFIGURACIÓN CENTRAL DE LA PALETA DE COLORES - TRUEQUAPP
 * =========================================================================================
 * Este archivo es la FUENTE ÚNICA DE VERDAD para todos los colores de la aplicación.
 * Para cambiar la paleta o ajustar tonalidades, simplemente edita los valores hexadecimales
 * aquí. Todos los componentes, temas (claro/oscuro) y extensiones semánticas se actualizarán
 * automáticamente en toda la app.
 * =========================================================================================
 */
object PaletteConfig {

    // -------------------------------------------------------------------------------------
    // 1. COLORES PRIMARIOS DE MARCA (Identidad: Sostenibilidad, Trueque Ecológico, Confianza)
    // -------------------------------------------------------------------------------------
    val PrimaryDefault       = Color(0xFF059669) // Emerald 600 - Color insignia de Truequapp
    val PrimaryDark          = Color(0xFF047857) // Emerald 700 - Presionado / Sombras
    val PrimaryLight         = Color(0xFF10B981) // Emerald 500 - Acentos y contrastes
    val PrimaryContainerLight= Color(0xFFD1FAE5) // Emerald 100 - Contenedores claros
    val PrimaryContainerDark = Color(0xFF064E3B) // Emerald 900 - Contenedores oscuros

    // -------------------------------------------------------------------------------------
    // 2. COLORES SECUNDARIOS (TruequeCoins, Escrow / Garantía, Transacciones Monetarias)
    // -------------------------------------------------------------------------------------
    val SecondaryDefault     = Color(0xFFD97706) // Amber 600 - Monedero y balances
    val SecondaryLight       = Color(0xFFF59E0B) // Amber 500 - Destacados dorados
    val SecondaryDark        = Color(0xFFB45309) // Amber 700
    val SecondaryContainerLight = Color(0xFFFEF3C7) // Amber 100
    val SecondaryContainerDark  = Color(0xFF78350F) // Amber 900

    // -------------------------------------------------------------------------------------
    // 3. COLORES TERCIARIOS (Trueque Triangular, Algoritmos de Grafos y Coincidencias)
    // -------------------------------------------------------------------------------------
    val TertiaryDefault      = Color(0xFF6366F1) // Indigo 500 - Algoritmos de matching
    val TertiaryLight        = Color(0xFF818CF8) // Indigo 400
    val TertiaryDark         = Color(0xFF4338CA) // Indigo 700
    val TertiaryContainerLight = Color(0xFFE0E7FF) // Indigo 100
    val TertiaryContainerDark  = Color(0xFF312E81) // Indigo 900

    // -------------------------------------------------------------------------------------
    // 4. SUPERFICIES Y FONDOS - MODO OSCURO (Slate Moderno de Alto Contraste)
    // -------------------------------------------------------------------------------------
    val DarkBackground       = Color(0xFF0F172A) // Slate 900 - Fondo principal
    val DarkSurface          = Color(0xFF1E293B) // Slate 800 - Tarjetas y componentes elevados
    val DarkSurfaceVariant   = Color(0xFF334155) // Slate 700 - Bordes y divisiones sutiles
    val DarkOnBackground     = Color(0xFFF8FAFC) // Slate 50 - Texto sobre fondo
    val DarkOnSurface        = Color(0xFFF1F5F9) // Slate 100 - Texto sobre tarjetas
    val DarkOnSurfaceVariant = Color(0xFF94A3B8) // Slate 400 - Texto secundario

    // -------------------------------------------------------------------------------------
    // 5. SUPERFICIES Y FONDOS - MODO CLARO (Blanco Puro y Slate Suave)
    // -------------------------------------------------------------------------------------
    val LightBackground      = Color(0xFFF8FAFC) // Slate 50 - Fondo general
    val LightSurface         = Color(0xFFFFFFFF) // Blanco puro - Tarjetas
    val LightSurfaceVariant  = Color(0xFFF1F5F9) // Slate 100 - Superficies alternas
    val LightOnBackground    = Color(0xFF0F172A) // Slate 900 - Texto principal
    val LightOnSurface       = Color(0xFF1E293B) // Slate 800 - Texto tarjetas
    val LightOnSurfaceVariant= Color(0xFF64748B) // Slate 500 - Texto atenuado

    // -------------------------------------------------------------------------------------
    // 6. ESTADOS Y DISTINTIVOS SEMÁNTICOS ESPECÍFICOS DE TRUEQUAPP
    // -------------------------------------------------------------------------------------
    // Safe Zones (Puntos Seguros de Intercambio)
    val SafeZoneVerified     = Color(0xFF059669) // Verde Esmeralda: Zona vigilada y validada
    val SafeZoneUnverified   = Color(0xFFDC2626) // Rojo: Punto sin validación oficial
    val SafeZonePending      = Color(0xFFEAB308) // Amarillo: Punto en revisión

    // Niveles de Validación KYC
    val KycLevel1Phone       = Color(0xFF0284C7) // Azul Sky: Teléfono y SMS verificado
    val KycLevel2OfficialId  = Color(0xFF7C3AED) // Púrpura Royal: Identificación Oficial (INE/Pasaporte)

    // Estado Físico del Artículo
    val ConditionNew         = Color(0xFF10B981) // Nuevo con empaque
    val ConditionLikeNew     = Color(0xFF3B82F6) // Como nuevo / Excelente
    val ConditionUsedGood    = Color(0xFFF59E0B) // Usado en buen estado
    val ConditionFair        = Color(0xFF64748B) // Aceptable / Regular

    // Alertas y Errores
    val ErrorDefault         = Color(0xFFEF4444) // Red 500
    val ErrorContainerLight  = Color(0xFFFEE2E2)
    val ErrorContainerDark   = Color(0xFF7F1D1D)
    val SuccessDefault       = Color(0xFF10B981) // Green 500
}
