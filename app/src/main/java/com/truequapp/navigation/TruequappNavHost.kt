package com.truequapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.truequapp.core.designsystem.theme.TruequappTheme
import com.truequapp.feature.auth.mvi.AuthViewModel
import com.truequapp.feature.auth.ui.AuthScreen
import com.truequapp.feature.auth.ui.ProfileScreen
import com.truequapp.feature.catalog.mvi.CatalogViewModel
import com.truequapp.feature.catalog.ui.CatalogFeedScreen
import com.truequapp.feature.catalog.ui.ItemPublishScreen
import com.truequapp.feature.chat.mvi.ChatViewModel
import com.truequapp.feature.chat.ui.ChatScreen
import com.truequapp.feature.trade.mvi.TradeUiEvent
import com.truequapp.feature.trade.mvi.TradeViewModel
import com.truequapp.feature.trade.ui.CreateTradeProposalScreen
import com.truequapp.feature.trade.ui.TradeListScreen
import com.truequapp.feature.trade.ui.TradeNegotiationScreen

@Composable
fun TruequappNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Catalog.route

    val topLevelRoutes = listOf(
        Screen.Catalog.route,
        Screen.Trades.route,
        Screen.SafeZones.route,
        Screen.Profile.route
    )
    val shouldShowBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Catalog.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Marketplace Feed (CU008)
            composable(Screen.Catalog.route) {
                val catalogViewModel: CatalogViewModel = hiltViewModel()
                val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()

                CatalogFeedScreen(
                    state = catalogState,
                    effectFlow = catalogViewModel.uiEffect,
                    onEvent = catalogViewModel::onEvent,
                    onNavigateToItemDetail = { targetItemId ->
                        navController.navigate(Screen.CreateTrade.createRoute(targetItemId))
                    },
                    onNavigateToPublish = {
                        navController.navigate(Screen.PublishItem.route)
                    }
                )
            }

            // Publicación WebP (CU005)
            composable(Screen.PublishItem.route) {
                val catalogViewModel: CatalogViewModel = hiltViewModel()
                val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()

                ItemPublishScreen(
                    state = catalogState,
                    onEvent = catalogViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Listado de Propuestas Activas e Historial
            composable(Screen.Trades.route) {
                val tradeViewModel: TradeViewModel = hiltViewModel()
                val tradeState by tradeViewModel.uiState.collectAsStateWithLifecycle()

                TradeListScreen(
                    state = tradeState,
                    onEvent = tradeViewModel::onEvent,
                    onSelectProposal = { proposalId ->
                        navController.navigate(Screen.TradeNegotiation.createRoute(proposalId))
                    }
                )
            }

            // Proponer Trueque Directo o Híbrido (CU009, RN-02, Opción A)
            composable(
                route = Screen.CreateTrade.route,
                arguments = listOf(navArgument("targetItemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val targetItemId = backStackEntry.arguments?.getString("targetItemId").orEmpty()
                val tradeViewModel: TradeViewModel = hiltViewModel()
                val tradeState by tradeViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(targetItemId) {
                    tradeViewModel.onEvent(TradeUiEvent.InitTradeCreation(targetItemId, tradeState.currentUserId))
                }

                CreateTradeProposalScreen(
                    state = tradeState,
                    effectFlow = tradeViewModel.uiEffect,
                    onEvent = tradeViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNegotiation = { proposalId ->
                        navController.navigate(Screen.TradeNegotiation.createRoute(proposalId)) {
                            popUpTo(Screen.Trades.route)
                        }
                    }
                )
            }

            // Detalle de Negociación (CU010, CU011, CU012, RN-01, RN-07)
            composable(
                route = Screen.TradeNegotiation.route,
                arguments = listOf(navArgument("proposalId") { type = NavType.StringType })
            ) { backStackEntry ->
                val proposalId = backStackEntry.arguments?.getString("proposalId").orEmpty()
                val tradeViewModel: TradeViewModel = hiltViewModel()
                val tradeState by tradeViewModel.uiState.collectAsStateWithLifecycle()

                TradeNegotiationScreen(
                    proposalId = proposalId,
                    state = tradeState,
                    effectFlow = tradeViewModel.uiEffect,
                    onEvent = tradeViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { pId ->
                        navController.navigate(Screen.Chat.createRoute(pId))
                    }
                )
            }

            // Chat Privado Transaccional (CU018, RN-03)
            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("proposalId") { type = NavType.StringType })
            ) { backStackEntry ->
                val proposalId = backStackEntry.arguments?.getString("proposalId").orEmpty()
                val chatViewModel: ChatViewModel = hiltViewModel()
                val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()

                ChatScreen(
                    proposalId = proposalId,
                    state = chatState,
                    effectFlow = chatViewModel.uiEffect,
                    onEvent = chatViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Perfil de Usuario y KYC (CU003, CU004, RN-09, RN-10)
            composable(Screen.Profile.route) {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()

                ProfileScreen(
                    state = authState,
                    onEvent = authViewModel::onEvent
                )
            }

            // Autenticación SMS OTP (CU001, CU023)
            composable(Screen.Auth.route) {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()

                AuthScreen(
                    state = authState,
                    effectFlow = authViewModel.uiEffect,
                    onEvent = authViewModel::onEvent,
                    onNavigateToHome = {
                        navController.navigate(Screen.Catalog.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            // Safe Zones (Fase 3 Placeholder)
            composable(Screen.SafeZones.route) {
                PlaceholderSectionScreen(
                    title = "Safe Zones Geoespaciales",
                    subtitle = "Módulo de Puntos Seguros con geofencing (Fase 3)"
                )
            }
        }
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
