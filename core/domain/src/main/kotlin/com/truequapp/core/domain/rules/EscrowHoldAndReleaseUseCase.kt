package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.WalletAccount

/**
 * Resultado de una operación de custodia en garantía (Escrow).
 */
data class EscrowHoldResult(
    val updatedBuyerWallet: WalletAccount,
    val heldAmount: Double
)

data class EscrowReleaseResult(
    val updatedBuyerWallet: WalletAccount,
    val updatedSellerWallet: WalletAccount,
    val releasedAmount: Double
)

/**
 * Caso de uso: Custodia Obligatoria y Liquidación en Escrow (RN-05 / CU015).
 * Elimina el riesgo de fraude en intercambios mediados por TruequeCoins.
 */
class EscrowHoldAndReleaseUseCase {

    /**
     * Retiene créditos preventivamente en Escrow al aceptar una propuesta.
     */
    fun holdInEscrow(buyerWallet: WalletAccount, amount: Double): EscrowHoldResult {
        if (amount <= 0.0) {
            return EscrowHoldResult(buyerWallet, 0.0)
        }

        if (!buyerWallet.hasSufficientFunds(amount)) {
            throw BusinessRuleException.EscrowViolationException(
                "Saldo insuficiente para retención en Escrow. Disponible: ${buyerWallet.availableBalance}, Requerido: $amount."
            )
        }

        val updatedWallet = buyerWallet.copy(
            availableBalance = buyerWallet.availableBalance - amount,
            escrowBalance = buyerWallet.escrowBalance + amount,
            versionLock = buyerWallet.versionLock + 1
        )

        return EscrowHoldResult(
            updatedBuyerWallet = updatedWallet,
            heldAmount = amount
        )
    }

    /**
     * Libera los créditos retenidos en Escrow hacia el vendedor tras confirmarse la entrega (CU020).
     */
    fun releaseToSeller(
        buyerWallet: WalletAccount,
        sellerWallet: WalletAccount,
        amount: Double
    ): EscrowReleaseResult {
        require(amount > 0.0) { "El monto a liberar debe ser mayor a cero." }

        if (buyerWallet.escrowBalance < amount) {
            throw BusinessRuleException.EscrowViolationException(
                "El saldo en garantía de la billetera compradora (${buyerWallet.escrowBalance}) es menor al monto solicitado ($amount)."
            )
        }

        val updatedBuyer = buyerWallet.copy(
            escrowBalance = buyerWallet.escrowBalance - amount,
            versionLock = buyerWallet.versionLock + 1
        )

        val updatedSeller = sellerWallet.copy(
            availableBalance = sellerWallet.availableBalance + amount,
            versionLock = sellerWallet.versionLock + 1
        )

        return EscrowReleaseResult(
            updatedBuyerWallet = updatedBuyer,
            updatedSellerWallet = updatedSeller,
            releasedAmount = amount
        )
    }

    /**
     * Reembolsa íntegramente los fondos desde el Escrow al saldo disponible del comprador ante cancelación legítima.
     */
    fun refundToBuyer(buyerWallet: WalletAccount, amount: Double): WalletAccount {
        require(amount > 0.0) { "El monto a reembolsar debe ser positivo." }

        if (buyerWallet.escrowBalance < amount) {
            throw BusinessRuleException.EscrowViolationException(
                "No hay suficientes créditos en Escrow para reembolsar."
            )
        }

        return buyerWallet.copy(
            availableBalance = buyerWallet.availableBalance + amount,
            escrowBalance = buyerWallet.escrowBalance - amount,
            versionLock = buyerWallet.versionLock + 1
        )
    }
}
