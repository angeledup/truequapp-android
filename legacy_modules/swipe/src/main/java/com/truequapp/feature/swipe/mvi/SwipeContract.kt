package com.truequapp.feature.swipe.mvi

import com.truequapp.core.common.mvi.UiEffect
import com.truequapp.core.common.mvi.UiEvent
import com.truequapp.core.common.mvi.UiState
import com.truequapp.core.domain.model.TradeItem

/**
 * Estado inmutable de la pantalla de Modo Swipe (CU017).
 */
data class SwipeUiState(
    val currentUserId: String = "",
    val itemsDeck: List<TradeItem> = emptyList(),
    val currentIndex: Int = 0,
    val matchedItem: TradeItem? = null,
    val showMatchDialog: Boolean = false,
    val likedItemIds: Set<String> = emptySet(),
    val passedItemIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : UiState {
    val currentTopItem: TradeItem?
        get() = itemsDeck.getOrNull(currentIndex)

    val hasRemainingItems: Boolean
        get() = currentIndex < itemsDeck.size
}

/**
 * Eventos e intenciones del usuario en el Modo Swipe.
 */
sealed interface SwipeUiEvent : UiEvent {
    data class LoadFeed(val currentUserId: String) : SwipeUiEvent
    data class SwipeRight(val item: TradeItem) : SwipeUiEvent // "Me interesa"
    data class SwipeLeft(val item: TradeItem) : SwipeUiEvent  // "Pasar"
    data object DismissMatchDialog : SwipeUiEvent
    data class ProposeTradeFromMatch(val targetItemId: String) : SwipeUiEvent
    data object DismissError : SwipeUiEvent
}

/**
 * Efectos secundarios de navegación y feedback.
 */
sealed interface SwipeUiEffect : UiEffect {
    data class NavigateToProposal(val targetItemId: String) : SwipeUiEffect
    data class ShowToast(val message: String) : SwipeUiEffect
    data object TriggerMatchCelebration : SwipeUiEffect
}

