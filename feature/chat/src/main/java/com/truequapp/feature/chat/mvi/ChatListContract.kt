package com.truequapp.feature.chat.mvi

import com.truequapp.core.common.mvi.UiEffect
import com.truequapp.core.common.mvi.UiEvent
import com.truequapp.core.common.mvi.UiState
import com.truequapp.feature.chat.data.ChatConversationSummary

/**
 * Estado inmutable para la pantalla principal del listado de conversaciones de trueque (Módulo 4).
 */
data class ChatListUiState(
    val conversations: List<ChatConversationSummary> = emptyList(),
    val currentUserId: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : UiState

sealed interface ChatListUiEvent : UiEvent {
    data object Refresh : ChatListUiEvent
    data class OnConversationSelected(val proposalId: String) : ChatListUiEvent
}

sealed interface ChatListUiEffect : UiEffect {
    data class NavigateToChat(val proposalId: String) : ChatListUiEffect
    data class ShowToast(val message: String) : ChatListUiEffect
}

