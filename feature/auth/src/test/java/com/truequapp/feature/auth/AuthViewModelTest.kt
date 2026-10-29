package com.truequapp.feature.auth

import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.datastore.model.UserSession
import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.entity.UserEntity
import com.truequapp.core.domain.model.KycLevel
import com.truequapp.feature.auth.mvi.AuthUiEffect
import com.truequapp.feature.auth.mvi.AuthUiEvent
import com.truequapp.feature.auth.mvi.AuthViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FakeUserSessionDataStore : UserSessionDataStore {
    private val _session = MutableStateFlow(UserSession())
    override val sessionFlow: Flow<UserSession> = _session

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        _session.update { it.copy(accessToken = accessToken, refreshToken = refreshToken) }
    }

    override suspend fun saveUser(
        userId: String,
        email: String,
        isKycVerified: Boolean,
        name: String,
        photoUrl: String?,
        authProvider: String
    ) {
        _session.update {
            it.copy(
                userId = userId,
                userEmail = email,
                isKycVerified = isKycVerified,
                userName = name,
                userPhotoUrl = photoUrl,
                authProvider = authProvider
            )
        }
    }

    override suspend fun getAccessToken(): String? = _session.value.accessToken
    override suspend fun getRefreshToken(): String? = _session.value.refreshToken
    override suspend fun clearSession() { _session.value = UserSession() }
}

class FakeUserDao : UserDao {
    val users = mutableMapOf<String, UserEntity>()

    override fun getUserById(id: String): Flow<UserEntity?> = MutableStateFlow(users[id])
    override suspend fun getUserByIdSync(id: String): UserEntity? = users[id]
    override fun getUserByEmail(email: String): Flow<UserEntity?> = MutableStateFlow(users.values.find { it.email.equals(email, ignoreCase = true) })
    override suspend fun getUserByEmailSync(email: String): UserEntity? = users.values.find { it.email.equals(email, ignoreCase = true) }
    override suspend fun authenticate(email: String, passwordHash: String): UserEntity? = users.values.find { it.email == email && it.passwordHash == passwordHash }
    override suspend fun insertUser(user: UserEntity) { users[user.id] = user }
    override suspend fun updateUser(user: UserEntity) { users[user.id] = user }
    override suspend fun updateTruequeCoins(userId: String, availableCoins: Double, escrowCoins: Double) {
        users[userId]?.let { users[userId] = it.copy(truequeCoins = availableCoins, escrowCoins = escrowCoins) }
    }
    override suspend fun getTruequeCoins(userId: String): Double? = users[userId]?.truequeCoins
    override fun getAllUsers(): Flow<List<UserEntity>> = MutableStateFlow(users.values.toList())
    override suspend fun deleteUser(userId: String) { users.remove(userId) }
}

class TestDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher get() = dispatcher
    override val io: CoroutineDispatcher get() = dispatcher
    override val default: CoroutineDispatcher get() = dispatcher
    override val unconfined: CoroutineDispatcher get() = dispatcher
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeDataStore = FakeUserSessionDataStore()
    private val fakeUserDao = FakeUserDao()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)

    private lateinit var viewModel: AuthViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(
            userSessionDataStore = fakeDataStore,
            userDao = fakeUserDao,
            dispatchers = testDispatcherProvider
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("CU001: Registro con teléfono no E.164 debe emitir mensaje de error")
    fun shouldEmitError_whenPhoneIsNotE164() = runTest(testDispatcher) {
        viewModel.onEvent(AuthUiEvent.ToggleAuthMode) // Register mode
        viewModel.onEvent(AuthUiEvent.NameChanged("Carlos Mendoza"))
        viewModel.onEvent(AuthUiEvent.EmailChanged("carlos@example.com"))
        viewModel.onEvent(AuthUiEvent.PhoneChanged("2281234567")) // Inválido: falta prefijo '+'
        viewModel.onEvent(AuthUiEvent.PasswordChanged("secret123"))

        viewModel.onEvent(AuthUiEvent.SubmitRegister)

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("E.164"))
        assertFalse(state.isOtpDialogVisible)
    }

    @Test
    @DisplayName("CU001: Registro válido activa el modal de verificación SMS OTP con 300 segundos")
    fun shouldShowOtpDialog_whenRegisterIsValid() = runTest(testDispatcher) {
        viewModel.onEvent(AuthUiEvent.ToggleAuthMode)
        viewModel.onEvent(AuthUiEvent.NameChanged("Carlos Mendoza"))
        viewModel.onEvent(AuthUiEvent.EmailChanged("carlos@example.com"))
        viewModel.onEvent(AuthUiEvent.PhoneChanged("+522281234567")) // Válido E.164
        viewModel.onEvent(AuthUiEvent.PasswordChanged("secret123"))

        viewModel.onEvent(AuthUiEvent.SubmitRegister)

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertTrue(state.isOtpDialogVisible)
        assertEquals(300, state.otpSecondsRemaining)
    }

    @Test
    @DisplayName("CU001: Verificación de SMS OTP con 6 dígitos eleva a KYC 1 e inicializa 100 TC")
    fun shouldUpgradeToKyc1AndInitWallet_whenOtpIsVerified() = runTest(testDispatcher) {
        viewModel.onEvent(AuthUiEvent.ToggleAuthMode)
        viewModel.onEvent(AuthUiEvent.NameChanged("Carlos Mendoza"))
        viewModel.onEvent(AuthUiEvent.EmailChanged("carlos@example.com"))
        viewModel.onEvent(AuthUiEvent.PhoneChanged("+522281234567"))
        viewModel.onEvent(AuthUiEvent.PasswordChanged("secret123"))
        viewModel.onEvent(AuthUiEvent.SubmitRegister)

        viewModel.onEvent(AuthUiEvent.OtpCodeChanged("123456"))
        viewModel.onEvent(AuthUiEvent.VerifyOtp)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLoggedIn)
        assertEquals(KycLevel.TELEFONO_SMS_NIVEL_1, state.kycLevel)
        assertEquals(100.0, state.truequeCoins)
        assertFalse(state.isOtpDialogVisible)
        assertTrue(fakeDataStore.sessionFlow.first().isAuthenticated)
    }

    @Test
    @DisplayName("CU023: Login con credenciales válidas inicia sesión y carga reputación bayesiana")
    fun shouldLoginSuccessfully_whenCredentialsAreValid() = runTest(testDispatcher) {
        viewModel.onEvent(AuthUiEvent.EmailChanged("veterano@example.com"))
        viewModel.onEvent(AuthUiEvent.PasswordChanged("segura123"))

        viewModel.onEvent(AuthUiEvent.SubmitLogin)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLoggedIn)
        assertEquals(KycLevel.TELEFONO_SMS_NIVEL_1, state.kycLevel)
        assertTrue(state.bayesianRating in 1.0..5.0)
        assertTrue(state.obfuscatedNeighborhood.isNotBlank())
        assertTrue(fakeDataStore.sessionFlow.first().isAuthenticated)
    }

    @Test
    @DisplayName("CU024: Logout limpia la sesión en DataStore y reinicia el estado a no logueado")
    fun shouldClearSession_whenLogoutIsTriggered() = runTest(testDispatcher) {
        // Primero loguear
        viewModel.onEvent(AuthUiEvent.EmailChanged("test@example.com"))
        viewModel.onEvent(AuthUiEvent.PasswordChanged("123456"))
        viewModel.onEvent(AuthUiEvent.SubmitLogin)
        advanceUntilIdle()

        // Luego desloguear
        viewModel.onEvent(AuthUiEvent.Logout)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoggedIn)
        assertFalse(fakeDataStore.sessionFlow.first().isAuthenticated)
    }

    @Test
    @DisplayName("Google Sign-In: Inicia sesión correctamente con cuenta de Google y persiste sesión")
    fun shouldLoginSuccessfully_whenGoogleSignInSucceeds() = runTest(testDispatcher) {
        viewModel.onEvent(
            AuthUiEvent.SignInWithGoogleSuccess(
                googleId = "goog_12345",
                email = "estudiante.unam@gmail.com",
                displayName = "Estudiante UNAM",
                photoUrl = "https://lh3.googleusercontent.com/photo"
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLoggedIn)
        assertEquals("GOOGLE", state.authProvider)
        assertEquals("estudiante.unam@gmail.com", state.emailInput)
        assertEquals("Estudiante UNAM", state.nameInput)
        assertEquals(100.0, state.truequeCoins)
        assertTrue(fakeDataStore.sessionFlow.first().isAuthenticated)
    }

    @Test
    @DisplayName("Room SQLite: Login con usuario existente carga sus datos reales y no strings genéricos")
    fun shouldLoadExistingUserFromDatabase_whenLoggingIn() = runTest(testDispatcher) {
        val existingUser = UserEntity(
            id = "usr_coatza_99",
            email = "carlos.mendoza@coatza.com",
            name = "Carlos Mendoza Coatzacoalcos",
            passwordHash = "segura123",
            phoneNumber = "+529211112233",
            authProvider = "EMAIL",
            truequeCoins = 150.0,
            obfuscatedNeighborhood = "Col. Petrolera, Coatzacoalcos",
            city = "Coatzacoalcos"
        )
        fakeUserDao.insertUser(existingUser)

        viewModel.onEvent(AuthUiEvent.EmailChanged("carlos.mendoza@coatza.com"))
        viewModel.onEvent(AuthUiEvent.PasswordChanged("segura123"))
        viewModel.onEvent(AuthUiEvent.SubmitLogin)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLoggedIn)
        assertEquals("Carlos Mendoza Coatzacoalcos", state.userName)
        assertEquals("carlos.mendoza@coatza.com", state.userEmail)
        assertEquals("+529211112233", state.userPhone)
        assertEquals(150.0, state.truequeCoins)
        assertEquals("Col. Petrolera, Coatzacoalcos", state.userNeighborhood)
    }

    @Test
    @DisplayName("Room SQLite: Editar perfil persiste cambios en la base de datos y actualiza UI")
    fun shouldUpdateProfileInDatabaseAndState_whenSaveEditedProfile() = runTest(testDispatcher) {
        // Primero registrar o iniciar sesión
        viewModel.onEvent(AuthUiEvent.EmailChanged("usuario.nuevo@gmail.com"))
        viewModel.onEvent(AuthUiEvent.PasswordChanged("pass12345"))
        viewModel.onEvent(AuthUiEvent.NameChanged("Nombre Inicial"))
        viewModel.onEvent(AuthUiEvent.SubmitLogin)
        advanceUntilIdle()

        // Abrir diálogo y modificar datos
        viewModel.onEvent(AuthUiEvent.OpenEditProfileDialog)
        viewModel.onEvent(AuthUiEvent.EditNameChanged("Angel Eduardo"))
        viewModel.onEvent(AuthUiEvent.EditPhoneChanged("+529217654321"))
        viewModel.onEvent(AuthUiEvent.EditNeighborhoodChanged("Puerto México, Coatzacoalcos"))
        viewModel.onEvent(AuthUiEvent.SaveEditedProfile)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Angel Eduardo", state.userName)
        assertEquals("+529217654321", state.userPhone)
        assertEquals("Puerto México, Coatzacoalcos", state.userNeighborhood)
        assertFalse(state.isEditProfileDialogVisible)

        // Verificar persistencia en Room DAO
        val userInDb = fakeUserDao.getUserByEmailSync("usuario.nuevo@gmail.com")
        assertNotNull(userInDb)
        assertEquals("Angel Eduardo", userInDb?.name)
        assertEquals("+529217654321", userInDb?.phoneNumber)
        assertEquals("Puerto México, Coatzacoalcos", userInDb?.obfuscatedNeighborhood)
    }
}
