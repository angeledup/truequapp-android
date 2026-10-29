package com.truequapp.feature.wallet.mvi

import com.truequapp.core.common.mvi.UiEffect
import com.truequapp.core.common.mvi.UiEvent
import com.truequapp.core.common.mvi.UiState
import com.truequapp.core.domain.model.LedgerEntry
import com.truequapp.core.domain.model.TriangularCycle
import com.truequapp.core.domain.model.WalletAccount

/**
 * Estado inmutable de la pantalla del Monedero Digital TruequeCoins y Trueque Triangular (RN-04, RN-05, RN-06).
 */
data class WalletUiState(
    val wallet: WalletAccount = WalletAccount(
        id = "wallet-demo-01",
        userId = "usr-demo",
        availableBalance = 100.0,
        escrowBalance = 0.0,
        isPinConfigured = true,
        isBiometricsEnabled = false
    ),
    val ledgerHistory: List<LedgerEntry> = emptyList(),
    val triangularCycle: TriangularCycle? = null,
    val showTriangularDialog: Boolean = false,
    val isSettingPin: Boolean = false,
    val isAuthenticating: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) : UiState {
    val totalBalance: Double
        get() = wallet.totalBalance

    val isInvariantPreserved: Boolean
        get() = (wallet.availableBalance + wallet.escrowBalance) == wallet.totalBalance
}

/**
 * Eventos e intenciones del usuario en el Monedero.
 */
sealed interface WalletUiEvent : UiEvent {
    data class LoadWallet(val userId: String) : WalletUiEvent
    data class SetSecurityPin(val pin: String) : WalletUiEvent
    data class ToggleBiometrics(val enabled: Boolean) : WalletUiEvent
    data class RequestEscrowHold(val amount: Double, val tradeId: String) : WalletUiEvent
    data class RequestEscrowRelease(val amount: Double, val sellerWalletId: String, val tradeId: String) : WalletUiEvent
    data class SignTriangularCycle(val cycleId: String, val userId: String) : WalletUiEvent
    data class AbortTriangularCycle(val cycleId: String, val userId: String) : WalletUiEvent
    data object OpenTriangularDetails : WalletUiEvent
    data object CloseTriangularDetails : WalletUiEvent
    data object DismissMessages : WalletUiEvent
}

/**
 * Efectos secundarios de navegación y seguridad.
 */
sealed interface WalletUiEffect : UiEffect {
    data class ShowToast(val message: String) : WalletUiEffect
    data object TriggerBiometricPrompt : WalletUiEffect
    data class RequirePinAuthentication(val onSuccessAction: String) : WalletUiEffect
    data class TriangularCycleCompleted(val cycleId: String) : WalletUiEffect
}

