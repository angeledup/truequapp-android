package com.truequapp.navigation

/**
 * Destinations for Jetpack Compose Navigation in Truequapp.
 */
sealed class Screen(val route: String, val title: String) {
    data object Splash : Screen("splash", "Bienvenida")
    data object Auth : Screen("auth", "Iniciar Sesión")
    data object Catalog : Screen("catalog", "Explorar Trueques")
    data object Swipe : Screen("swipe", "Modo Swipe")
    data object PublishItem : Screen("publish_item", "Publicar Artículo")
    data object Trades : Screen("trades", "Mis Trueques")
    data object Wallet : Screen("wallet", "Monedero TC")
    data object CreateTrade : Screen("create_trade/{targetItemId}", "Proponer Trueque") {
        fun createRoute(targetItemId: String) = "create_trade/$targetItemId"
    }
    data object TradeNegotiation : Screen("trade_negotiation/{proposalId}", "Detalle de Negociación") {
        fun createRoute(proposalId: String) = "trade_negotiation/$proposalId"
    }
    data object Chat : Screen("chat/{proposalId}", "Chat Transaccional") {
        fun createRoute(proposalId: String) = "chat/$proposalId"
    }
    data object ChatList : Screen("chat_list", "Chats")
    data object SafeZones : Screen("safe_zones", "Puntos Seguros")
    data object QrExchange : Screen("qr_exchange/{tradeId}", "Confirmación QR") {
        fun createRoute(tradeId: String) = "qr_exchange/$tradeId"
    }
    data object TradeReview : Screen("trade_review/{tradeId}", "Calificar Trueque") {
        fun createRoute(tradeId: String) = "trade_review/$tradeId"
    }
    data object Dispute : Screen("dispute/{tradeId}", "Reportar Disputa") {
        fun createRoute(tradeId: String) = "dispute/$tradeId"
    }
    data object AccountDeletion : Screen("account_deletion", "Baja de Cuenta")
    data object Reports : Screen("reports", "Panel de Liquidez")
    data object Profile : Screen("profile", "Mi Perfil")
}
