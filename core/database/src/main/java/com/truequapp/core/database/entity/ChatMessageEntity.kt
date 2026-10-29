package com.truequapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla SQLite de mensajes de chat transaccional privado y registros de auditoría (RN-03).
 */
@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["proposalId"]),
        Index(value = ["senderId"])
    ]
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val proposalId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val isAuditLog: Boolean = false, // Registros automáticos de modificaciones de oferta (RN-03)
    val isRead: Boolean = false
)
