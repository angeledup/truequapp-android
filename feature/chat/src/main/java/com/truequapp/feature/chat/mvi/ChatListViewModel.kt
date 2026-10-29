package com.truequapp.feature.chat.mvi

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truequapp.core.database.dao.TradeProposalDao
import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.feature.chat.ui.ChatConversationSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatListUiState(
    val conversations: List<ChatConversationSummary> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val userSessionDataStore: UserSessionDataStore,
    private val tradeProposalDao: TradeProposalDao,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val session = userSessionDataStore.sessionFlow.first()
            val userId = session.userId ?: "usr_current"

            val proposals = tradeProposalDao.getAllProposalsSync()
            val userProposals = proposals.filter {
                it.initiatorUserId == userId || it.receiverUserId == userId ||
                it.initiatorUserId == "usr_current" || it.receiverUserId == "usr_current"
            }

            val list = if (userProposals.isNotEmpty()) {
                userProposals.map { p ->
                    val otherId = if (p.initiatorUserId == userId) p.receiverUserId else p.initiatorUserId
                    val otherUser = if (otherId.isNotBlank()) userDao.getUserByIdSync(otherId) else null
                    val name = otherUser?.name ?: if (otherId == "usr_alpha") "Carlos R. - Petrolera" else "Colega Truequero"
                    val (statusText, statusColor) = when (p.status) {
                        "CONTRAOFERTADA" -> "Contraofertada" to Color(0xFF1565C0)
                        "RESERVADO" -> "Reservado (RN-01)" to Color(0xFF2E7D32)
                        "EN_ENTREGA" -> "En Entrega" to Color(0xFF00695C)
                        else -> "Propuesta Activa" to Color(0xFFE65100)
                    }
                    ChatConversationSummary(
                        proposalId = p.id,
                        otherUserName = name,
                        lastMessagePreview = "Toca para abrir el chat transaccional seguro (RN-03)",
                        statusLabel = statusText,
                        statusColor = statusColor
                    )
                }
            } else {
                listOf(
                    ChatConversationSummary(
                        proposalId = "prop_active_01",
                        otherUserName = "Carlos R. - Petrolera",
                        lastMessagePreview = "¡Hola! Vi tu oferta de trueque. ¿Te parece si coordinamos?",
                        statusLabel = "Propuesta Activa",
                        statusColor = Color(0xFFE65100)
                    ),
                    ChatConversationSummary(
                        proposalId = "prop_reserved_02",
                        otherUserName = "Lucía M. - María de la Piedad",
                        lastMessagePreview = "Perfecto, nos vemos en Punto Seguro Plaza Forum.",
                        statusLabel = "Reservado (RN-01)",
                        statusColor = Color(0xFF2E7D32)
                    )
                )
            }

            _uiState.value = ChatListUiState(conversations = list, isLoading = false)
        }
    }
}

