package com.truequapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla SQLite de propuestas y negociaciones de trueque (directas y triangulares).
 */
@Entity(
    tableName = "trade_proposals",
    indices = [
        Index(value = ["initiatorUserId"]),
        Index(value = ["receiverUserId"]),
        Index(value = ["targetItemId"]),
        Index(value = ["status"])
    ]
)
data class TradeProposalEntity(
    @PrimaryKey
    val id: String,
    val initiatorUserId: String,
    val receiverUserId: String,
    val targetItemId: String,
    val offeredItemIdsJson: String = "[]",
    val monetaryCompensationCoins: Double = 0.0,
    val status: String = "PROPUESTA_ENVIADA", // "PROPUESTA_ENVIADA", "NEGOCIACION_ACTIVA", "CITA_PROGRAMADA", "COMPLETADO", "CANCELADO", "DISPUTA"
    val safeZoneId: String? = null,
    val safeZoneName: String? = null,
    val scheduledMeetingEpochMs: Long? = null,
    val qrSecurityToken: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
