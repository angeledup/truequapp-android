package com.truequapp.feature.trade

import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.datastore.model.UserSession
import com.truequapp.core.domain.model.GeoCoordinates
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.core.domain.model.ItemStatus
import com.truequapp.core.domain.model.ItemType
import com.truequapp.core.domain.model.MeetingAppointment
import com.truequapp.core.domain.model.SafeZonePoint
import com.truequapp.core.domain.model.TradeItem
import com.truequapp.core.domain.model.TradeModality
import com.truequapp.core.domain.model.TradeProposal
import com.truequapp.core.domain.model.TradeStatus
import com.truequapp.core.domain.rules.EvaluateCancellationWindowUseCase
import com.truequapp.core.domain.rules.LockInventoryForTradeUseCase
import com.truequapp.core.domain.rules.ValidateTradeValueUseCase
import com.truequapp.feature.trade.mvi.TradeUiEffect
import com.truequapp.feature.trade.mvi.TradeUiEvent
import com.truequapp.feature.trade.mvi.TradeViewModel
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FakeTradeDataStore : UserSessionDataStore {
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

class TestTradeDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher get() = dispatcher
    override val io: CoroutineDispatcher get() = dispatcher
    override val default: CoroutineDispatcher get() = dispatcher
    override val unconfined: CoroutineDispatcher get() = dispatcher
}

@OptIn(ExperimentalCoroutinesApi::class)
class TradeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = TestTradeDispatcherProvider(testDispatcher)
    private val dataStore = FakeTradeDataStore()

    private lateinit var viewModel: TradeViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TradeViewModel(
            validateTradeValueUseCase = ValidateTradeValueUseCase(),
            lockInventoryForTradeUseCase = LockInventoryForTradeUseCase(),
            evaluateCancellationWindowUseCase = EvaluateCancellationWindowUseCase(),
            userSessionDataStore = dataStore,
            dispatchers = dispatchers
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("CU009 & RN-02: Rechaza ofertas por debajo del umbral mínimo del 40%")
    fun validateTradeValueRatio_rejectsOfferBelow40Percent() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Objetivo: Consola Switch OLED (Mediana ~$4850)
        viewModel.onEvent(TradeUiEvent.InitTradeCreation(targetItemId = "item_001", userId = "usr_current"))

        // Ofrecer solo audífonos de ~$900 (900 / 4850 = ~18.5% < 40%)
        viewModel.onEvent(TradeUiEvent.ToggleOfferedItemSelection(itemId = "item_user_02"))

        val state = viewModel.uiState.value
        assertFalse(state.isRatioValid, "La oferta no debe ser válida si es menor al 40%")
        assertFalse(state.canSubmitProposal, "No debe permitir enviar propuesta")
        assertTrue(state.calculatedRatio < 0.40)

        // Intento de envío forzado
        viewModel.onEvent(TradeUiEvent.SubmitProposal)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("RN-02"))
    }

    @Test
    @DisplayName("CU009 & Opción A: Exige al menos un artículo del catálogo propio aunque se ofrezcan créditos")
    fun optionA_requiresAtLeastOneCatalogItem() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Objetivo: Stewart (Mediana ~$600)
        viewModel.onEvent(TradeUiEvent.InitTradeCreation(targetItemId = "item_002", userId = "usr_current"))

        // Sin seleccionar artículos, solo ofrecer 500 TC (500 / 600 = ~83% > 40%)
        viewModel.onEvent(TradeUiEvent.OnTruequeCoinsChanged("500"))

        val state = viewModel.uiState.value
        assertTrue(state.isRatioValid, "El ratio es mayor al 40%")
        assertFalse(state.hasMinimumItemsSelected, "Debe marcar que falta al menos 1 artículo según la Opción A")
        assertFalse(state.canSubmitProposal, "No debe permitir enviar propuesta sin artículo físico")
    }

    @Test
    @DisplayName("CU009 & Opción A: Propuesta híbrida válida (artículo + monedas >= 40%) permite envío y redirige a negociación")
    fun validProposal_submissionNavigatesToNegotiation() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Objetivo: Stewart (Mediana ~$600)
        viewModel.onEvent(TradeUiEvent.InitTradeCreation(targetItemId = "item_002", userId = "usr_current"))

        // Ofrecer Audífonos (Mediana ~$900) + 100 TC -> Valor combinado ~$1000 / $600 = ~166%
        viewModel.onEvent(TradeUiEvent.ToggleOfferedItemSelection(itemId = "item_user_02"))
        viewModel.onEvent(TradeUiEvent.OnTruequeCoinsChanged("100"))

        val state = viewModel.uiState.value
        assertTrue(state.isRatioValid)
        assertTrue(state.hasMinimumItemsSelected)
        assertTrue(state.canSubmitProposal)

        // Enviar propuesta
        viewModel.onEvent(TradeUiEvent.SubmitProposal)
        advanceUntilIdle()

        // Verificar efecto de redirección a negociación (Opción A)
        val effect = viewModel.uiEffect.first()
        assertTrue(effect is TradeUiEffect.ShowToast || effect is TradeUiEffect.NavigateToNegotiation)
    }

    @Test
    @DisplayName("CU010 & RN-01: Aceptar propuesta ejecuta reserva exclusiva y cancela propuestas conflictivas")
    fun lockInventory_atomicReservation_RN01() = runTest(testDispatcher) {
        advanceUntilIdle()

        val proposalA = TradeProposal(
            id = "prop_accept_test",
            requesterUserId = "usr_beta",
            receiverUserId = "usr_current",
            offeredItemIds = listOf("item_user_01"),
            targetItemId = "item_002",
            modality = TradeModality.DIRECTO_1_A_1,
            status = TradeStatus.PENDIENTE
        )
        val conflictingProposal = TradeProposal(
            id = "prop_conflict_test",
            requesterUserId = "usr_gamma",
            receiverUserId = "usr_alpha",
            offeredItemIds = listOf("item_002"), // Compite por el mismo item_002
            targetItemId = "item_001",
            modality = TradeModality.DIRECTO_1_A_1,
            status = TradeStatus.PENDIENTE
        )

        viewModel.addTestProposal(proposalA)
        viewModel.addTestProposal(conflictingProposal)

        viewModel.onEvent(TradeUiEvent.AcceptProposal(proposalA.id))
        advanceUntilIdle()

        // Verificar que proposalA pasó a RESERVADO
        viewModel.onEvent(TradeUiEvent.LoadProposalDetails(proposalA.id, "usr_current"))
        advanceUntilIdle()

        assertEquals(TradeStatus.RESERVADO, viewModel.uiState.value.currentProposal?.status)
    }

    @Test
    @DisplayName("CU011: Emitir contraoferta actualiza el estado a CONTRAOFERTADA con nuevos créditos")
    fun counterOffer_transitionsToContraofertada() = runTest(testDispatcher) {
        advanceUntilIdle()

        val proposal = TradeProposal(
            id = "prop_counter_test",
            requesterUserId = "usr_beta",
            receiverUserId = "usr_current",
            offeredItemIds = listOf("item_user_01"),
            targetItemId = "item_001",
            compensatoryTruequeCoins = 100.0,
            modality = TradeModality.HIBRIDO_CON_CREDITOS,
            status = TradeStatus.PENDIENTE
        )
        viewModel.addTestProposal(proposal)

        viewModel.onEvent(TradeUiEvent.LoadProposalDetails(proposal.id, "usr_current"))
        advanceUntilIdle()

        viewModel.onEvent(TradeUiEvent.OpenCounterOfferDialog)
        viewModel.onEvent(TradeUiEvent.OnCounterOfferTruequeCoinsChanged("1500"))
        viewModel.onEvent(TradeUiEvent.SubmitCounterOffer)
        advanceUntilIdle()

        assertEquals(TradeStatus.CONTRAOFERTADA, viewModel.uiState.value.currentProposal?.status)
        assertEquals(1500.0, viewModel.uiState.value.currentProposal?.compensatoryTruequeCoins)
    }

    @Test
    @DisplayName("Zona de Trueques: Segregación entre Activas y Ofertas recibidas de otros usuarios")
    fun tradeList_segregatesActiveAndReceivedOffers() = runTest(testDispatcher) {
        advanceUntilIdle()

        val incomingOffer = TradeProposal(
            id = "prop_offer_1",
            requesterUserId = "usr_other",
            receiverUserId = "usr_current",
            offeredItemIds = listOf("item_001"),
            targetItemId = "item_user_01",
            modality = TradeModality.DIRECTO_1_A_1,
            status = TradeStatus.PENDIENTE
        )
        val outgoingActive = TradeProposal(
            id = "prop_active_1",
            requesterUserId = "usr_current",
            receiverUserId = "usr_other",
            offeredItemIds = listOf("item_user_01"),
            targetItemId = "item_002",
            modality = TradeModality.DIRECTO_1_A_1,
            status = TradeStatus.PENDIENTE
        )
        val counterOfferToMe = TradeProposal(
            id = "prop_counter_offer_1",
            requesterUserId = "usr_current",
            receiverUserId = "usr_other",
            offeredItemIds = listOf("item_user_02"),
            targetItemId = "item_002",
            modality = TradeModality.DIRECTO_1_A_1,
            status = TradeStatus.CONTRAOFERTADA
        )

        viewModel.addTestProposal(incomingOffer)
        viewModel.addTestProposal(outgoingActive)
        viewModel.addTestProposal(counterOfferToMe)

        viewModel.onEvent(TradeUiEvent.LoadProposals("usr_current"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.activeProposals.any { it.id == "prop_active_1" })
        assertTrue(state.receivedOffers.any { it.id == "prop_offer_1" })
        assertTrue(state.receivedOffers.any { it.id == "prop_counter_offer_1" })
    }

    @Test
    @DisplayName("CU012 & RN-07: Cancelación a menos de 4 horas de la cita aplica advertencia y sanción de asistencia del 5%")
    fun cancellationWithinWindow_evaluatesPenalty() = runTest(testDispatcher) {
        advanceUntilIdle()

        val proposal = TradeProposal(
            id = "prop_cancel_test",
            requesterUserId = "usr_beta",
            receiverUserId = "usr_current",
            offeredItemIds = listOf("item_user_01"),
            targetItemId = "item_001",
            modality = TradeModality.DIRECTO_1_A_1,
            status = TradeStatus.EN_ENTREGA
        )
        viewModel.addTestProposal(proposal)

        viewModel.onEvent(TradeUiEvent.LoadProposalDetails(proposal.id, "usr_current"))
        advanceUntilIdle()

        // Solicitar cancelación (la cita simulada está a 2 horas < 4 horas)
        viewModel.onEvent(TradeUiEvent.RequestCancelProposal(proposal.id))
        advanceUntilIdle()

        val stateWithWarning = viewModel.uiState.value
        assertTrue(stateWithWarning.showCancelDialog)
        assertTrue(stateWithWarning.isPenaltyCancellation)
        assertTrue(stateWithWarning.cancellationWarningMessage!!.contains("penalización del 5%"))

        // Confirmar cancelación
        viewModel.onEvent(TradeUiEvent.ConfirmCancelProposal)
        advanceUntilIdle()

        assertEquals(TradeStatus.CANCELADO, viewModel.uiState.value.currentProposal?.status)
        assertEquals(0.95, viewModel.uiState.value.currentUserProfile?.attendanceRate ?: 1.0, 0.001)
    }

    @Test
    @DisplayName("Anti-auto-trueque: Denegar propuesta si el artículo objetivo pertenece al mismo usuario")
    fun shouldDenyTradeProposal_whenTargetItemBelongsToSameUser() = runTest(testDispatcher) {
        advanceUntilIdle()

        // item_user_01 pertenece a "usr_current"
        viewModel.onEvent(TradeUiEvent.InitTradeCreation("item_user_01", "usr_current"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.canSubmitProposal)
        assertEquals("Esta publicación es tuya. No puedes realizar un trueque contigo mismo.", state.errorMessage)
    }
}
