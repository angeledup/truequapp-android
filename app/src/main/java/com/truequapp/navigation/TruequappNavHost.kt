package com.truequapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.Modifier
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
import com.truequapp.feature.chat.ui.ChatListScreen
import com.truequapp.feature.chat.ui.ChatScreen
import com.truequapp.feature.reports.ui.LiquidityDashboardScreen
import com.truequapp.feature.safezones.mvi.SafeZoneViewModel
import com.truequapp.feature.safezones.ui.AccountDeletionScreen
import com.truequapp.feature.safezones.ui.DisputeReportScreen
import com.truequapp.feature.safezones.ui.QrExchangeScreen
import com.truequapp.feature.safezones.ui.SafeZoneSelectionScreen
import com.truequapp.feature.safezones.ui.TradeReviewScreen
import com.truequapp.feature.swipe.ui.SwipeScreen
import com.truequapp.feature.trade.mvi.TradeUiEvent
import com.truequapp.feature.trade.mvi.TradeViewModel
import com.truequapp.feature.trade.ui.CreateTradeProposalScreen
import com.truequapp.feature.trade.ui.TradeListScreen
import com.truequapp.feature.trade.ui.TradeNegotiationScreen
import com.truequapp.feature.wallet.ui.WalletScreen

@Composable
fun TruequappNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Catalog.route

    val topLevelRoutes = listOf(
        Screen.Catalog.route,
        Screen.Swipe.route,
        Screen.Trades.route,
        Screen.ChatList.route,
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
                        Triple(Screen.Swipe, Icons.Default.AutoAwesome, "Swipe"),
                        Triple(Screen.Trades, Icons.Default.SwapHoriz, "Trueques"),
                        Triple(Screen.ChatList, Icons.AutoMirrored.Filled.Chat, "Chats"),
                        Triple(Screen.Profile, Icons.Default.AccountCircle, "Perfil")
                    )

                    items.forEach { (screen, icon, label) ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp) },
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
            startDestination = Screen.Auth.route,
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
                    },
                    onCoinsClick = {
                        navController.navigate(Screen.Wallet.route)
                    }
                )
            }

            // Modo Exploración Swipe (CU017 / Fase 4)
            composable(Screen.Swipe.route) {
                SwipeScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProposal = { targetItemId ->
                        navController.navigate(Screen.CreateTrade.createRoute(targetItemId))
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
                    },
                    onNavigateToChat = { proposalId ->
                        navController.navigate(Screen.Chat.createRoute(proposalId))
                    }
                )
            }

            // Bandeja de Chats Transaccionales entre Usuarios (CU018)
            composable(Screen.ChatList.route) {
                ChatListScreen(
                    onNavigateToChat = { proposalId ->
                        navController.navigate(Screen.Chat.createRoute(proposalId))
                    },
                    onNavigateToCatalog = {
                        navController.navigate(Screen.Catalog.route)
                    }
                )
            }

            // Monedero Digital TruequeCoins y Custodia Escrow (CU013, CU014, CU015, CU016 / Fase 4)
            composable(Screen.Wallet.route) {
                WalletScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTrades = { navController.navigate(Screen.Trades.route) }
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
                    },
                    onNavigateToSafeZone = { _ ->
                        navController.navigate(Screen.SafeZones.route)
                    },
                    onNavigateToQr = { tradeId ->
                        navController.navigate(Screen.QrExchange.createRoute(tradeId))
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
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSafeZone = {
                        navController.navigate(Screen.SafeZones.route)
                    }
                )
            }

            // Puntos Seguros Geoespaciales Google Maps (CU019 / Fase 3)
            composable(Screen.SafeZones.route) {
                val safeZoneViewModel: SafeZoneViewModel = hiltViewModel()
                val safeZoneState by safeZoneViewModel.uiState.collectAsStateWithLifecycle()

                SafeZoneSelectionScreen(
                    tradeId = "trade-default-01",
                    currentUserId = "usr-demo",
                    counterpartUserId = "usr-counterpart",
                    state = safeZoneState,
                    effectFlow = safeZoneViewModel.uiEffect,
                    onEvent = safeZoneViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQr = { tradeId ->
                        navController.navigate(Screen.QrExchange.createRoute(tradeId))
                    }
                )
            }

            // Confirmación Presencial QR y OTP Fallback (CU020 / Fase 3)
            composable(
                route = Screen.QrExchange.route,
                arguments = listOf(navArgument("tradeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tradeId = backStackEntry.arguments?.getString("tradeId").orEmpty()
                val safeZoneViewModel: SafeZoneViewModel = hiltViewModel()
                val safeZoneState by safeZoneViewModel.uiState.collectAsStateWithLifecycle()

                QrExchangeScreen(
                    tradeId = tradeId,
                    state = safeZoneState,
                    effectFlow = safeZoneViewModel.uiEffect,
                    onEvent = safeZoneViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToReview = { tId ->
                        navController.navigate(Screen.TradeReview.createRoute(tId))
                    },
                    onNavigateToDispute = { tId ->
                        navController.navigate(Screen.Dispute.createRoute(tId))
                    }
                )
            }

            // Calificación Doble Ciega Pública (CU020, RN-12 / Fase 3)
            composable(
                route = Screen.TradeReview.route,
                arguments = listOf(navArgument("tradeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tradeId = backStackEntry.arguments?.getString("tradeId").orEmpty()
                val safeZoneViewModel: SafeZoneViewModel = hiltViewModel()
                val safeZoneState by safeZoneViewModel.uiState.collectAsStateWithLifecycle()

                TradeReviewScreen(
                    tradeId = tradeId,
                    state = safeZoneState,
                    effectFlow = safeZoneViewModel.uiEffect,
                    onEvent = safeZoneViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Gestión de Disputas y Anti-Spoofing (CU022, RN-08 / Fase 3)
            composable(
                route = Screen.Dispute.route,
                arguments = listOf(navArgument("tradeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tradeId = backStackEntry.arguments?.getString("tradeId").orEmpty()
                val safeZoneViewModel: SafeZoneViewModel = hiltViewModel()
                val safeZoneState by safeZoneViewModel.uiState.collectAsStateWithLifecycle()

                DisputeReportScreen(
                    tradeId = tradeId,
                    state = safeZoneState,
                    effectFlow = safeZoneViewModel.uiEffect,
                    onEvent = safeZoneViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Baja de Cuenta y Supresión de Datos GDPR (CU002 / Fase 3)
            composable(Screen.AccountDeletion.route) {
                val safeZoneViewModel: SafeZoneViewModel = hiltViewModel()
                val safeZoneState by safeZoneViewModel.uiState.collectAsStateWithLifecycle()

                AccountDeletionScreen(
                    state = safeZoneState,
                    onEvent = safeZoneViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Panel de Liquidez & Exportación PDF (CU021 / Fase 5)
            composable(Screen.Reports.route) {
                LiquidityDashboardScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Perfil de Usuario y KYC (CU003, CU004, RN-09, RN-10)
            composable(Screen.Profile.route) {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()

                ProfileScreen(
                    state = authState,
                    onEvent = authViewModel::onEvent,
                    onNavigateToWallet = { navController.navigate(Screen.Wallet.route) },
                    onNavigateToSafeZones = { navController.navigate(Screen.SafeZones.route) },
                    onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                    onNavigateToAccountDeletion = { navController.navigate(Screen.AccountDeletion.route) },
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
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
        }
    }
}
