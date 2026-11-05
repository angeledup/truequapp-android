package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.WalletAccount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RN-05: Custodia Obligatoria en Escrow y Liquidación Atómica")
class EscrowHoldAndReleaseUseCaseTest {

    private lateinit var useCase: EscrowHoldAndReleaseUseCase

    @BeforeEach
    fun setUp() {
        useCase = EscrowHoldAndReleaseUseCase()
    }

    private fun createWallet(id: String, available: Double, escrow: Double = 0.0): WalletAccount {
        return WalletAccount(
            id = id,
            userId = "user_$id",
            availableBalance = available,
            escrowBalance = escrow
        )
    }

    @Test
    @DisplayName("Debe retener fondos exitosamente del saldo disponible al saldo Escrow")
    fun shouldHoldFundsInEscrow_whenSufficientFunds() {
        val buyerWallet = createWallet("buyer_1", available = 500.0, escrow = 0.0)

        val result = useCase.holdInEscrow(buyerWallet, amount = 200.0)

        assertEquals(300.0, result.updatedBuyerWallet.availableBalance)
        assertEquals(200.0, result.updatedBuyerWallet.escrowBalance)
        assertEquals(200.0, result.heldAmount)
    }

    @Test
    @DisplayName("Debe lanzar EscrowViolationException si no hay fondos suficientes para retener")
    fun shouldThrowException_whenInsufficientFundsForEscrow() {
        val buyerWallet = createWallet("buyer_1", available = 50.0, escrow = 0.0)

        val ex = assertThrows(BusinessRuleException.EscrowViolationException::class.java) {
            useCase.holdInEscrow(buyerWallet, amount = 100.0)
        }

        assertEquals("RN-05", ex.ruleCode)
    }

    @Test
    @DisplayName("Debe liberar fondos desde el Escrow del comprador al disponible del vendedor")
    fun shouldReleaseFundsToSeller_afterConfirmation() {
        val buyerWallet = createWallet("buyer_1", available = 100.0, escrow = 150.0)
        val sellerWallet = createWallet("seller_1", available = 50.0, escrow = 0.0)

        val result = useCase.releaseToSeller(buyerWallet, sellerWallet, amount = 150.0)

        assertEquals(0.0, result.updatedBuyerWallet.escrowBalance)
        assertEquals(100.0, result.updatedBuyerWallet.availableBalance)
        assertEquals(200.0, result.updatedSellerWallet.availableBalance)
        assertEquals(150.0, result.releasedAmount)
    }

    @Test
    @DisplayName("Debe reembolsar íntegramente al comprador ante cancelación legítima")
    fun shouldRefundToBuyer_uponLegitimateCancellation() {
        val buyerWallet = createWallet("buyer_1", available = 300.0, escrow = 200.0)

        val refundedWallet = useCase.refundToBuyer(buyerWallet, amount = 200.0)

        assertEquals(500.0, refundedWallet.availableBalance)
        assertEquals(0.0, refundedWallet.escrowBalance)
    }
}
