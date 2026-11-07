package com.truequapp.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
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
import com.truequapp.auth.ui.LoginRoute
import com.truequapp.auth.ui.LoginViewModel
import com.truequapp.auth.ui.RegisterRoute
import com.truequapp.auth.ui.RegisterViewModel
import com.truequapp.core.designsystem.theme.TruequappTheme
import com.truequapp.feature.catalog.mvi.CatalogViewModel
import com.truequapp.feature.catalog.ui.CatalogFeedScreen
import com.truequapp.feature.catalog.ui.ItemPublishScreen
import com.truequapp.feature.chat.mvi.ChatListViewModel
import com.truequapp.feature.chat.mvi.ChatViewModel
import com.truequapp.feature.chat.ui.ChatListScreen
import com.truequapp.feature.chat.ui.ChatScreen
import com.truequapp.feature.trade.mvi.TradeViewModel
import com.truequapp.feature.trade.ui.CreateTradeProposalScreen
import com.truequapp.feature.trade.ui.TradeListScreen
import com.truequapp.feature.trade.ui.TradeNegotiationScreen
import com.truequapp.profile.ui.ProfileRoute
import com.truequapp.profile.ui.ProfileViewModel

@Composable
fun TruequappNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Login.route

    val topLevelRoutes = listOf(
        Screen.Catalog.route,
        Screen.Trades.route,
        Screen.Messages.route,
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
                        Triple(Screen.Messages, Icons.AutoMirrored.Filled.Chat, "Mensajes"),
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
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Pantalla 1: Inicio de Sesión (Valida contra SQLite Room)
            composable(Screen.Login.route) {
                val loginViewModel: LoginViewModel = hiltViewModel()
                LoginRoute(
                    viewModel = loginViewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Catalog.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    }
                )
            }

            // Pantalla 2: Registro de Cuenta Nueva (Persiste en SQLite Room con 7 columnas)
            composable(Screen.Register.route) {
                val registerViewModel: RegisterViewModel = hiltViewModel()
                RegisterRoute(
                    viewModel = registerViewModel,
                    onNavigateBackToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                )
            }

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

                LaunchedEffect(Unit) {
                    tradeViewModel.uiEffect.collect { effect ->
                        when (effect) {
                            is com.truequapp.feature.trade.mvi.TradeUiEffect.NavigateToChat -> {
                                navController.navigate(Screen.Chat.createRoute(effect.proposalId))
                            }
                            else -> Unit
                        }
                    }
                }

                TradeListScreen(
                    state = tradeState,
                    onEvent = tradeViewModel::onEvent,
                    onSelectProposal = { proposalId ->
                        navController.navigate(Screen.TradeNegotiation.createRoute(proposalId))
                    }
                )
            }

            // Crear Propuesta de Trueque 1:1 o N:1 (CU009, RN-02)
            composable(
                route = Screen.CreateTrade.route,
                arguments = listOf(navArgument("targetItemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val targetItemId = backStackEntry.arguments?.getString("targetItemId").orEmpty()
                val tradeViewModel: TradeViewModel = hiltViewModel()
                val tradeState by tradeViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(targetItemId) {
                    if (targetItemId.isNotBlank()) {
                        tradeViewModel.onEvent(com.truequapp.feature.trade.mvi.TradeUiEvent.InitTradeCreation(targetItemId, "usuario_actual"))
                    }
                }

                CreateTradeProposalScreen(
                    state = tradeState,
                    effectFlow = tradeViewModel.uiEffect,
                    onEvent = tradeViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNegotiation = { pId ->
                        navController.navigate(Screen.TradeNegotiation.createRoute(pId)) {
                            popUpTo(Screen.Catalog.route)
                        }
                    }
                )
            }

            // Detalle y Negociación de Propuesta (CU010, CU011, CU012, RN-01, RN-07)
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

            // Menú Principal: Listado de Mensajes / Chats Transaccionales (Módulo 4)
            composable(Screen.Messages.route) {
                val chatListViewModel: ChatListViewModel = hiltViewModel()
                val chatListState by chatListViewModel.uiState.collectAsStateWithLifecycle()

                ChatListScreen(
                    state = chatListState,
                    effectFlow = chatListViewModel.uiEffect,
                    onEvent = chatListViewModel::onEvent,
                    onNavigateToChat = { proposalId ->
                        navController.navigate(Screen.Chat.createRoute(proposalId))
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

            // Perfil de Usuario con información real de la base de datos SQL
            composable(Screen.Profile.route) {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                ProfileRoute(
                    viewModel = profileViewModel,
                    onLogoutSuccess = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
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
