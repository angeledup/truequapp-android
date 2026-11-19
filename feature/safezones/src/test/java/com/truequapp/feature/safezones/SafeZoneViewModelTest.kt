package com.truequapp.feature.safezones

import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.datastore.model.UserSession
import com.truequapp.core.domain.model.GeoCoordinates
import com.truequapp.core.domain.model.MeetingStatus
import com.truequapp.core.domain.model.ReviewVisibilityStatus
import com.truequapp.core.domain.model.SafeZonePoint
import com.truequapp.core.domain.rules.EvaluateReviewVisibilityUseCase
import com.truequapp.core.domain.rules.VerifySafeZonePresenceUseCase
import com.truequapp.feature.safezones.mvi.SafeZoneUiEffect
import com.truequapp.feature.safezones.mvi.SafeZoneUiEvent
import com.truequapp.feature.safezones.mvi.SafeZoneViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

class FakeSafeZoneDataStore : UserSessionDataStore {
    private val _session = MutableStateFlow(
        UserSession(userId = "usr_current", userEmail = "current@truequapp.com", isKycVerified = true)
    )
    override val sessionFlow: Flow<UserSession> = _session
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

class TestSafeZoneDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher get() = dispatcher
    override val io: CoroutineDispatcher get() = dispatcher
    override val default: CoroutineDispatcher get() = dispatcher
    override val unconfined: CoroutineDispatcher get() = dispatcher
}

@OptIn(ExperimentalCoroutinesApi::class)
class SafeZoneViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = TestSafeZoneDispatcherProvider(testDispatcher)
    private val dataStore = FakeSafeZoneDataStore()

    private lateinit var viewModel: SafeZoneViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SafeZoneViewModel(
            verifySafeZonePresenceUseCase = VerifySafeZonePresenceUseCase(),
            evaluateReviewVisibilityUseCase = EvaluateReviewVisibilityUseCase(),
            userSessionDataStore = dataStore,
            dispatchers = dispatchers
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("CU019: Calcula centroide/punto medio geográfico y lista Safe Zones")
    fun initSafeZoneFlow_calculatesMidpointAndSeedsSafeZones() = runTest(testDispatcher) {
        viewModel.onEvent(
            SafeZoneUiEvent.InitSafeZoneFlow(
                tradeId = "trade_001",
                currentUserId = "usr_a",
                counterpartUserId = "usr_b"
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("trade_001", state.tradeId)
        assertTrue(state.safeZones.isNotEmpty(), "Debe contener Puntos Seguros verificados")
        assertNotNull(state.selectedSafeZone)
        assertTrue(state.midpointCoordinates.latitude > 0.0)
    }

    @Test
    @DisplayName("CU019 & CU020: Confirmar cita genera semilla HMAC y PIN TOTP de 6 dígitos")
    fun confirmAppointment_generatesHmacSeedAndOtpPin() = runTest(testDispatcher) {
        viewModel.onEvent(SafeZoneUiEvent.InitSafeZoneFlow("trade_002", "usr_a", "usr_b"))
        advanceUntilIdle()

        viewModel.onEvent(SafeZoneUiEvent.ConfirmAppointment)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.appointment)
        assertNotNull(state.qrSecuritySeed)
        assertNotNull(state.fallbackOtpPin)
        assertEquals(6, state.fallbackOtpPin!!.length, "El PIN de respaldo debe tener 6 dígitos")
    }

    @Test
    @DisplayName("CU020: Escaneo de QR válido liquida trueque y detona vibración háptica")
    fun verifyScannedQr_validSignatureCompletesTrade() = runTest(testDispatcher) {
        viewModel.onEvent(SafeZoneUiEvent.InitSafeZoneFlow("trade_003", "usr_a", "usr_b"))
        viewModel.onEvent(SafeZoneUiEvent.ConfirmAppointment)
        advanceUntilIdle()

        viewModel.onEvent(SafeZoneUiEvent.OnQrCodeScanned("TRUEQUAPP_QR:trade_003:valid_hmac"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isQrVerified)
        assertTrue(state.isTradeCompleted)
        assertEquals(MeetingStatus.COMPLETADA_EXITOSA, state.appointment?.status)
    }

    @Test
    @DisplayName("CU020: Validación de PIN de respaldo offline liquida trueque")
    fun verifyFallbackOtp_validPinCompletesTrade() = runTest(testDispatcher) {
        viewModel.onEvent(SafeZoneUiEvent.InitSafeZoneFlow("trade_004", "usr_a", "usr_b"))
        viewModel.onEvent(SafeZoneUiEvent.ConfirmAppointment)
        advanceUntilIdle()

        val pin = viewModel.uiState.value.fallbackOtpPin!!
        viewModel.onEvent(SafeZoneUiEvent.SubmitFallbackOtp(pin))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isQrVerified)
        assertTrue(state.isTradeCompleted)
    }

    @Test
    @DisplayName("CU022 & RN-08: Detección de Mock Location bloquea la acción con alerta de seguridad")
    fun checkInGpsLocation_rejectsMockLocation() = runTest(testDispatcher) {
        viewModel.onEvent(SafeZoneUiEvent.InitSafeZoneFlow("trade_005", "usr_a", "usr_b"))
        advanceUntilIdle()

        viewModel.onEvent(
            SafeZoneUiEvent.CheckInGpsLocation(
                coordinates = GeoCoordinates(19.3332, -99.1878),
                isMockLocation = true // Falsificación simulada
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isMockLocationDetected)
        assertNotNull(state.mockLocationWarning)
        assertTrue(state.mockLocationWarning!!.contains("Mock Location"))
    }

    @Test
    @DisplayName("RN-08: Validación de geocerca a menos de 150m confirma presencia en Safe Zone")
    fun checkInGpsLocation_within150mRadiusPasses() = runTest(testDispatcher) {
        val safeZone = SafeZonePoint(
            id = "sz_test",
            name = "Safe Zone Test",
            addressDescription = "Test Address",
            coordinates = GeoCoordinates(19.3332, -99.1878)
        )
        viewModel.addTestSafeZone(safeZone)
        viewModel.onEvent(SafeZoneUiEvent.InitSafeZoneFlow("trade_006", "usr_a", "usr_b"))
        viewModel.onEvent(SafeZoneUiEvent.SelectSafeZone("sz_test"))
        advanceUntilIdle()

        // Mismo punto geográfico -> distancia 0m <= 150m
        viewModel.onEvent(
            SafeZoneUiEvent.CheckInGpsLocation(
                coordinates = GeoCoordinates(19.3332, -99.1878),
                isMockLocation = false
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isMockLocationDetected)
        assertTrue(state.isWithin150mRadius)
        assertTrue(state.distanceToSafeZoneMeters <= 150.0)
    }

    @Test
    @DisplayName("RN-12: Publicación de reseña es inmediata y accesible públicamente sin retrasos")
    fun submitReview_publishesImmediatelyUnderRN12() = runTest(testDispatcher) {
        viewModel.onEvent(SafeZoneUiEvent.InitSafeZoneFlow("trade_007", "usr_a", "usr_b"))
        advanceUntilIdle()

        viewModel.onEvent(
            SafeZoneUiEvent.SubmitReview(
                stars = 5,
                wasPunctual = true,
                matchedPhotos = true,
                comment = "Excelente intercambio presencial en Safe Zone."
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isReviewSubmitted)
        assertTrue(state.existingReviews.isNotEmpty())
        assertEquals(ReviewVisibilityStatus.PUBLICA_INMEDIATA, state.existingReviews.first().visibilityStatus)
    }

    @Test
    @DisplayName("CU002: Baja de usuario permitida cuando no existen intercambios en proceso")
    fun requestAccountDeletion_allowsWhenNoActiveLocks() = runTest(testDispatcher) {
        viewModel.onEvent(SafeZoneUiEvent.InitSafeZoneFlow("trade_008", "usr_a", "usr_b"))
        advanceUntilIdle()

        viewModel.onEvent(SafeZoneUiEvent.RequestAccountDeletion)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAccountDeleted)
        assertFalse(state.isAccountDeletionBlocked)
    }
}

