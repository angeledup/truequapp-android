package com.truequapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.truequapp.core.designsystem.component.ConditionTag
import com.truequapp.core.designsystem.component.ItemCondition
import com.truequapp.core.designsystem.component.KycLevel
import com.truequapp.core.designsystem.component.SafeZoneChip
import com.truequapp.core.designsystem.component.TruequappBadge
import com.truequapp.core.designsystem.component.TruequappPrimaryButton
import com.truequapp.core.designsystem.component.TruequappTopAppBar
import com.truequapp.core.designsystem.theme.TruequappTheme

@Composable
fun TruequappNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Catalog.route

    Scaffold(
        topBar = {
            TruequappTopAppBar(
                title = "Truequapp",
                coinsBalance = 150,
                unreadNotifications = 3,
                onCoinsClick = { /* Navigate to wallet in Phase 4 */ },
                onNotificationsClick = { /* Open notifications */ }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = TruequappTheme.colorScheme.surface,
                contentColor = TruequappTheme.colorScheme.onSurface
            ) {
                val items = listOf(
                    Triple(Screen.Catalog, Icons.Default.Home, "Catálogo"),
                    Triple(Screen.Trades, Icons.Default.SwapHoriz, "Trueques"),
                    Triple(Screen.SafeZones, Icons.Default.Place, "Zonas"),
                    Triple(Screen.Profile, Icons.Default.AccountCircle, "Perfil")
                )

                items.forEach { (screen, icon, label) ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp) },
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TruequappTheme.colorScheme.primary,
                            selectedTextColor = TruequappTheme.colorScheme.primary,
                            indicatorColor = TruequappTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Catalog.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Catalog.route) {
                CatalogShowcaseScreen()
            }
            composable(Screen.Trades.route) {
                PlaceholderSectionScreen(title = "Mis Trueques", subtitle = "Módulo activo en Fase 2")
            }
            composable(Screen.SafeZones.route) {
                PlaceholderSectionScreen(title = "Safe Zones Geoespaciales", subtitle = "Módulo activo en Fase 3")
            }
            composable(Screen.Profile.route) {
                PlaceholderSectionScreen(title = "Perfil de Usuario & KYC", subtitle = "Módulo activo en Fase 2")
            }
        }
    }
}

/**
 * Visual showcase verifying that Design System tokens, components, and
 * the easily editable color palette are working seamlessly.
 */
@Composable
private fun CatalogShowcaseScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Architecture Status Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = TruequappTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = TruequappTheme.colors.safeZoneVerified,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fase 1: Arquitectura Base Lista",
                        style = TruequappTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TruequappTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "MVI Base, Inyección de Dependencias Hilt, Red Retrofit/OkHttp, DataStore y Design System Material 3 integrados.",
                    style = TruequappTheme.typography.bodyMedium,
                    color = TruequappTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Color Palette Fast-Edit Demo Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = TruequappTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = null,
                        tint = TruequappTheme.colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Paleta de Colores de Fácil Edición",
                        style = TruequappTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Centralizada en PaletteConfig.kt. Cualquier cambio en sus valores hexadecimales se refleja en toda la aplicación.",
                    style = TruequappTheme.typography.bodyMedium,
                    color = TruequappTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Color Swatches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PaletteSwatch(label = "Primary", color = TruequappTheme.colors.primary)
                    PaletteSwatch(label = "TruequeCoins", color = TruequappTheme.colors.truequeCoins)
                    PaletteSwatch(label = "Triangular", color = TruequappTheme.colors.triangularMatch)
                    PaletteSwatch(label = "KYC Level 2", color = TruequappTheme.colors.kycLevel2)
                }
            }
        }

        // Reusable Components Showcase
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = TruequappTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Componentes de Design System",
                    style = TruequappTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                // Badges
                Text(text = "Insignias KYC:", style = TruequappTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TruequappBadge(level = KycLevel.LEVEL_1_PHONE)
                    TruequappBadge(level = KycLevel.LEVEL_2_OFFICIAL_ID)
                }

                // Safe Zones
                Text(text = "Safe Zones Geoespaciales:", style = TruequappTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SafeZoneChip(isVerified = true, zoneName = "Metro Insurgentes (Vigilado)")
                    SafeZoneChip(isVerified = false)
                }

                // Condition Tags
                Text(text = "Estado de Artículos:", style = TruequappTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConditionTag(condition = ItemCondition.NEW)
                    ConditionTag(condition = ItemCondition.LIKE_NEW)
                    ConditionTag(condition = ItemCondition.USED_GOOD)
                    ConditionTag(condition = ItemCondition.FAIR)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons
                TruequappPrimaryButton(
                    text = "Proponer Trueque",
                    onClick = { /* Demo click */ }
                )
            }
        }
    }
}

@Composable
private fun PaletteSwatch(label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = TruequappTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlaceholderSectionScreen(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.GppGood,
                contentDescription = null,
                tint = TruequappTheme.colors.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = TruequappTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = TruequappTheme.typography.bodyMedium,
                color = TruequappTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
