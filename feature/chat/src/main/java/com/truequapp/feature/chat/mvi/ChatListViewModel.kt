package com.truequapp.feature.chat.mvi

import androidx.lifecycle.viewModelScope
import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.feature.chat.data.ChatConversationSummary
import com.truequapp.feature.chat.data.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel que gestiona la pantalla del nuevo menú "Mensajes" (Módulo 4).
 * Carga de forma reactiva todas las conversaciones vinculadas a trueques activos o en auditoría (RN-03).
 */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userSessionDataStore: UserSessionDataStore,
    private val dispatchers: DispatcherProvider
) : BaseViewModel<ChatListUiState, ChatListUiEvent, ChatListUiEffect>(ChatListUiState()) {

    init {
        loadConversations()
    }

    override fun onEvent(event: ChatListUiEvent) {
        when (event) {
            is ChatListUiEvent.Refresh -> loadConversations()
            is ChatListUiEvent.OnConversationSelected -> {
                sendEffect(ChatListUiEffect.NavigateToChat(event.proposalId))
            }
        }
    }

    private fun loadConversations() {
        viewModelScope.launch(dispatchers.io) {
            setState { copy(isLoading = true) }
            val session = userSessionDataStore.sessionFlow.first()
            val userEmail = session.userEmail ?: "angeleduvr@gmail.com"

            setState { copy(currentUserId = userEmail) }

            chatRepository.getConversations(userEmail).collectLatest { dbConversations ->
                val finalConversations = if (dbConversations.isNotEmpty()) {
                    dbConversations
                } else {
                    // Semilla inicial representativa si la base de datos de trueques aún no tiene registros
                    listOf(
                        ChatConversationSummary(
                            proposalId = "prop_active_01",
                            otherUserEmail = "gamer_alpha@truequapp.com",
                            otherUserName = "GamerAlpha",
                            tradeStatus = "PENDIENTE",
                            isTradeActive = true,
                            targetItemSummary = "Nintendo Switch OLED",
                            lastMessage = "¡Hola! Vi tu oferta de trueque. ¿Te parece si coordinamos?",
                            lastMessageTimeMs = System.currentTimeMillis() - 120000,
                            unreadCount = 1
                        ),
                        ChatConversationSummary(
                            proposalId = "prop_reserved_02",
                            otherUserEmail = "colega_truequero@truequapp.com",
                            otherUserName = "ColegaTruequero",
                            tradeStatus = "RESERVADO",
                            isTradeActive = true,
                            targetItemSummary = "Audífonos Sony WH-1000XM4",
                            lastMessage = "De acuerdo con el punto de entrega en Plaza Cristal.",
                            lastMessageTimeMs = System.currentTimeMillis() - 3600000,
                            unreadCount = 0
                        )
                    )
                }

                setState {
                    copy(
                        conversations = finalConversations,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }
}

