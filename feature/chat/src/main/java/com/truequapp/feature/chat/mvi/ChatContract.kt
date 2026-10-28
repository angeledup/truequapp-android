package com.truequapp.feature.chat.mvi

import com.truequapp.core.common.mvi.UiEffect
import com.truequapp.core.common.mvi.UiEvent
import com.truequapp.core.common.mvi.UiState
import com.truequapp.core.domain.rules.ChatAccessPermission

/**
 * Representa un mensaje individual dentro de la sala de negociación del trueque.
 */
data class ChatMessageItem(
    val id: String,
    val proposalId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val isDelivered: Boolean = true,
    val isRead: Boolean = false,
    val isFromMe: Boolean = false
)

/**
 * Estado inmutable de la pantalla de chat privado transaccional (CU018).
 */
data class ChatUiState(
    val proposalId: String = "",
    val currentUserId: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "Contraparte del Trueque",
    val messages: List<ChatMessageItem> = emptyList(),
    val messageInput: String = "",
    val permission: ChatAccessPermission? = null,
    val isReadOnly: Boolean = false, // Modo solo lectura después de finalizar (48h)
    val isAccessDenied: Boolean = false, // Infracción RN-03
    val isOtherUserTyping: Boolean = false,
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : UiState

/**
 * Eventos disparados por el usuario o por la recepción de eventos de WebSockets.
 */
sealed interface ChatUiEvent : UiEvent {
    data class InitChat(val proposalId: String, val currentUserId: String) : ChatUiEvent
    data class OnMessageInputChanged(val text: String) : ChatUiEvent
    data object SendMessage : ChatUiEvent
    data class SendTypingStatus(val isTyping: Boolean) : ChatUiEvent
    data class ReceiveMessage(val message: ChatMessageItem) : ChatUiEvent
    data class OtherUserTypingChanged(val isTyping: Boolean) : ChatUiEvent
    data object DismissError : ChatUiEvent
}

/**
 * Efectos secundarios de la interfaz de chat.
 */
sealed interface ChatUiEffect : UiEffect {
    data object NavigateBack : ChatUiEffect
    data class ShowToast(val message: String) : ChatUiEffect
    data object ScrollToBottom : ChatUiEffect
}

