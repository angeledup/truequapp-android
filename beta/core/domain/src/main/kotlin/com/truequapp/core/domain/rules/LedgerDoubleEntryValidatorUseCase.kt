package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.DoubleEntryTransaction
import com.truequapp.core.domain.model.LedgerConcept
import com.truequapp.core.domain.model.LedgerEntry
import com.truequapp.core.domain.model.LedgerEntryType
import java.util.UUID

/**
 * Caso de uso: Validación y Generación de Asientos Contables de Partida Doble (RN-04).
 * Garantiza integridad contable estricta de suma cero en el monedero TruequeCoins.
 */
class LedgerDoubleEntryValidatorUseCase {

    /**
     * Valida que una lista de asientos contables cumpla la ecuación de conservación de suma cero.
     *
     * @param entries Asientos a verificar.
     * @throws BusinessRuleException.LedgerImbalanceException si la suma de débitos difiere de la suma de créditos.
     */
    fun validateZeroSumImbalance(entries: List<LedgerEntry>) {
        val totalDebits = entries.filter { it.type == LedgerEntryType.DEBITO }.sumOf { it.amount }
        val totalCredits = entries.filter { it.type == LedgerEntryType.CREDITO }.sumOf { it.amount }

        val epsilon = 0.0001
        if (Math.abs(totalDebits - totalCredits) > epsilon) {
            throw BusinessRuleException.LedgerImbalanceException(
                debitTotal = totalDebits,
                creditTotal = totalCredits
            )
        }
    }

    /**
     * Construye una transacción contable canónica de partida doble entre dos billeteras.
     */
    fun createDoubleEntry(
        sourceWalletId: String,
        destinationWalletId: String,
        amount: Double,
        concept: LedgerConcept,
        tradeId: String? = null
    ): DoubleEntryTransaction {
        require(amount > 0.0) { "El importe a transferir debe ser mayor a cero." }
        require(sourceWalletId != destinationWalletId) {
            "La billetera de origen ($sourceWalletId) y destino ($destinationWalletId) no pueden ser idénticas."
        }

        val debit = LedgerEntry(
            id = UUID.randomUUID().toString(),
            walletId = sourceWalletId,
            tradeId = tradeId,
            type = LedgerEntryType.DEBITO,
            amount = amount,
            concept = concept,
            counterpartWalletId = destinationWalletId
        )

        val credit = LedgerEntry(
            id = UUID.randomUUID().toString(),
            walletId = destinationWalletId,
            tradeId = tradeId,
            type = LedgerEntryType.CREDITO,
            amount = amount,
            concept = concept,
            counterpartWalletId = sourceWalletId
        )

        return DoubleEntryTransaction(
            transactionId = UUID.randomUUID().toString(),
            debitEntry = debit,
            creditEntry = credit
        )
    }
}
