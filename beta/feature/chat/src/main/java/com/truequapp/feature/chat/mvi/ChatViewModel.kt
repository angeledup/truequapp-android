package com.truequapp.feature.chat.mvi

import androidx.lifecycle.viewModelScope
import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.database.entity.TruequeConOfertas
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.domain.model.TradeModality
import com.truequapp.core.domain.model.TradeProposal
import com.truequapp.core.domain.model.TradeStatus
import com.truequapp.core.domain.rules.BusinessRuleException
import com.truequapp.core.domain.rules.ChatAccessPermission
import com.truequapp.core.domain.rules.ValidateDirectChatAccessUseCase
import com.truequapp.core.network.websocket.SocketConnectionState
import com.truequapp.core.network.websocket.SocketManager
import com.truequapp.core.database.storage.UserPublicationStorage
import com.truequapp.feature.chat.data.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
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
    private val dispatchers: DispatcherProvider,
    private val chatRepository: ChatRepository? = null,
    private val userPublicationStorage: UserPublicationStorage? = null
) : BaseViewModel<ChatUiState, ChatUiEvent, ChatUiEffect>(ChatUiState()) {

    private val inMemoryProposals = mutableListOf<TradeProposal>()

    init {
        observeSocketConnection()
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
        viewModelScope.launch(dispatchers.io) {
            val sessionEmail = userSessionDataStore.sessionFlow.firstOrNull()?.userEmail
            val resolvedUserId = if (currentUserId.isNotBlank()) currentUserId else (sessionEmail ?: "angeleduvr@gmail.com")

            val dbProposal = chatRepository?.getProposalById(proposalId)
            val proposal = if (dbProposal != null) {
                mapToTradeProposal(dbProposal)
            } else {
                inMemoryProposals.find { it.id == proposalId }
            }

            try {
                // Regla RN-03: Validación canónica de acceso al chat
                val permission = validateDirectChatAccessUseCase(
                    userId = resolvedUserId,
                    activeProposal = proposal
                )

                val isReadOnly = permission == ChatAccessPermission.SOLO_LECTURA_HISTORIAL
                val otherUserId = if (proposal?.requesterUserId == resolvedUserId) proposal.receiverUserId else proposal?.requesterUserId.orEmpty()
                val otherUserName = if (otherUserId == "usr_alpha") "GamerAlpha" else otherUserId.substringBefore("@").ifBlank { "ColegaTruequero" }

                val allItems = userPublicationStorage?.loadPublishedItems().orEmpty()
                val itemMap = allItems.associateBy { it.id }

                val offeredIds = proposal?.offeredItemIds.orEmpty().ifEmpty {
                    dbProposal?.ofertaA?.articulosIds?.split(",")?.filter { it.isNotBlank() }.orEmpty()
                }
                val targetId = proposal?.targetItemId ?: dbProposal?.ofertaB?.articulosIds.orEmpty()

                val photosA = offeredIds.flatMap { id -> itemMap[id]?.imagesWebP.orEmpty() }
                val photosB = itemMap[targetId]?.imagesWebP.orEmpty()

                setState {
                    copy(
                        proposalId = proposalId,
                        currentUserId = resolvedUserId,
                        otherUserId = otherUserId,
                        otherUserName = otherUserName,
                        permission = permission,
                        isReadOnly = isReadOnly,
                        isAccessDenied = false,
                        errorMessage = null,
                        offerAPhotoUrls = photosA,
                        offerBPhotoUrls = photosB
                    )
                }

                if (chatRepository != null) {
                    launch(dispatchers.main) {
                        chatRepository.getMessagesForProposal(proposalId, resolvedUserId).collectLatest { dbMsgs ->
                            if (dbMsgs.isNotEmpty()) {
                                setState { copy(messages = dbMsgs) }
                            }
                        }
                    }
                    launch(dispatchers.io) {
                        chatRepository.markAsRead(proposalId, resolvedUserId)
                    }
                    launch(dispatchers.io) {
                        while (isActive) {
                            try {
                                chatRepository.syncMessagesFromCloud(proposalId)
                            } catch (_: Exception) {}
                            delay(3000)
                        }
                    }
                } else {
                    setState { copy(messages = emptyList()) }
                }

                // Unirse a sala de chat si no está denegado
                socketManager.joinRoom("trade_chat_$proposalId")
                listenToChatEvents(proposalId)

            } catch (e: BusinessRuleException.UnauthorizedChatAccessException) {
                setState {
                    copy(
                        proposalId = proposalId,
                        currentUserId = resolvedUserId,
                        isAccessDenied = true,
                        errorMessage = e.message ?: "Acceso restringido por la regla RN-03."
                    )
                }
                sendEffect(ChatUiEffect.ShowToast("RN-03: No tienes autorización para acceder a este chat."))
            }
        }
    }

    private fun mapToTradeProposal(t: TruequeConOfertas): TradeProposal {
        val ofA = t.ofertaA
        val ofB = t.ofertaB
        val offeredList = ofA?.articulosIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        val target = ofB?.articulosIds?.split(",")?.firstOrNull() ?: ""
        val coins = ofA?.truequeCoins ?: 0.0
        val statusEnum = try {
            TradeStatus.valueOf(t.trueque.estado)
        } catch (e: Exception) {
            TradeStatus.PENDIENTE
        }
        return TradeProposal(
            id = t.trueque.id,
            requesterUserId = t.trueque.usuarioACorreo,
            receiverUserId = t.trueque.usuarioBCorreo,
            offeredItemIds = offeredList,
            targetItemId = target,
            compensatoryTruequeCoins = coins,
            modality = TradeModality.DIRECTO_1_A_1,
            status = statusEnum,
            createdAtEpochMs = t.trueque.fechaCreacion
        )
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

        viewModelScope.launch(dispatchers.io) {
            chatRepository?.sendMessage(
                proposalId = state.proposalId,
                senderEmail = state.currentUserId,
                receiverEmail = state.otherUserId,
                content = text
            )
        }

        socketManager.emitEvent("send_message", text)
        sendEffect(ChatUiEffect.ScrollToBottom)
    }

    private fun handleReceiveMessage(message: ChatMessageItem) {
        setState {
            copy(messages = messages + message)
        }
        viewModelScope.launch(dispatchers.io) {
            chatRepository?.saveIncomingMessage(
                proposalId = message.proposalId,
                senderEmail = message.senderId,
                receiverEmail = currentState.currentUserId,
                content = message.content
            )
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
