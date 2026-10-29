package com.truequapp.feature.wallet

import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.datastore.model.UserSession
import com.truequapp.core.domain.model.TriangularCycleStatus
import com.truequapp.core.domain.rules.EscrowHoldAndReleaseUseCase
import com.truequapp.core.domain.rules.LedgerDoubleEntryValidatorUseCase
import com.truequapp.core.domain.rules.TriangularCycleRulesUseCase
import com.truequapp.feature.wallet.mvi.WalletUiEffect
import com.truequapp.feature.wallet.mvi.WalletUiEvent
import com.truequapp.feature.wallet.mvi.WalletViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FakeWalletDataStore : UserSessionDataStore {
    private val _session = MutableStateFlow(UserSession())
    override val sessionFlow: Flow<UserSession> = _session
    override suspend fun saveTokens(accessToken: String, refreshToken: String) {}
    override suspend fun saveUser(userId: String, email: String, isKycVerified: Boolean, name: String, photoUrl: String?, authProvider: String) {}
    override suspend fun updateTruequeCoins(available: Double, escrow: Double?) {
        _session.update { it.copy(truequeCoins = available, escrowCoins = escrow ?: it.escrowCoins) }
    }
    override suspend fun getAccessToken(): String? = null
    override suspend fun getRefreshToken(): String? = null
    override suspend fun getTruequeCoins(): Double = _session.value.truequeCoins
    override suspend fun clearSession() {}
}

class TestWalletDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher get() = dispatcher
    override val io: CoroutineDispatcher get() = dispatcher
    override val default: CoroutineDispatcher get() = dispatcher
    override val unconfined: CoroutineDispatcher get() = dispatcher
}

@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: WalletViewModel
    private val fakeDataStore = FakeWalletDataStore()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = WalletViewModel(
            escrowUseCase = EscrowHoldAndReleaseUseCase(),
            ledgerUseCase = LedgerDoubleEntryValidatorUseCase(),
            triangularUseCase = TriangularCycleRulesUseCase(),
            userSessionDataStore = fakeDataStore,
            dispatchers = TestWalletDispatcherProvider(testDispatcher)
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialState preserves RN-04 double entry invariant`() {
        val state = viewModel.uiState.value
        assertEquals(state.wallet.availableBalance + state.wallet.escrowBalance, state.totalBalance)
        assertTrue(state.isInvariantPreserved)
        assertFalse(state.ledgerHistory.isEmpty())
        assertNotNull(state.triangularCycle)
    }

    @Test
    fun `setSecurityPin updates wallet state`() = runTest(testDispatcher) {
        viewModel.onEvent(WalletUiEvent.SetSecurityPin("1234"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.wallet.isPinConfigured)
        assertNotNull(state.successMessage)
    }

    @Test
    fun `setSecurityPin with short PIN emits error message`() = runTest(testDispatcher) {
        viewModel.onEvent(WalletUiEvent.SetSecurityPin("12"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `onBiometricAuthSuccess updates wallet state`() = runTest(testDispatcher) {
        viewModel.onBiometricAuthSuccess()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.wallet.isBiometricsEnabled)
    }

    @Test
    fun `requestEscrowHold decreases available balance, increases escrow balance, preserves total balance`() = runTest(testDispatcher) {
        val initialAvailable = viewModel.uiState.value.wallet.availableBalance
        val initialEscrow = viewModel.uiState.value.wallet.escrowBalance
        val initialTotal = viewModel.uiState.value.totalBalance
        val holdAmount = 50.0

        viewModel.onEvent(WalletUiEvent.RequestEscrowHold(holdAmount, "trade-test-01"))
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertEquals(initialAvailable - holdAmount, updatedState.wallet.availableBalance)
        assertEquals(initialEscrow + holdAmount, updatedState.wallet.escrowBalance)
        assertEquals(initialTotal, updatedState.totalBalance)
        assertTrue(updatedState.isInvariantPreserved)
    }

    @Test
    fun `requestEscrowHold with insufficient funds sets error message`() = runTest(testDispatcher) {
        val hugeAmount = 999999.0
        viewModel.onEvent(WalletUiEvent.RequestEscrowHold(hugeAmount, "trade-test-02"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `requestEscrowRelease decreases escrow balance and updates ledger`() = runTest(testDispatcher) {
        // Hold funds first so escrow has a balance to release (RN-04)
        viewModel.onEvent(WalletUiEvent.RequestEscrowHold(50.0, "trade-test-setup"))
        advanceUntilIdle()

        val initialEscrow = viewModel.uiState.value.wallet.escrowBalance
        val releaseAmount = 50.0

        viewModel.onEvent(WalletUiEvent.RequestEscrowRelease(releaseAmount, "wallet-counterpart", "trade-test-03"))
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertEquals(initialEscrow - releaseAmount, updatedState.wallet.escrowBalance)
        assertTrue(updatedState.isInvariantPreserved)
    }

    @Test
    fun `signTriangularCycle completes cycle when all 3 participants accept`() = runTest(testDispatcher) {
        val effects = mutableListOf<WalletUiEffect>()
        val job = launch {
            viewModel.uiEffect.toList(effects)
        }

        // usr-vendor-b and usr-vendor-c are already accepted in sample cycle, signing as usr-demo completes 3/3
        val cycleId = viewModel.uiState.value.triangularCycle!!.cycleId
        viewModel.onEvent(WalletUiEvent.SignTriangularCycle(cycleId, "usr-demo"))
        advanceUntilIdle()

        val updatedCycle = viewModel.uiState.value.triangularCycle!!
        assertEquals(3, updatedCycle.signaturesCount)
        assertEquals(TriangularCycleStatus.BLOQUEADO_ESCROW, updatedCycle.status)
        assertTrue(effects.any { it is WalletUiEffect.TriangularCycleCompleted })

        job.cancel()
    }

    @Test
    fun `abortTriangularCycle aborts cycle without penalty per RN-06`() = runTest(testDispatcher) {
        val cycleId = viewModel.uiState.value.triangularCycle!!.cycleId
        viewModel.onEvent(WalletUiEvent.AbortTriangularCycle(cycleId, "usr-demo"))
        advanceUntilIdle()

        val updatedCycle = viewModel.uiState.value.triangularCycle!!
        assertEquals(TriangularCycleStatus.ABORTADO, updatedCycle.status)
        assertNotNull(viewModel.uiState.value.successMessage)
    }
}

