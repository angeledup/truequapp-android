package com.truequapp.feature.trade.mvi

import com.truequapp.core.common.mvi.UiEffect
import com.truequapp.core.common.mvi.UiEvent
import com.truequapp.core.common.mvi.UiState
import com.truequapp.core.domain.model.MeetingAppointment
import com.truequapp.core.domain.model.TradeItem
import com.truequapp.core.domain.model.TradeProposal
import com.truequapp.core.domain.model.UserProfile

/**
 * Estado inmutable de la interfaz de usuario para el módulo de negociación de trueques.
 */
data class TradeUiState(
    // Datos para creación de propuesta (CU009)
    val targetItem: TradeItem? = null,
    val currentUserId: String = "",
    val availableUserItems: List<TradeItem> = emptyList(),
    val selectedOfferedItemIds: Set<String> = emptySet(),
    val truequeCoinsInput: String = "0",
    val truequeCoins: Double = 100.0,
    val calculatedRatio: Double = 0.0,
    val isRatioValid: Boolean = false, // RN-02: ratio >= 0.40
    val hasMinimumItemsSelected: Boolean = false, // Opción A: mínimo 1 artículo
    val canSubmitProposal: Boolean = false, // isRatioValid && hasMinimumItemsSelected

    // Datos para gestión y listado de propuestas (CU010 / CU011 / CU012)
    // Pestañas 'Activas' y 'Ofertas' solicitadas por el usuario
    val activeProposals: List<TradeProposal> = emptyList(),
    val receivedOffers: List<TradeProposal> = emptyList(),
    val historyProposals: List<TradeProposal> = emptyList(),
    val selectedTab: Int = 0, // 0: Activas, 1: Ofertas
    val currentProposal: TradeProposal? = null,
    val negotiationTargetItem: TradeItem? = null,
    val negotiationOfferedItems: List<TradeItem> = emptyList(),
    val scheduledAppointment: MeetingAppointment? = null,
    val currentUserProfile: UserProfile? = null,

    // Diálogos y advertencias
    val showCounterOfferDialog: Boolean = false,
    val counterOfferOfferedItemIds: Set<String> = emptySet(),
    val counterOfferTruequeCoinsInput: String = "0",
    val counterOfferRatio: Double = 0.0,
    val isCounterOfferValid: Boolean = false,

    val showCancelDialog: Boolean = false,
    val cancellationWarningMessage: String? = null,
    val isPenaltyCancellation: Boolean = false,

    // Estados generales
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) : UiState {
    // Getters retrocompatibles para unificación de variable de moneda
    val compensatoryCoinsInput: String get() = truequeCoinsInput
    val userCoinsBalance: Double get() = truequeCoins
    val counterOfferCoinsInput: String get() = counterOfferTruequeCoinsInput
}

/**
 * Eventos e intenciones del usuario para el flujo de negociación de trueques.
 */
sealed interface TradeUiEvent : UiEvent {
    // Creación de propuesta (CU009)
    data class InitTradeCreation(val targetItemId: String, val userId: String) : TradeUiEvent
    data class ToggleOfferedItemSelection(val itemId: String) : TradeUiEvent
    data class OnTruequeCoinsChanged(val truequeCoinsStr: String) : TradeUiEvent
    data class OnCompensatoryCoinsChanged(val coinsStr: String) : TradeUiEvent
    data object SubmitProposal : TradeUiEvent

    // Listado y consulta (Pestañas 'Activas' y 'Ofertas')
    data class LoadProposals(val userId: String) : TradeUiEvent
    data class SelectTab(val tabIndex: Int) : TradeUiEvent
    data class LoadProposalDetails(val proposalId: String, val currentUserId: String) : TradeUiEvent

    // Negociación: Aceptación con bloqueo ACID (CU010 / RN-01)
    data class AcceptProposal(val proposalId: String) : TradeUiEvent

    // Negociación: Contraoferta (CU011)
    data object OpenCounterOfferDialog : TradeUiEvent
    data object DismissCounterOfferDialog : TradeUiEvent
    data class ToggleCounterOfferItemSelection(val itemId: String) : TradeUiEvent
    data class OnCounterOfferTruequeCoinsChanged(val truequeCoinsStr: String) : TradeUiEvent
    data class OnCounterOfferCoinsChanged(val coinsStr: String) : TradeUiEvent
    data object SubmitCounterOffer : TradeUiEvent

    // Cancelación y rechazo (CU012 / RN-07)
    data class RejectProposal(val proposalId: String) : TradeUiEvent
    data class RequestCancelProposal(val proposalId: String) : TradeUiEvent
    data object DismissCancelDialog : TradeUiEvent
    data object ConfirmCancelProposal : TradeUiEvent

    // Utilidades
    data object DismissError : TradeUiEvent
}

/**
 * Efectos de un solo disparo emitidos hacia la UI (Navegación, Toasts).
 */
sealed interface TradeUiEffect : UiEffect {
    data class NavigateToNegotiation(val proposalId: String) : TradeUiEffect
    data class NavigateToChat(val proposalId: String) : TradeUiEffect
    data object NavigateBack : TradeUiEffect
    data class ShowToast(val message: String) : TradeUiEffect
}
