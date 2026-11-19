package com.truequapp.feature.wallet.mvi

import androidx.lifecycle.viewModelScope
import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.database.dao.LedgerEntryDao
import com.truequapp.core.database.entity.LedgerEntryEntity
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.domain.model.*
import com.truequapp.core.domain.rules.BusinessRuleException
import com.truequapp.core.domain.rules.EscrowHoldAndReleaseUseCase
import com.truequapp.core.domain.rules.LedgerDoubleEntryValidatorUseCase
import com.truequapp.core.domain.rules.TriangularCycleRulesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel del Monedero Digital TruequeCoins, Custodia en Escrow y Algoritmo de Grafos Triangulares
 * (RN-04, RN-05, RN-06 / CU013, CU014, CU015, CU016).
 */
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val escrowUseCase: EscrowHoldAndReleaseUseCase = EscrowHoldAndReleaseUseCase(),
    private val ledgerUseCase: LedgerDoubleEntryValidatorUseCase = LedgerDoubleEntryValidatorUseCase(),
    private val triangularUseCase: TriangularCycleRulesUseCase = TriangularCycleRulesUseCase(),
    private val userSessionDataStore: UserSessionDataStore? = null,
    private val ledgerEntryDao: LedgerEntryDao? = null,
    private val dispatchers: DispatcherProvider
) : BaseViewModel<WalletUiState, WalletUiEvent, WalletUiEffect>(WalletUiState()) {

    init {
        observeSession()
        initializeSampleLedgerAndCycle()
    }

    private fun observeSession() {
        if (userSessionDataStore == null) return
        viewModelScope.launch(dispatchers.io) {
            userSessionDataStore.sessionFlow.collect { session ->
                setState {
                    copy(
                        wallet = wallet.copy(
                            userId = session.userId ?: wallet.userId,
                            availableBalance = session.truequeCoins,
                            escrowBalance = session.escrowCoins
                        )
                    )
                }
            }
        }
    }

    override fun onEvent(event: WalletUiEvent) {
        when (event) {
            is WalletUiEvent.LoadWallet -> loadWalletForUser(event.userId)
            is WalletUiEvent.SetSecurityPin -> handleSetSecurityPin(event.pin)
            is WalletUiEvent.ToggleBiometrics -> handleToggleBiometrics(event.enabled)
            is WalletUiEvent.RequestEscrowHold -> handleEscrowHold(event.amount, event.tradeId)
            is WalletUiEvent.RequestEscrowRelease -> handleEscrowRelease(event.amount, event.sellerWalletId, event.tradeId)
            is WalletUiEvent.SignTriangularCycle -> handleSignTriangularCycle(event.cycleId, event.userId)
            is WalletUiEvent.AbortTriangularCycle -> handleAbortTriangularCycle(event.cycleId, event.userId)
            is WalletUiEvent.OpenTriangularDetails -> setState { copy(showTriangularDialog = true) }
            is WalletUiEvent.CloseTriangularDetails -> setState { copy(showTriangularDialog = false) }
            is WalletUiEvent.DismissMessages -> setState { copy(errorMessage = null, successMessage = null) }
        }
    }

    private fun loadWalletForUser(userId: String) {
        setState { copy(isLoading = true) }
        viewModelScope.launch(dispatchers.io) {
            val loadedWallet = currentState.wallet.copy(userId = userId)
            setState {
                copy(
                    wallet = loadedWallet,
                    isLoading = false
                )
            }
        }
    }

    private fun handleSetSecurityPin(pin: String) {
        if (pin.length < 4) {
            setState { copy(errorMessage = "El PIN de seguridad debe contener al menos 4 dígitos.") }
            return
        }
        val updatedWallet = currentState.wallet.copy(isPinConfigured = true)
        setState {
            copy(
                wallet = updatedWallet,
                successMessage = "PIN de seguridad configurado correctamente."
            )
        }
        sendEffect(SwipeUiEffectShow("PIN configurado"))
    }

    private fun handleToggleBiometrics(enabled: Boolean) {
        if (enabled) {
            sendEffect(WalletUiEffect.TriggerBiometricPrompt)
        } else {
            val updatedWallet = currentState.wallet.copy(isBiometricsEnabled = false)
            setState {
                copy(
                    wallet = updatedWallet,
                    successMessage = "Autenticación biométrica desactivada."
                )
            }
        }
    }

    fun onBiometricAuthSuccess() {
        val updatedWallet = currentState.wallet.copy(isBiometricsEnabled = true)
        setState {
            copy(
                wallet = updatedWallet,
                successMessage = "Biometría vinculada exitosamente para autorizar pagos."
            )
        }
        sendEffect(WalletUiEffect.ShowToast("Autenticación biométrica activada"))
    }

    private fun handleEscrowHold(amount: Double, tradeId: String) {
        try {
            val holdResult = escrowUseCase.holdInEscrow(currentState.wallet, amount)

            // Registro contable partida doble (RN-04)
            val debitEntry = LedgerEntry(
                id = UUID.randomUUID().toString(),
                walletId = currentState.wallet.id,
                tradeId = tradeId,
                type = LedgerEntryType.DEBITO,
                amount = amount,
                concept = LedgerConcept.RETENCION_EN_ESCROW_TRUEQUE,
                counterpartWalletId = "ESCROW_SYSTEM"
            )

            val updatedHistory = listOf(debitEntry) + currentState.ledgerHistory

            setState {
                copy(
                    wallet = holdResult.updatedBuyerWallet,
                    ledgerHistory = updatedHistory,
                    successMessage = "Se han retenido \$${amount.toInt()} TC en custodia segura (Escrow)."
                )
            }
            viewModelScope.launch(dispatchers.io) {
                userSessionDataStore?.updateTruequeCoins(
                    available = holdResult.updatedBuyerWallet.availableBalance,
                    escrow = holdResult.updatedBuyerWallet.escrowBalance
                )
                ledgerEntryDao?.insertEntry(
                    LedgerEntryEntity(
                        id = debitEntry.id,
                        walletId = debitEntry.walletId,
                        tradeId = debitEntry.tradeId,
                        entryType = "DEBITO",
                        amount = debitEntry.amount,
                        concept = debitEntry.concept.name,
                        counterpartWalletId = debitEntry.counterpartWalletId,
                        timestampEpochMs = System.currentTimeMillis()
                    )
                )
            }
            sendEffect(WalletUiEffect.ShowToast("Retención en Escrow completada"))
        } catch (e: BusinessRuleException.EscrowViolationException) {
            setState { copy(errorMessage = e.message) }
            sendEffect(WalletUiEffect.ShowToast(e.message ?: "Error al retener fondos en Escrow"))
        }
    }

    private fun handleEscrowRelease(amount: Double, sellerWalletId: String, tradeId: String) {
        try {
            val dummySellerWallet = WalletAccount(
                id = sellerWalletId,
                userId = "usr-counterpart",
                availableBalance = 500.0,
                escrowBalance = 0.0
            )

            val releaseResult = escrowUseCase.releaseToSeller(
                buyerWallet = currentState.wallet,
                sellerWallet = dummySellerWallet,
                amount = amount
            )

            // Asiento contable de partida doble
            val doubleEntry = ledgerUseCase.createDoubleEntry(
                sourceWalletId = currentState.wallet.id,
                destinationWalletId = sellerWalletId,
                amount = amount,
                concept = LedgerConcept.LIBERACION_ESCROW_ENTREGA,
                tradeId = tradeId
            )

            val updatedHistory = listOf(doubleEntry.debitEntry) + currentState.ledgerHistory

            setState {
                copy(
                    wallet = releaseResult.updatedBuyerWallet,
                    ledgerHistory = updatedHistory,
                    successMessage = "Se han liberado con éxito \$${amount.toInt()} TC hacia la contraparte."
                )
            }
            viewModelScope.launch(dispatchers.io) {
                userSessionDataStore?.updateTruequeCoins(
                    available = releaseResult.updatedBuyerWallet.availableBalance,
                    escrow = releaseResult.updatedBuyerWallet.escrowBalance
                )
                ledgerEntryDao?.insertEntry(
                    LedgerEntryEntity(
                        id = doubleEntry.debitEntry.id,
                        walletId = doubleEntry.debitEntry.walletId,
                        tradeId = doubleEntry.debitEntry.tradeId,
                        entryType = "DEBITO",
                        amount = doubleEntry.debitEntry.amount,
                        concept = doubleEntry.debitEntry.concept.name,
                        counterpartWalletId = doubleEntry.debitEntry.counterpartWalletId,
                        timestampEpochMs = System.currentTimeMillis()
                    )
                )
            }
            sendEffect(WalletUiEffect.ShowToast("TruequeCoins liberadas exitosamente"))
        } catch (e: BusinessRuleException.EscrowViolationException) {
            setState { copy(errorMessage = e.message) }
            sendEffect(WalletUiEffect.ShowToast(e.message ?: "Error al liberar fondos en Escrow"))
        }
    }

    private fun handleSignTriangularCycle(cycleId: String, userId: String) {
        val currentCycle = currentState.triangularCycle
        if (currentCycle == null || currentCycle.cycleId != cycleId) {
            setState { copy(errorMessage = "Ciclo triangular no encontrado.") }
            return
        }

        try {
            val updatedCycle = triangularUseCase.signCycle(currentCycle, userId)
            val isCompleted = updatedCycle.status == TriangularCycleStatus.BLOQUEADO_ESCROW

            setState {
                copy(
                    triangularCycle = updatedCycle,
                    successMessage = if (isCompleted) {
                        "¡Ciclo Triangular completado! 3 de 3 firmas registradas. Artículos reservados."
                    } else {
                        "Firma registrada. Esperando a los demás participantes (${updatedCycle.signaturesCount}/3)."
                    }
                )
            }

            if (isCompleted) {
                sendEffect(WalletUiEffect.TriangularCycleCompleted(cycleId))
            }
            sendEffect(WalletUiEffect.ShowToast("Firma registrada exitosamente"))
        } catch (e: BusinessRuleException.TriangularCycleAbortException) {
            setState { copy(errorMessage = e.message) }
        }
    }

    private fun handleAbortTriangularCycle(cycleId: String, userId: String) {
        val currentCycle = currentState.triangularCycle ?: return
        val abortedCycle = triangularUseCase.abortCycleWithoutPenalty(currentCycle, userId)

        setState {
            copy(
                triangularCycle = abortedCycle,
                showTriangularDialog = false,
                successMessage = "Has declinado la propuesta triangular sin penalización (RN-06)."
            )
        }
        sendEffect(WalletUiEffect.ShowToast("Propuesta declinada sin penalización"))
    }

    private fun initializeSampleLedgerAndCycle() {
        val initialLedger = listOf(
            LedgerEntry(
                id = "tx-seed-001",
                walletId = "wallet-demo-01",
                tradeId = null,
                type = LedgerEntryType.CREDITO,
                amount = 1000.0,
                concept = LedgerConcept.BONO_BIENVENIDA_SEMILLA,
                counterpartWalletId = "SISTEMA_CENTRAL",
                timestampEpochMs = System.currentTimeMillis() - 86400000L * 5
            ),
            LedgerEntry(
                id = "tx-escrow-002",
                walletId = "wallet-demo-01",
                tradeId = "trade-782",
                type = LedgerEntryType.DEBITO,
                amount = 350.0,
                concept = LedgerConcept.RETENCION_EN_ESCROW_TRUEQUE,
                counterpartWalletId = "ESCROW_SYSTEM",
                timestampEpochMs = System.currentTimeMillis() - 86400000L * 2
            ),
            LedgerEntry(
                id = "tx-bonus-003",
                walletId = "wallet-demo-01",
                tradeId = null,
                type = LedgerEntryType.CREDITO,
                amount = 850.0,
                concept = LedgerConcept.BONIFICACION_REPUTACION_ALTA,
                counterpartWalletId = "SISTEMA_CENTRAL",
                timestampEpochMs = System.currentTimeMillis() - 86400000L * 1
            )
        )

        val sampleTriangularCycle = TriangularCycle(
            cycleId = "cycle-tri-uv-001",
            participants = listOf(
                TriangularNodeParticipant(
                    userId = "usr-demo", // Tú
                    givenItemId = "item-demo-bike",
                    receivedItemId = "item-vendorB-laptop",
                    hasAccepted = false
                ),
                TriangularNodeParticipant(
                    userId = "usr-vendor-b",
                    givenItemId = "item-vendorB-laptop",
                    receivedItemId = "item-vendorC-camera",
                    hasAccepted = true,
                    signedAtEpochMs = System.currentTimeMillis() - 3600000L
                ),
                TriangularNodeParticipant(
                    userId = "usr-vendor-c",
                    givenItemId = "item-vendorC-camera",
                    receivedItemId = "item-demo-bike",
                    hasAccepted = true,
                    signedAtEpochMs = System.currentTimeMillis() - 1800000L
                )
            ),
            compatibilityScore = 0.92,
            status = TriangularCycleStatus.PROPUESTO
        )

        setState {
            copy(
                ledgerHistory = initialLedger,
                triangularCycle = sampleTriangularCycle
            )
        }
    }

    private fun SwipeUiEffectShow(msg: String): WalletUiEffect {
        return WalletUiEffect.ShowToast(msg)
    }
}

