package com.truequapp.feature.chat

import com.truequapp.core.database.dao.ChatMessageDao
import com.truequapp.core.database.dao.TruequeDao
import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.entity.ChatMessageEntity
import com.truequapp.core.database.entity.OfertaItemEntity
import com.truequapp.core.database.entity.TruequeConOfertas
import com.truequapp.core.database.entity.TruequeEntity
import com.truequapp.core.database.entity.UserEntity
import com.truequapp.core.network.websocket.SocketManager
import com.truequapp.feature.chat.data.ChatRepository
import com.truequapp.feature.chat.mvi.ChatListUiEffect
import com.truequapp.feature.chat.mvi.ChatListUiEvent
import com.truequapp.feature.chat.mvi.ChatListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FakeChatMessageDaoForTest : ChatMessageDao {
    val messages = mutableListOf<ChatMessageEntity>()

    override suspend fun insertMessage(message: ChatMessageEntity) {
        messages.add(message)
    }

    override suspend fun insertMessages(messages: List<ChatMessageEntity>) {
        this.messages.addAll(messages)
    }

    override fun getMessagesForTrueque(truequeId: String): Flow<List<ChatMessageEntity>> {
        return flowOf(messages.filter { it.truequeId == truequeId })
    }

    override suspend fun getLatestMessageForTrueque(truequeId: String): ChatMessageEntity? {
        return messages.filter { it.truequeId == truequeId }.maxByOrNull { it.fechaEnvioMs }
    }

    override suspend fun markMessagesAsRead(truequeId: String, userEmail: String) {
        // no-op
    }

    override fun countUnreadMessages(truequeId: String, userEmail: String): Flow<Int> {
        return flowOf(messages.count { it.truequeId == truequeId && it.receptorCorreo == userEmail && !it.estaLeido })
    }
}

class FakeTruequeDaoForChatTest : TruequeDao {
    val trueques = mutableListOf<TruequeConOfertas>()

    override suspend fun insertOfertaItem(oferta: OfertaItemEntity) {}
    override suspend fun insertTrueque(trueque: TruequeEntity) {}
    override suspend fun updateTrueque(trueque: TruequeEntity) {}
    override suspend fun updateEstado(truequeId: String, nuevoEstado: String, estaActiva: Boolean, timestamp: Long) {}
    override suspend fun getTruequeById(truequeId: String): TruequeConOfertas? = trueques.find { it.trueque.id == truequeId }

    override fun getTruequesActivos(): Flow<List<TruequeConOfertas>> {
        return flowOf(trueques.filter { it.trueque.estaActiva })
    }

    override fun getTruequesActivosPorUsuario(correo: String): Flow<List<TruequeConOfertas>> {
        return flowOf(trueques.filter { (it.trueque.usuarioACorreo == correo || it.trueque.usuarioBCorreo == correo) && it.trueque.estaActiva })
    }

    override fun getHistorialTruequesPorUsuario(correo: String): Flow<List<TruequeConOfertas>> {
        return flowOf(trueques.filter { it.trueque.usuarioACorreo == correo || it.trueque.usuarioBCorreo == correo })
    }

    override fun getOfertasRecibidas(correo: String): Flow<List<TruequeConOfertas>> {
        return flowOf(trueques.filter { it.trueque.usuarioBCorreo == correo && it.trueque.estaActiva })
    }

    override fun getOfertasEnviadas(correo: String): Flow<List<TruequeConOfertas>> {
        return flowOf(trueques.filter { it.trueque.usuarioACorreo == correo && it.trueque.estaActiva })
    }

    override suspend fun deleteTruequeById(truequeId: String) {
        trueques.removeAll { it.trueque.id == truequeId }
    }
}

class FakeUserDaoForChatTest : UserDao {
    override suspend fun getUserByEmail(correo: String): UserEntity? = null
    override fun observeUserByEmail(correo: String): Flow<UserEntity?> = flowOf(null)
    override suspend fun countByEmail(correo: String): Int = 0
    override suspend fun insertUser(user: UserEntity) {}
    override fun getAllUsers(): Flow<List<UserEntity>> = flowOf(emptyList())
    override suspend fun getCoinsByEmail(correo: String): Double? = 100.0
    override fun observeCoinsByEmail(correo: String): Flow<Double?> = flowOf(100.0)
    override suspend fun reduceCoins(correo: String, amount: Double): Int = 1
    override suspend fun addCoins(correo: String, amount: Double): Int = 1
    override suspend fun updateCoins(correo: String, newCoins: Double): Int = 1
    override suspend fun getCompletedTradesByEmail(correo: String): Int? = 0
    override fun observeCompletedTradesByEmail(correo: String): Flow<Int?> = flowOf(0)
    override suspend fun incrementCompletedTrades(correo: String): Int = 1
    override suspend fun updateCompletedTrades(correo: String, count: Int): Int = 1
    override suspend fun getRatingByEmail(correo: String): Double? = 5.0
    override fun observeRatingByEmail(correo: String): Flow<Double?> = flowOf(5.0)
    override suspend fun updateRating(correo: String, newRating: Double): Int = 1
    override fun getUserById(id: String): Flow<UserEntity?> = flowOf(null)
    override suspend fun getUserByIdSync(id: String): UserEntity? = null
    override suspend fun getUserByEmailSync(email: String): UserEntity? = null
    override suspend fun getUserByIdentifierSync(identifier: String): UserEntity? = null
    override suspend fun authenticate(email: String, passwordHash: String): UserEntity? = null
    override suspend fun updateUser(user: UserEntity) {}
    override suspend fun updateTruequeCoins(userId: String, availableCoins: Double, escrowCoins: Double) {}
    override suspend fun getTruequeCoins(userId: String): Double? = 100.0
    override suspend fun deleteUser(userId: String) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = TestChatDispatcherProvider(testDispatcher)
    private val dataStore = FakeChatDataStore()
    private val socketManager = SocketManager()

    private val chatMessageDao = FakeChatMessageDaoForTest()
    private val truequeDao = FakeTruequeDaoForChatTest()
    private val userDao = FakeUserDaoForChatTest()

    private lateinit var chatRepository: ChatRepository
    private lateinit var viewModel: ChatListViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        chatRepository = ChatRepository(
            chatMessageDao = chatMessageDao,
            truequeDao = truequeDao,
            userDao = userDao,
            socketManager = socketManager
        )

        // Sembrar un trueque real para current@truequapp.com
        val trueque = TruequeEntity(
            id = "trueque_real_01",
            usuarioACorreo = "current@truequapp.com",
            ofertaAId = "of_a",
            usuarioBCorreo = "otro_usuario@truequapp.com",
            ofertaBId = "of_b",
            estado = "RESERVADO",
            estaActiva = true
        )
        val ofB = OfertaItemEntity(
            id = "of_b",
            usuarioCorreo = "otro_usuario@truequapp.com",
            articulosIds = "Bicicleta de montaña",
            truequeCoins = 0.0
        )
        truequeDao.trueques.add(
            TruequeConOfertas(
                trueque = trueque,
                ofertaA = null,
                ofertaB = ofB
            )
        )

        viewModel = ChatListViewModel(
            chatRepository = chatRepository,
            userSessionDataStore = dataStore,
            dispatchers = dispatchers
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("Carga y presenta la lista de conversaciones vinculadas a trueques del usuario activo")
    fun loadConversations_populatesActiveConversations() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("current@truequapp.com", state.currentUserId)
        assertEquals(1, state.conversations.size)

        val conv = state.conversations.first()
        assertEquals("trueque_real_01", conv.proposalId)
        assertEquals("otro_usuario@truequapp.com", conv.otherUserEmail)
        assertEquals("RESERVADO", conv.tradeStatus)
        assertTrue(conv.isTradeActive)
    }

    @Test
    @DisplayName("Seleccionar una conversación emite el efecto de navegación hacia la sala de chat correspondiente")
    fun onConversationSelected_emitsNavigateToChatEffect() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(ChatListUiEvent.OnConversationSelected("trueque_real_01"))
        advanceUntilIdle()

        val effect = viewModel.uiEffect.first()
        assertTrue(effect is ChatListUiEffect.NavigateToChat)
        assertEquals("trueque_real_01", (effect as ChatListUiEffect.NavigateToChat).proposalId)
    }
}

