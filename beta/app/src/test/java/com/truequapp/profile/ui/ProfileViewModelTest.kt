package com.truequapp.profile.ui

import com.truequapp.auth.data.AuthRepository
import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.entity.UserEntity
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.datastore.model.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("Pruebas Unitarias de Perfil de Usuario con Base de Datos SQL")
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeProfileUserDao
    private lateinit var fakeSession: FakeProfileSessionDataStore
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: ProfileViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeProfileUserDao()
        fakeSession = FakeProfileSessionDataStore()
        authRepository = AuthRepository(fakeDao, fakeSession)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("Carga correctamente la información de las 7 columnas del usuario en la pantalla de Perfil")
    fun loadProfile_loadsUserFromDatabaseWith7Columns() = runTest {
        val userInDb = UserEntity(
            correo = "perfil@truequapp.com",
            contrasena = "claveSegura123",
            usuario = "truequero_veracruz",
            numeroTelefonico = "+529211234567",
            cantidadTruequeCoins = 250.0,
            truequesRealizados = 8,
            calificacion = 4.9
        )
        fakeDao.insertUser(userInDb)
        fakeSession.saveUser("truequero_veracruz", "perfil@truequapp.com", false)

        viewModel = ProfileViewModel(authRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.user)
        assertEquals("perfil@truequapp.com", state.user?.correo)
        assertEquals("truequero_veracruz", state.user?.usuario)
        assertEquals("+529211234567", state.user?.numeroTelefonico)
        assertEquals(250.0, state.user?.cantidadTruequeCoins)
        assertEquals(8, state.user?.truequesRealizados)
        assertEquals(4.9, state.user?.calificacion)
    }

    @Test
    @DisplayName("Cerrar sesión limpia el UserSessionDataStore y emite efecto NavigateToLogin")
    fun logout_clearsSessionAndNavigatesToLogin() = runTest {
        fakeSession.saveUser("truequero_veracruz", "perfil@truequapp.com", false)

        viewModel = ProfileViewModel(authRepository)
        advanceUntilIdle()

        viewModel.onEvent(ProfileUiEvent.Logout)
        advanceUntilIdle()

        val currentSession = fakeSession.sessionFlow.first()
        assertNull(currentSession.userEmail)
        assertNull(currentSession.userId)

        val effect = viewModel.uiEffect.first()
        assertTrue(effect is ProfileUiEffect.NavigateToLogin)
    }
}

private class FakeProfileUserDao : UserDao {
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

private class FakeProfileSessionDataStore : UserSessionDataStore {
    private val _sessionFlow = MutableStateFlow(UserSession())
    override val sessionFlow: Flow<UserSession> get() = _sessionFlow

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        _sessionFlow.value = _sessionFlow.value.copy(accessToken = accessToken, refreshToken = refreshToken)
    }

    override suspend fun saveUser(
        userId: String,
        email: String,
        isKycVerified: Boolean,
        name: String,
        photoUrl: String?,
        authProvider: String
    ) {
        _sessionFlow.value = _sessionFlow.value.copy(
            userId = userId,
            userEmail = email,
            isKycVerified = isKycVerified,
            userName = name,
            userPhotoUrl = photoUrl,
            authProvider = authProvider
        )
    }

    override suspend fun getAccessToken(): String? = _sessionFlow.value.accessToken
    override suspend fun getRefreshToken(): String? = _sessionFlow.value.refreshToken
    override suspend fun clearSession() {
        _sessionFlow.value = UserSession()
    }
}
