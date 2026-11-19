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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("Pruebas Unitarias de Ajustes de Usuario (Modificación de Nombre, Contraseña y Correo)")
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeSettingsUserDao
    private lateinit var fakeSession: FakeSettingsSessionDataStore
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: SettingsViewModel

    private val initialUser = UserEntity(
        correo = "original@truequapp.com",
        contrasena = "password123",
        usuario = "usuario_original",
        numeroTelefonico = "+525511223344",
        cantidadTruequeCoins = 150.0,
        truequesRealizados = 3,
        calificacion = 4.8
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeSettingsUserDao()
        fakeSession = FakeSettingsSessionDataStore()
        authRepository = AuthRepository(fakeDao, fakeSession).apply {
            cloudBaseUrl = "" // Desactivar llamadas de red en tests unitarios
        }

        fakeDao.insertUserSync(initialUser)
        fakeSession.saveUserSync(initialUser.usuario, initialUser.correo, false)

        viewModel = SettingsViewModel(authRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("Carga correctamente los ajustes actuales del usuario en sesión")
    fun loadCurrentSettings_loadsCorrectly() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("usuario_original", state.username)
        assertEquals("original@truequapp.com", state.email)
        assertEquals("password123", state.password)
        assertEquals("original@truequapp.com", state.currentEmail)
    }

    @Test
    @DisplayName("Modifica exitosamente nombre de usuario, contraseña y correo registrado")
    fun saveSettings_modifiesUsernamePasswordAndEmailSuccessfully() = runTest {
        advanceUntilIdle()

        viewModel.onEvent(SettingsUiEvent.UsernameChanged("nuevo_nombre"))
        viewModel.onEvent(SettingsUiEvent.EmailChanged("nuevo_correo@truequapp.com"))
        viewModel.onEvent(SettingsUiEvent.PasswordChanged("nuevaContrasena456"))

        viewModel.onEvent(SettingsUiEvent.SaveSettings)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals("nuevo_nombre", state.username)
        assertEquals("nuevo_correo@truequapp.com", state.email)
        assertEquals("nuevaContrasena456", state.password)

        // Verificar persistencia en UserDao
        val updatedInDb = fakeDao.getUserByEmail("nuevo_correo@truequapp.com")
        assertNotNull(updatedInDb)
        assertEquals("nuevo_nombre", updatedInDb?.usuario)
        assertEquals("nuevaContrasena456", updatedInDb?.contrasena)
        assertEquals("nuevo_correo@truequapp.com", updatedInDb?.correo)
        // Datos intactos
        assertEquals(150.0, updatedInDb?.cantidadTruequeCoins)

        // Verificar actualización en la sesión de DataStore
        val session = fakeSession.sessionFlow.first()
        assertEquals("nuevo_correo@truequapp.com", session.userEmail)
        assertEquals("nuevo_nombre", session.userName)
    }

    @Test
    @DisplayName("Valida que la contraseña tenga una longitud mínima de 4 caracteres")
    fun saveSettings_validatesShortPassword() = runTest {
        advanceUntilIdle()

        viewModel.onEvent(SettingsUiEvent.PasswordChanged("123"))
        viewModel.onEvent(SettingsUiEvent.SaveSettings)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals("La contraseña debe tener al menos 4 caracteres", state.errorMessage)
    }

    @Test
    @DisplayName("Valida formato de correo inválido")
    fun saveSettings_validatesInvalidEmail() = runTest {
        advanceUntilIdle()

        viewModel.onEvent(SettingsUiEvent.EmailChanged("correoInvalidoSinArroba"))
        viewModel.onEvent(SettingsUiEvent.SaveSettings)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals("Ingresa un correo electrónico válido", state.errorMessage)
    }

    @Test
    @DisplayName("Rechaza modificación si el nuevo correo ya pertenece a otro usuario")
    fun saveSettings_rejectsDuplicatedEmail() = runTest {
        // Registrar a otro usuario
        fakeDao.insertUser(
            UserEntity(
                correo = "otro@truequapp.com",
                contrasena = "otraClave123",
                usuario = "otro_user",
                numeroTelefonico = "+525599887766"
            )
        )

        advanceUntilIdle()

        viewModel.onEvent(SettingsUiEvent.EmailChanged("otro@truequapp.com"))
        viewModel.onEvent(SettingsUiEvent.SaveSettings)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals("El correo ya se encuentra registrado por otra cuenta", state.errorMessage)
    }

    @Test
    @DisplayName("Alterna visibilidad de contraseña")
    fun togglePasswordVisibility_togglesBoolean() = runTest {
        assertFalse(viewModel.uiState.value.isPasswordVisible)
        viewModel.onEvent(SettingsUiEvent.TogglePasswordVisibility)
        assertTrue(viewModel.uiState.value.isPasswordVisible)
        viewModel.onEvent(SettingsUiEvent.TogglePasswordVisibility)
        assertFalse(viewModel.uiState.value.isPasswordVisible)
    }
}

// -----------------------------------------------------------------------------
// FAKES PARA TEST DE AJUSTES
// -----------------------------------------------------------------------------

private class FakeSettingsUserDao : UserDao {
    private val users = mutableMapOf<String, UserEntity>()

    override suspend fun getUserByEmail(correo: String): UserEntity? = users[correo.lowercase()]
    override fun observeUserByEmail(correo: String): Flow<UserEntity?> = flowOf(users[correo.lowercase()])
    override suspend fun countByEmail(correo: String): Int = if (users.containsKey(correo.lowercase())) 1 else 0
    fun insertUserSync(user: UserEntity) { users[user.correo.lowercase()] = user }
    override suspend fun insertUser(user: UserEntity) { users[user.correo.lowercase()] = user }
    override fun getAllUsers(): Flow<List<UserEntity>> = flowOf(users.values.toList())
    override suspend fun updateUser(user: UserEntity) { users[user.correo.lowercase()] = user }

    override suspend fun updateUserCredentials(oldEmail: String, newEmail: String, newUsername: String, newPassword: String): Int {
        val existing = users.remove(oldEmail.lowercase()) ?: return 0
        val updated = existing.copy(
            correo = newEmail,
            usuario = newUsername,
            contrasena = newPassword
        )
        users[newEmail.lowercase()] = updated
        return 1
    }

    override suspend fun getCoinsByEmail(correo: String): Double? = users[correo.lowercase()]?.cantidadTruequeCoins
    override fun observeCoinsByEmail(correo: String): Flow<Double?> = flowOf(users[correo.lowercase()]?.cantidadTruequeCoins)
    override suspend fun reduceCoins(correo: String, amount: Double): Int = 1
    override suspend fun addCoins(correo: String, amount: Double): Int = 1
    override suspend fun updateCoins(correo: String, newCoins: Double): Int = 1
    override suspend fun getCompletedTradesByEmail(correo: String): Int? = users[correo.lowercase()]?.truequesRealizados
    override fun observeCompletedTradesByEmail(correo: String): Flow<Int?> = flowOf(users[correo.lowercase()]?.truequesRealizados)
    override suspend fun incrementCompletedTrades(correo: String): Int = 1
    override suspend fun updateCompletedTrades(correo: String, count: Int): Int = 1
    override suspend fun getRatingByEmail(correo: String): Double? = users[correo.lowercase()]?.calificacion
    override fun observeRatingByEmail(correo: String): Flow<Double?> = flowOf(users[correo.lowercase()]?.calificacion)
    override suspend fun updateRating(correo: String, newRating: Double): Int = 1
    override fun getUserById(id: String): Flow<UserEntity?> = flowOf(users[id.lowercase()])
    override suspend fun getUserByIdSync(id: String): UserEntity? = users[id.lowercase()]
    override suspend fun getUserByEmailSync(email: String): UserEntity? = users[email.lowercase()]
    override suspend fun getUserByIdentifierSync(identifier: String): UserEntity? = users[identifier.lowercase()]
    override suspend fun authenticate(email: String, passwordHash: String): UserEntity? = users[email.lowercase()]
    override suspend fun updateTruequeCoins(userId: String, availableCoins: Double, escrowCoins: Double) {}
    override suspend fun getTruequeCoins(userId: String): Double? = users[userId.lowercase()]?.cantidadTruequeCoins
    override suspend fun deleteUser(userId: String) { users.remove(userId.lowercase()) }
}

private class FakeSettingsSessionDataStore : UserSessionDataStore {
    private val _sessionFlow = MutableStateFlow(UserSession())
    override val sessionFlow: Flow<UserSession> get() = _sessionFlow

    fun saveUserSync(userId: String, email: String, isKycVerified: Boolean) {
        _sessionFlow.value = _sessionFlow.value.copy(
            userId = userId,
            userEmail = email,
            isKycVerified = isKycVerified,
            userName = userId
        )
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {}

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
