package com.truequapp.core.domain.model

/**
 * Tipo de asiento contable para garantizar partida doble estricta (RN-04).
 */
enum class LedgerEntryType {
    DEBITO,  // Cargo a la cuenta (disminuye disponibilidad o sale de cuenta emisora)
    CREDITO  // Abono a la cuenta (aumenta disponibilidad o entra a cuenta receptora)
}

/**
 * Conceptos contables admitidos en el ecosistema TruequeCoins.
 */
enum class LedgerConcept {
    BONO_BIENVENIDA_SEMILLA,       // Otorgado a usuarios pioneros (Fase 5)
    RETENCION_EN_ESCROW_TRUEQUE,   // Fondos congelados preventivamente por oferta aceptada
    LIBERACION_ESCROW_ENTREGA,     // Transferencia al vendedor tras escanear código QR
    REEMBOLSO_ESCROW_CANCELACION,  // Retorno al comprador por cancelación legítima
    RESOLUCION_DISPUTA_ARBITRAJE,  // Veredicto emitido por el moderador
    BONIFICACION_REPUTACION_ALTA   // Incentivo mensual por tasa de asistencia intachable
}

/**
 * Asiento contable individual e inmutable en el libro mayor de Truequapp.
 */
data class LedgerEntry(
    val id: String,
    val walletId: String,
    val tradeId: String?,
    val type: LedgerEntryType,
    val amount: Double,
    val concept: LedgerConcept,
    val counterpartWalletId: String?,
    val timestampEpochMs: Long = System.currentTimeMillis()
) {
    init {
        require(amount > 0.0) { "El monto del asiento contable debe ser estrictamente positivo." }
    }
}

/**
 * Transacción de partida doble que vincula dos asientos complementarios de suma cero.
 */
data class DoubleEntryTransaction(
    val transactionId: String,
    val debitEntry: LedgerEntry,
    val creditEntry: LedgerEntry
) {
    init {
        require(debitEntry.type == LedgerEntryType.DEBITO) { "El asiento emisor debe ser de tipo DÉBITO." }
        require(creditEntry.type == LedgerEntryType.CREDITO) { "El asiento receptor debe ser de tipo CRÉDITO." }
        require(debitEntry.amount == creditEntry.amount) {
            "Violación de invariante RN-04: El monto debitado (${debitEntry.amount}) debe ser exactamente igual al monto acreditado (${creditEntry.amount})."
        }
    }
}

/**
 * Estado financiero del monedero del usuario.
 */
data class WalletAccount(
    val id: String,
    val userId: String,
    val availableBalance: Double = 0.0,
    val escrowBalance: Double = 0.0,
    val isPinConfigured: Boolean = false,
    val isBiometricsEnabled: Boolean = false,
    val versionLock: Long = 1L // Optimistic Locking para concurrencia ACID
) {
    val totalBalance: Double
        get() = availableBalance + escrowBalance

    fun hasSufficientFunds(amount: Double): Boolean {
        return availableBalance >= amount
    }
}
