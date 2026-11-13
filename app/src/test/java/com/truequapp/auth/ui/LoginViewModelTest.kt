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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeLoginUserDao
    private lateinit var fakeSession: FakeLoginSessionDataStore
    private lateinit var repository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeLoginUserDao()
        fakeSession = FakeLoginSessionDataStore()
        repository = AuthRepository(fakeDao, fakeSession)
        viewModel = LoginViewModel(repository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows email error when email is not registered in database`() = runTest {
        viewModel.onEvent(LoginUiEvent.EmailChanged("desconocido@correo.com"))
        viewModel.onEvent(LoginUiEvent.PasswordChanged("pass1234"))
        viewModel.onEvent(LoginUiEvent.SubmitLogin)

        advanceUntilIdle()

        assertEquals("No hay una cuenta vinculada al correo", viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun `shows password error when email exists but password is wrong`() = runTest {
        fakeDao.insertUser(
            UserEntity(
                correo = "angel@correo.com",
                contrasena = "correctPass",
                usuario = "angel",
                numeroTelefonico = "12345678"
            )
        )

        viewModel.onEvent(LoginUiEvent.EmailChanged("angel@correo.com"))
        viewModel.onEvent(LoginUiEvent.PasswordChanged("wrongPass"))
        viewModel.onEvent(LoginUiEvent.SubmitLogin)

        advanceUntilIdle()

        assertEquals("Contraseña incorrecta", viewModel.uiState.value.passwordError)
        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun `clears error when email or password is edited`() = runTest {
        viewModel.onEvent(LoginUiEvent.EmailChanged("desconocido@correo.com"))
        viewModel.onEvent(LoginUiEvent.PasswordChanged("pass1234"))
        viewModel.onEvent(LoginUiEvent.SubmitLogin)
        advanceUntilIdle()

        assertEquals("No hay una cuenta vinculada al correo", viewModel.uiState.value.emailError)

        viewModel.onEvent(LoginUiEvent.EmailChanged("nuevo@correo.com"))
        assertNull(viewModel.uiState.value.emailError)
    }
}

private class FakeLoginUserDao : UserDao {
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

    override fun getUserById(id: String): Flow<UserEntity?> = flowOf(users[id.lowercase()])
    override suspend fun getUserByIdSync(id: String): UserEntity? = users[id.lowercase()]
    override suspend fun getUserByEmailSync(email: String): UserEntity? = users[email.lowercase()]
    override suspend fun getUserByIdentifierSync(identifier: String): UserEntity? = users[identifier.lowercase()]
    override suspend fun authenticate(email: String, passwordHash: String): UserEntity? = users[email.lowercase()]
    override suspend fun updateUser(user: UserEntity) { users[user.correo.lowercase()] = user }
    override suspend fun updateTruequeCoins(userId: String, availableCoins: Double, escrowCoins: Double) {}
    override suspend fun getTruequeCoins(userId: String): Double? = users[userId.lowercase()]?.cantidadTruequeCoins
    override suspend fun deleteUser(userId: String) { users.remove(userId.lowercase()) }
}

private class FakeLoginSessionDataStore : UserSessionDataStore {
    override val sessionFlow: Flow<UserSession> get() = flowOf(UserSession())
    override suspend fun saveTokens(accessToken: String, refreshToken: String) {}
    override suspend fun saveUser(
        userId: String,
        email: String,
        isKycVerified: Boolean,
        name: String,
        photoUrl: String?,
        authProvider: String
    ) {}
    override suspend fun getAccessToken(): String? = null
    override suspend fun getRefreshToken(): String? = null
    override suspend fun clearSession() {}
}
