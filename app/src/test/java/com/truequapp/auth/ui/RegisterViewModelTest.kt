package com.truequapp.auth.ui

import com.truequapp.auth.data.AuthRepository
import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.entity.UserEntity
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.datastore.model.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeRegisterUserDao
    private lateinit var fakeSession: FakeRegisterSessionDataStore
    private lateinit var repository: AuthRepository
    private lateinit var viewModel: RegisterViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeRegisterUserDao()
        fakeSession = FakeRegisterSessionDataStore()
        repository = AuthRepository(fakeDao, fakeSession)
        viewModel = RegisterViewModel(repository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful registration persists 4 inputs and initializes 7 columns in database`() = runTest {
        viewModel.onEvent(RegisterUiEvent.UsuarioChanged("carlos_trueques"))
        viewModel.onEvent(RegisterUiEvent.NumeroTelefonicoChanged("+525588776655"))
        viewModel.onEvent(RegisterUiEvent.CorreoChanged("carlos@truequapp.com"))
        viewModel.onEvent(RegisterUiEvent.ContrasenaChanged("miClave1234"))

        viewModel.onEvent(RegisterUiEvent.SubmitRegister)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isRegisteredSuccessfully)
        assertNull(state.errorMessage)

        // Verificar persistencia en base de datos Room
        val savedUser = fakeDao.getUserByEmail("carlos@truequapp.com")
        assertNotNull(savedUser)
        assertEquals("carlos@truequapp.com", savedUser?.correo)
        assertEquals("miClave1234", savedUser?.contrasena)
        assertEquals("carlos_trueques", savedUser?.usuario)
        assertEquals("+525588776655", savedUser?.numeroTelefonico)
        assertEquals(100.0, savedUser?.cantidadTruequeCoins)
        assertEquals(0, savedUser?.truequesRealizados)
        assertEquals(5.0, savedUser?.calificacion)
    }

    @Test
    fun `shows error when email is already registered in database`() = runTest {
        fakeDao.insertUser(
            UserEntity(
                correo = "existente@correo.com",
                contrasena = "clave1",
                usuario = "usuario_existente",
                numeroTelefonico = "12345678"
            )
        )

        viewModel.onEvent(RegisterUiEvent.UsuarioChanged("nuevo_usuario"))
        viewModel.onEvent(RegisterUiEvent.NumeroTelefonicoChanged("98765432"))
        viewModel.onEvent(RegisterUiEvent.CorreoChanged("existente@correo.com"))
        viewModel.onEvent(RegisterUiEvent.ContrasenaChanged("otraClave"))

        viewModel.onEvent(RegisterUiEvent.SubmitRegister)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRegisteredSuccessfully)
        assertEquals("Ya existe una cuenta registrada con este correo", state.errorMessage)
    }

    @Test
    fun `validates empty fields and shows specific message`() = runTest {
        viewModel.onEvent(RegisterUiEvent.UsuarioChanged(""))
        viewModel.onEvent(RegisterUiEvent.SubmitRegister)
        advanceUntilIdle()

        assertEquals("Ingresa un nombre de usuario", viewModel.uiState.value.errorMessage)
    }
}

private class FakeRegisterUserDao : UserDao {
    private val users = mutableMapOf<String, UserEntity>()

    override suspend fun getUserByEmail(correo: String): UserEntity? {
        return users[correo.lowercase()]
    }

    override fun observeUserByEmail(correo: String): Flow<UserEntity?> {
        return flowOf(users[correo.lowercase()])
    }

    override suspend fun countByEmail(correo: String): Int {
        return if (users.containsKey(correo.lowercase())) 1 else 0
    }

    override suspend fun insertUser(user: UserEntity) {
        users[user.correo.lowercase()] = user
    }

    override fun getAllUsers(): Flow<List<UserEntity>> {
        return flowOf(users.values.toList())
    }

    override suspend fun getCoinsByEmail(correo: String): Double? = users[correo.lowercase()]?.cantidadTruequeCoins
    override fun observeCoinsByEmail(correo: String): Flow<Double?> = flowOf(users[correo.lowercase()]?.cantidadTruequeCoins)
    override suspend fun reduceCoins(correo: String, amount: Double): Int {
        val u = users[correo.lowercase()] ?: return 0
        users[correo.lowercase()] = u.copy(cantidadTruequeCoins = maxOf(0.0, u.cantidadTruequeCoins - amount))
        return 1
    }
    override suspend fun addCoins(correo: String, amount: Double): Int {
        val u = users[correo.lowercase()] ?: return 0
        users[correo.lowercase()] = u.copy(cantidadTruequeCoins = u.cantidadTruequeCoins + amount)
        return 1
    }
    override suspend fun updateCoins(correo: String, newCoins: Double): Int {
        val u = users[correo.lowercase()] ?: return 0
        users[correo.lowercase()] = u.copy(cantidadTruequeCoins = newCoins)
        return 1
    }
    override suspend fun getCompletedTradesByEmail(correo: String): Int? = users[correo.lowercase()]?.truequesRealizados
    override fun observeCompletedTradesByEmail(correo: String): Flow<Int?> = flowOf(users[correo.lowercase()]?.truequesRealizados)
    override suspend fun incrementCompletedTrades(correo: String): Int {
        val u = users[correo.lowercase()] ?: return 0
        users[correo.lowercase()] = u.copy(truequesRealizados = u.truequesRealizados + 1)
        return 1
    }
    override suspend fun updateCompletedTrades(correo: String, count: Int): Int {
        val u = users[correo.lowercase()] ?: return 0
        users[correo.lowercase()] = u.copy(truequesRealizados = count)
        return 1
    }
    override suspend fun getRatingByEmail(correo: String): Double? = users[correo.lowercase()]?.calificacion
    override fun observeRatingByEmail(correo: String): Flow<Double?> = flowOf(users[correo.lowercase()]?.calificacion)
    override suspend fun updateRating(correo: String, newRating: Double): Int {
        val u = users[correo.lowercase()] ?: return 0
        users[correo.lowercase()] = u.copy(calificacion = newRating)
        return 1
    }
}

private class FakeRegisterSessionDataStore : UserSessionDataStore {
    override val sessionFlow: Flow<UserSession> get() = flowOf(UserSession())
    override suspend fun saveTokens(accessToken: String, refreshToken: String) {}
    override suspend fun saveUser(userId: String, email: String, isKycVerified: Boolean) {}
    override suspend fun getAccessToken(): String? = null
    override suspend fun getRefreshToken(): String? = null
    override suspend fun clearSession() {}
}
