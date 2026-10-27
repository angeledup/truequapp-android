package com.truequapp.navigation

/**
 * Destinations for Jetpack Compose Navigation in Truequapp.
 */
sealed class Screen(val route: String, val title: String) {
    data object Splash : Screen("splash", "Bienvenida")
    data object Auth : Screen("auth", "Iniciar Sesión")
    data object Catalog : Screen("catalog", "Explorar Trueques")
    data object Trades : Screen("trades", "Mis Trueques")
    data object SafeZones : Screen("safe_zones", "Puntos Seguros")
    data object Profile : Screen("profile", "Mi Perfil")
}
