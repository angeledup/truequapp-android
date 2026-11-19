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

            // Sincronizar en segundo plano propuestas y mensajes desde la nube (Turso)
            launch(dispatchers.io) {
                try {
                    chatRepository.syncConversationsFromCloud(userEmail)
                } catch (_: Exception) {}
            }

            chatRepository.getConversations(userEmail).collectLatest { dbConversations ->
                setState {
                    copy(
                        conversations = dbConversations,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }
}

