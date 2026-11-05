package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.LedgerConcept
import com.truequapp.core.domain.model.LedgerEntry
import com.truequapp.core.domain.model.LedgerEntryType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RN-04: Conservación de Suma Cero en Ledger y Partida Doble")
class LedgerDoubleEntryValidatorUseCaseTest {

    private lateinit var useCase: LedgerDoubleEntryValidatorUseCase

    @BeforeEach
    fun setUp() {
        useCase = LedgerDoubleEntryValidatorUseCase()
    }

    @Test
    @DisplayName("Debe construir una transacción balanceada de partida doble con suma cero")
    fun shouldCreateBalancedDoubleEntryTransaction() {
        val transaction = useCase.createDoubleEntry(
            sourceWalletId = "wallet_alice",
            destinationWalletId = "wallet_bob",
            amount = 150.0,
            concept = LedgerConcept.LIBERACION_ESCROW_ENTREGA,
            tradeId = "trade_99"
        )

        assertEquals(150.0, transaction.debitEntry.amount)
        assertEquals(150.0, transaction.creditEntry.amount)
        assertEquals(LedgerEntryType.DEBITO, transaction.debitEntry.type)
        assertEquals(LedgerEntryType.CREDITO, transaction.creditEntry.type)
        assertEquals("wallet_alice", transaction.debitEntry.walletId)
        assertEquals("wallet_bob", transaction.creditEntry.walletId)
    }

    @Test
    @DisplayName("Debe validar exitosamente cuando los débitos igualan a los créditos")
    fun shouldPassValidation_whenDebitsEqualCredits() {
        val entries = listOf(
            LedgerEntry("1", "w1", null, LedgerEntryType.DEBITO, 100.0, LedgerConcept.RETENCION_EN_ESCROW_TRUEQUE, "w2"),
            LedgerEntry("2", "w2", null, LedgerEntryType.CREDITO, 100.0, LedgerConcept.RETENCION_EN_ESCROW_TRUEQUE, "w1")
        )

        useCase.validateZeroSumImbalance(entries)
    }

    @Test
    @DisplayName("Debe lanzar LedgerImbalanceException si la suma de débitos y créditos no coincide")
    fun shouldThrowException_whenDebitsAndCreditsDoNotMatch() {
        val imbalancedEntries = listOf(
            LedgerEntry("1", "w1", null, LedgerEntryType.DEBITO, 100.0, LedgerConcept.RETENCION_EN_ESCROW_TRUEQUE, "w2"),
            LedgerEntry("2", "w2", null, LedgerEntryType.CREDITO, 80.0, LedgerConcept.RETENCION_EN_ESCROW_TRUEQUE, "w1")
        )

        val ex = assertThrows(BusinessRuleException.LedgerImbalanceException::class.java) {
            useCase.validateZeroSumImbalance(imbalancedEntries)
        }

        assertEquals("RN-04", ex.ruleCode)
    }
}
