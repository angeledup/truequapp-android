package com.truequapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla SQLite del libro mayor contable de partida doble (RN-04).
 * Registra débitos y créditos inmutables de TruequeCoins y depósitos en custodia Escrow.
 */
@Entity(
    tableName = "ledger_entries",
    indices = [
        Index(value = ["walletId"]),
        Index(value = ["tradeId"])
    ]
)
data class LedgerEntryEntity(
    @PrimaryKey
    val id: String,
    val walletId: String,
    val tradeId: String? = null,
    val entryType: String, // "DEBITO", "CREDITO"
    val amount: Double,
    val concept: String,   // "RETENCION_EN_ESCROW_TRUEQUE", "LIBERACION_ESCROW_ENTREGA", "RECARGA_TRUEQUECOINS", "REVERSO_CANCELACION"
    val counterpartWalletId: String? = null,
    val timestampEpochMs: Long = System.currentTimeMillis()
)
