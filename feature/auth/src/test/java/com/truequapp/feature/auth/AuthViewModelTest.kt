package com.truequapp.feature.auth

import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.datastore.model.UserSession
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

    override suspend fun saveUser(userId: String, email: String, isKycVerified: Boolean) {
        _session.update { it.copy(userId = userId, userEmail = email, isKycVerified = isKycVerified) }
    }

    override suspend fun getAccessToken(): String? = _session.value.accessToken
    override suspend fun getRefreshToken(): String? = _session.value.refreshToken
    override suspend fun clearSession() { _session.value = UserSession() }
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
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)

    private lateinit var viewModel: AuthViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(
            userSessionDataStore = fakeDataStore,
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
}
