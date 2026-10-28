package com.truequapp.feature.chat

import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.datastore.model.UserSession
import com.truequapp.core.domain.model.TradeModality
import com.truequapp.core.domain.model.TradeProposal
import com.truequapp.core.domain.model.TradeStatus
import com.truequapp.core.domain.rules.ChatAccessPermission
import com.truequapp.core.domain.rules.ValidateDirectChatAccessUseCase
import com.truequapp.core.network.websocket.SocketManager
import com.truequapp.feature.chat.mvi.ChatMessageItem
import com.truequapp.feature.chat.mvi.ChatUiEvent
import com.truequapp.feature.chat.mvi.ChatViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FakeChatDataStore : UserSessionDataStore {
    private val _session = MutableStateFlow(
        UserSession(userId = "usr_current", userEmail = "current@truequapp.com", isKycVerified = true)
    )
    override val sessionFlow: Flow<UserSession> = _session
    override suspend fun saveTokens(accessToken: String, refreshToken: String) {}
    override suspend fun saveUser(userId: String, email: String, isKycVerified: Boolean) {}
    override suspend fun getAccessToken(): String? = null
    override suspend fun getRefreshToken(): String? = null
    override suspend fun clearSession() {}
}

class TestChatDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher get() = dispatcher
    override val io: CoroutineDispatcher get() = dispatcher
    override val default: CoroutineDispatcher get() = dispatcher
    override val unconfined: CoroutineDispatcher get() = dispatcher
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = TestChatDispatcherProvider(testDispatcher)
    private val dataStore = FakeChatDataStore()
    private val socketManager = SocketManager()

    private lateinit var viewModel: ChatViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ChatViewModel(
            validateDirectChatAccessUseCase = ValidateDirectChatAccessUseCase(),
            socketManager = socketManager,
            userSessionDataStore = dataStore,
            dispatchers = dispatchers
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("RN-03: Deniega acceso al chat si no existe propuesta vinculada o el usuario no participa")
    fun chatAccess_deniedForNonParticipants() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Intentar abrir un chat para una propuesta inexistente
        viewModel.onEvent(ChatUiEvent.InitChat(proposalId = "prop_non_existent", currentUserId = "usr_intruder"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAccessDenied, "El acceso debe denegarse según RN-03")
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("RN-03"))
    }

    @Test
    @DisplayName("RN-03: Concede permiso completo de lectura y escritura para propuestas activas")
    fun chatAccess_allowedForActiveProposal() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(ChatUiEvent.InitChat(proposalId = "prop_active_01", currentUserId = "usr_current"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAccessDenied)
        assertFalse(state.isReadOnly)
        assertEquals(ChatAccessPermission.PERMITIDO_ESCRITURA_Y_LECTURA, state.permission)
        assertTrue(state.messages.isNotEmpty())
    }

    @Test
    @DisplayName("RN-03: Activa modo solo lectura para propuestas finalizadas dentro de las 48 horas de auditoría")
    fun chatAccess_readOnlyForClosedProposalWithin48Hours() = runTest(testDispatcher) {
        advanceUntilIdle()

        // prop_closed_recent fue finalizada hace 10 horas (< 48h)
        viewModel.onEvent(ChatUiEvent.InitChat(proposalId = "prop_closed_recent", currentUserId = "usr_current"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAccessDenied)
        assertTrue(state.isReadOnly, "Debe estar en modo solo lectura de auditoría")
        assertEquals(ChatAccessPermission.SOLO_LECTURA_HISTORIAL, state.permission)

        // Intentar enviar mensaje en modo solo lectura
        val initialMessageCount = state.messages.size
        viewModel.onEvent(ChatUiEvent.OnMessageInputChanged("Intentando escribir"))
        viewModel.onEvent(ChatUiEvent.SendMessage)
        advanceUntilIdle()

        val newState = viewModel.uiState.value
        assertEquals(initialMessageCount, newState.messages.size, "No debe permitir enviar mensajes en modo solo lectura")
        assertNotNull(newState.errorMessage)
    }

    @Test
    @DisplayName("RN-03: Bloquea acceso para propuestas finalizadas con más de 48 horas")
    fun chatAccess_deniedAfter48Hours() = runTest(testDispatcher) {
        advanceUntilIdle()

        // prop_closed_old fue finalizada hace 60 horas (> 48h)
        viewModel.onEvent(ChatUiEvent.InitChat(proposalId = "prop_closed_old", currentUserId = "usr_current"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ChatAccessPermission.DENEGADO, state.permission)
    }

    @Test
    @DisplayName("CU018: Enviar mensaje añade el mensaje localmente con estado de entrega y limpia el input")
    fun sendMessage_appendsMessageAndClearsInput() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(ChatUiEvent.InitChat(proposalId = "prop_active_01", currentUserId = "usr_current"))
        advanceUntilIdle()

        val initialCount = viewModel.uiState.value.messages.size
        viewModel.onEvent(ChatUiEvent.OnMessageInputChanged("¿Podemos vernos a las 5 PM en el Punto Seguro?"))
        viewModel.onEvent(ChatUiEvent.SendMessage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(initialCount + 1, state.messages.size)
        assertEquals("", state.messageInput)

        val lastMessage = state.messages.last()
        assertEquals("¿Podemos vernos a las 5 PM en el Punto Seguro?", lastMessage.content)
        assertTrue(lastMessage.isFromMe)
        assertTrue(lastMessage.isDelivered)
    }

    @Test
    @DisplayName("CU018: Indicador de escritura actualiza el estado isOtherUserTyping")
    fun typingStatus_updatesState() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(ChatUiEvent.InitChat(proposalId = "prop_active_01", currentUserId = "usr_current"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isOtherUserTyping)

        viewModel.onEvent(ChatUiEvent.OtherUserTypingChanged(true))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isOtherUserTyping)

        viewModel.onEvent(ChatUiEvent.OtherUserTypingChanged(false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isOtherUserTyping)
    }
}

