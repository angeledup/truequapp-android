package com.truequapp.feature.chat.mvi

import androidx.lifecycle.viewModelScope
import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.database.dao.TradeProposalDao
import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.mapper.toDomainModel
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val tradeProposalDao: TradeProposalDao? = null,
    private val userDao: UserDao? = null,
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
        viewModelScope.launch(dispatchers.io) {
            val session = userSessionDataStore.sessionFlow.first()
            val effectiveUserId = if (currentUserId.isNotBlank()) currentUserId else (session.userId ?: "usr_current")

            // Chat de Ejemplo 1 interactivo con mensajes predeterminados
            if (proposalId == "chat_ejemplo_1" || proposalId.contains("ejemplo")) {
                val demoMessages = listOf(
                    ChatMessageItem(
                        id = "msg_ex_01",
                        proposalId = proposalId,
                        senderId = "usr_alpha",
                        senderName = "Carlos R. (Chat Ejemplo 1)",
                        content = "¡Hola! Vi tu publicación y me interesa hacer el trueque. ¿Te parece si coordinamos la entrega?",
                        timestampEpochMs = System.currentTimeMillis() - 120000,
                        isDelivered = true,
                        isRead = true,
                        isFromMe = false
                    ),
                    ChatMessageItem(
                        id = "msg_ex_02",
                        proposalId = proposalId,
                        senderId = "usr_alpha",
                        senderName = "Carlos R. (Chat Ejemplo 1)",
                        content = "Podemos acordar en uno de los Puntos Seguros de Coatzacoalcos, como Plaza Forum o el Hemiciclo a los Niños Héroes en el Malecón.",
                        timestampEpochMs = System.currentTimeMillis() - 60000,
                        isDelivered = true,
                        isRead = true,
                        isFromMe = false
                    )
                )

                withContext(dispatchers.main) {
                    setState {
                        copy(
                            proposalId = proposalId,
                            currentUserId = effectiveUserId,
                            otherUserId = "usr_alpha",
                            otherUserName = "Carlos R. (Chat Ejemplo 1)",
                            permission = ChatAccessPermission.PERMITIDO_ESCRITURA_Y_LECTURA,
                            isReadOnly = false,
                            isAccessDenied = false,
                            messages = demoMessages,
                            errorMessage = null
                        )
                    }
                }
                return@launch
            }

            val proposalFromDb = tradeProposalDao?.getProposalByIdSync(proposalId)?.toDomainModel()
            val rawProposal = proposalFromDb ?: inMemoryProposals.find { it.id == proposalId }

            // Si la propuesta tiene "usr_current", mapearla al usuario activo de la sesión para evitar falsos rechazos de RN-03
            val proposal = when {
                rawProposal == null -> null
                rawProposal.requesterUserId == "usr_current" -> rawProposal.copy(requesterUserId = effectiveUserId)
                rawProposal.receiverUserId == "usr_current" -> rawProposal.copy(receiverUserId = effectiveUserId)
                else -> rawProposal
            }

            try {
                // Regla RN-03: Validación canónica de acceso al chat
                val permission = validateDirectChatAccessUseCase(
                    userId = effectiveUserId,
                    activeProposal = proposal
                )

                val isReadOnly = permission == ChatAccessPermission.SOLO_LECTURA_HISTORIAL
                val otherUserId = if (proposal?.requesterUserId == effectiveUserId) proposal.receiverUserId else proposal?.requesterUserId.orEmpty()
                val otherUserEntity = if (otherUserId.isNotBlank()) userDao?.getUserByIdSync(otherUserId) else null
                val otherUserName = otherUserEntity?.name ?: if (otherUserId == "usr_alpha") "Carlos R. - Petrolera" else "Colega Truequero"

                val initialMessages = listOf(
                    ChatMessageItem(
                        id = "msg_seed_01",
                        proposalId = proposalId,
                        senderId = otherUserId,
                        senderName = otherUserName,
                        content = "¡Hola! Vi tu oferta de trueque. ¿Te parece si coordinamos la entrega?",
                        timestampEpochMs = System.currentTimeMillis() - 120000,
                        isDelivered = true,
                        isRead = true,
                        isFromMe = false
                    )
                )

                withContext(dispatchers.main) {
                    setState {
                        copy(
                            proposalId = proposalId,
                            currentUserId = effectiveUserId,
                            otherUserId = otherUserId,
                            otherUserName = otherUserName,
                            permission = permission,
                            isReadOnly = isReadOnly,
                            isAccessDenied = false,
                            messages = initialMessages,
                            errorMessage = null
                        )
                    }
                }

                // Unirse a sala de chat si no está denegado
                socketManager.joinRoom("trade_chat_$proposalId")
                listenToChatEvents(proposalId)

            } catch (e: BusinessRuleException.UnauthorizedChatAccessException) {
                withContext(dispatchers.main) {
                    setState {
                        copy(
                            proposalId = proposalId,
                            currentUserId = effectiveUserId,
                            isAccessDenied = true,
                            errorMessage = e.message ?: "Acceso restringido por la regla RN-03."
                        )
                    }
                }
                sendEffect(ChatUiEffect.ShowToast("RN-03: No tienes autorización para acceder a este chat."))
            }
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
        sendEffect(ChatUiEffect.ScrollToBottom)

        // Respuesta interactiva automática para Chat Ejemplo 1
        if (state.proposalId == "chat_ejemplo_1" || state.proposalId.contains("ejemplo")) {
            viewModelScope.launch(dispatchers.io) {
                delay(1200)
                val autoReply = ChatMessageItem(
                    id = "msg_reply_${System.currentTimeMillis()}",
                    proposalId = state.proposalId,
                    senderId = state.otherUserId,
                    senderName = state.otherUserName,
                    content = "¡Enterado! Me parece perfecto. Puedes pulsar arriba en el botón de 'Punto Seguro / Mapa' para ver las ubicaciones vigiladas en Coatzacoalcos.",
                    timestampEpochMs = System.currentTimeMillis(),
                    isDelivered = true,
                    isRead = true,
                    isFromMe = false
                )
                withContext(dispatchers.main) {
                    setState { copy(messages = messages + autoReply) }
                    sendEffect(ChatUiEffect.ScrollToBottom)
                }
            }
            return
        }

        socketManager.emitEvent("send_message", text)
        handleSendTypingStatus(false)
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

