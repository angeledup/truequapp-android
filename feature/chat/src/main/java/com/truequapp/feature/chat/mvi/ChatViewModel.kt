package com.truequapp.feature.chat.mvi

import androidx.lifecycle.viewModelScope
import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.domain.model.TradeModality
import com.truequapp.core.domain.model.TradeProposal
import com.truequapp.core.domain.model.TradeStatus
import com.truequapp.core.domain.rules.BusinessRuleException
import com.truequapp.core.domain.rules.ChatAccessPermission
import com.truequapp.core.domain.rules.ValidateDirectChatAccessUseCase
import com.truequapp.core.network.websocket.SocketConnectionState
import com.truequapp.core.network.websocket.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel que gestiona la sala de chat privado transaccional en tiempo real vía WebSockets (CU018).
 * Hace cumplir con rigor la regla RN-03: Restricción de Contacto Directo y Ventana de Auditoría de 48 Horas.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val validateDirectChatAccessUseCase: ValidateDirectChatAccessUseCase = ValidateDirectChatAccessUseCase(),
    private val socketManager: SocketManager,
    private val userSessionDataStore: UserSessionDataStore,
    private val dispatchers: DispatcherProvider
) : BaseViewModel<ChatUiState, ChatUiEvent, ChatUiEffect>(ChatUiState()) {

    private val inMemoryProposals = mutableListOf<TradeProposal>()

    init {
        seedProposals()
        observeSocketConnection()
    }

    private fun seedProposals() {
        val proposals = listOf(
            TradeProposal(
                id = "prop_active_01",
                requesterUserId = "usr_current",
                receiverUserId = "usr_beta",
                offeredItemIds = listOf("item_user_01"),
                targetItemId = "item_002",
                compensatoryTruequeCoins = 100.0,
                modality = TradeModality.HIBRIDO_CON_CREDITOS,
                status = TradeStatus.PENDIENTE
            ),
            TradeProposal(
                id = "prop_reserved_02",
                requesterUserId = "usr_current",
                receiverUserId = "usr_alpha",
                offeredItemIds = listOf("item_user_02"),
                targetItemId = "item_001",
                compensatoryTruequeCoins = 2500.0,
                modality = TradeModality.HIBRIDO_CON_CREDITOS,
                status = TradeStatus.RESERVADO
            ),
            TradeProposal(
                id = "prop_closed_recent",
                requesterUserId = "usr_current",
                receiverUserId = "usr_beta",
                offeredItemIds = listOf("item_user_01"),
                targetItemId = "item_002",
                modality = TradeModality.DIRECTO_1_A_1,
                status = TradeStatus.COMPLETADO,
                createdAtEpochMs = System.currentTimeMillis() - (10 * 60 * 60 * 1000L) // Finalizado hace 10 horas (< 48h)
            ),
            TradeProposal(
                id = "prop_closed_old",
                requesterUserId = "usr_current",
                receiverUserId = "usr_beta",
                offeredItemIds = listOf("item_user_01"),
                targetItemId = "item_002",
                modality = TradeModality.DIRECTO_1_A_1,
                status = TradeStatus.COMPLETADO,
                createdAtEpochMs = System.currentTimeMillis() - (60 * 60 * 60 * 1000L) // Finalizado hace 60 horas (> 48h)
            )
        )
        inMemoryProposals.addAll(proposals)
    }

    private fun observeSocketConnection() {
        viewModelScope.launch(dispatchers.main) {
            socketManager.connectionState.collect { connState ->
                setState {
                    copy(isConnected = connState == SocketConnectionState.CONNECTED)
                }
            }
        }
    }

    override fun onEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.InitChat -> initChat(event.proposalId, event.currentUserId)
            is ChatUiEvent.OnMessageInputChanged -> handleMessageInputChanged(event.text)
            is ChatUiEvent.SendMessage -> handleSendMessage()
            is ChatUiEvent.SendTypingStatus -> handleSendTypingStatus(event.isTyping)
            is ChatUiEvent.ReceiveMessage -> handleReceiveMessage(event.message)
            is ChatUiEvent.OtherUserTypingChanged -> setState { copy(isOtherUserTyping = event.isTyping) }
            is ChatUiEvent.DismissError -> setState { copy(errorMessage = null) }
        }
    }

    private fun initChat(proposalId: String, currentUserId: String) {
        val proposal = inMemoryProposals.find { it.id == proposalId }

        try {
            // Regla RN-03: Validación canónica de acceso al chat
            val permission = validateDirectChatAccessUseCase(
                userId = currentUserId,
                activeProposal = proposal
            )

            val isReadOnly = permission == ChatAccessPermission.SOLO_LECTURA_HISTORIAL
            val otherUserId = if (proposal?.requesterUserId == currentUserId) proposal.receiverUserId else proposal?.requesterUserId.orEmpty()
            val otherUserName = if (otherUserId == "usr_alpha") "GamerAlpha" else "ColegaTruequero"

            val initialMessages = listOf(
                ChatMessageItem(
                    id = "msg_seed_01",
                    proposalId = proposalId,
                    senderId = otherUserId,
                    senderName = otherUserName,
                    content = "¡Hola! Vi tu oferta de trueque. ¿Te parece si coordinamos?",
                    timestampEpochMs = System.currentTimeMillis() - 120000,
                    isDelivered = true,
                    isRead = true,
                    isFromMe = false
                )
            )

            setState {
                copy(
                    proposalId = proposalId,
                    currentUserId = currentUserId,
                    otherUserId = otherUserId,
                    otherUserName = otherUserName,
                    permission = permission,
                    isReadOnly = isReadOnly,
                    isAccessDenied = false,
                    messages = initialMessages,
                    errorMessage = null
                )
            }

            // Unirse a sala de chat si no está denegado
            socketManager.joinRoom("trade_chat_$proposalId")
            listenToChatEvents(proposalId)

        } catch (e: BusinessRuleException.UnauthorizedChatAccessException) {
            setState {
                copy(
                    proposalId = proposalId,
                    currentUserId = currentUserId,
                    isAccessDenied = true,
                    errorMessage = e.message ?: "Acceso restringido por la regla RN-03."
                )
            }
            sendEffect(ChatUiEffect.ShowToast("RN-03: No tienes autorización para acceder a este chat."))
        }
    }

    private fun listenToChatEvents(proposalId: String) {
        viewModelScope.launch(dispatchers.main) {
            socketManager.listenToEvent("receive_message").collect { payload ->
                if (payload.isNotBlank()) {
                    val incoming = ChatMessageItem(
                        id = "msg_${System.currentTimeMillis()}",
                        proposalId = proposalId,
                        senderId = currentState.otherUserId,
                        senderName = currentState.otherUserName,
                        content = payload,
                        timestampEpochMs = System.currentTimeMillis(),
                        isDelivered = true,
                        isRead = true,
                        isFromMe = false
                    )
                    handleReceiveMessage(incoming)
                }
            }
        }

        viewModelScope.launch(dispatchers.main) {
            socketManager.listenToEvent("typing_indicator").collect { isTypingPayload ->
                val isTyping = isTypingPayload.toBooleanStrictOrNull() ?: false
                setState { copy(isOtherUserTyping = isTyping) }
            }
        }
    }

    private fun handleMessageInputChanged(text: String) {
        setState { copy(messageInput = text) }
        handleSendTypingStatus(text.isNotBlank())
    }

    private fun handleSendTypingStatus(isTyping: Boolean) {
        if (!currentState.isReadOnly && !currentState.isAccessDenied) {
            socketManager.emitEvent("typing_indicator", isTyping)
        }
    }

    private fun handleSendMessage() {
        val state = currentState
        if (state.isReadOnly) {
            setState { copy(errorMessage = "El trueque ha finalizado. La conversación está en modo solo lectura de auditoría (48h).") }
            return
        }
        if (state.isAccessDenied) {
            setState { copy(errorMessage = "No tienes permiso para enviar mensajes en este trueque (RN-03).") }
            return
        }

        val text = state.messageInput.trim()
        if (text.isBlank()) return

        val message = ChatMessageItem(
            id = "msg_${System.currentTimeMillis()}",
            proposalId = state.proposalId,
            senderId = state.currentUserId,
            senderName = "Yo",
            content = text,
            timestampEpochMs = System.currentTimeMillis(),
            isDelivered = true,
            isRead = false,
            isFromMe = true
        )

        setState {
            copy(
                messages = messages + message,
                messageInput = ""
            )
        }

        socketManager.emitEvent("send_message", text)
        sendEffect(ChatUiEffect.ScrollToBottom)
    }

    private fun handleReceiveMessage(message: ChatMessageItem) {
        setState {
            copy(messages = messages + message)
        }
        sendEffect(ChatUiEffect.ScrollToBottom)
        socketManager.emitEvent("message_read", message.id)
    }

    override fun onCleared() {
        super.onCleared()
        if (currentState.proposalId.isNotBlank()) {
            socketManager.leaveRoom("trade_chat_${currentState.proposalId}")
        }
    }

    // Métodos para facilitar pruebas unitarias
    fun addTestProposal(proposal: TradeProposal) {
        inMemoryProposals.add(proposal)
    }
}

