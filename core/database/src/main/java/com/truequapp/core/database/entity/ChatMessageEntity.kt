package com.truequapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla SQL que almacena los mensajes del chat privado transaccional (CU018).
 * Los mensajes se persisten localmente en el dispositivo y quedan estrictamente vinculados
 * a una propuesta formal de trueque ('trueque_id') bajo la regla canónica RN-03.
 */
@Entity(
    tableName = "chat_mensajes",
    indices = [
        Index("trueque_id"),
        Index("emisor_correo"),
        Index("receptor_correo"),
        Index("fecha_envio_ms")
    ]
)
data class ChatMessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "trueque_id")
    val truequeId: String,

    @ColumnInfo(name = "emisor_correo")
    val emisorCorreo: String,

    @ColumnInfo(name = "receptor_correo")
    val receptorCorreo: String = "",

    @ColumnInfo(name = "contenido")
    val contenido: String,

    @ColumnInfo(name = "fecha_envio_ms")
    val fechaEnvioMs: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "esta_entregado")
    val estaEntregado: Boolean = true,

    @ColumnInfo(name = "esta_leido")
    val estaLeido: Boolean = false
) {
    // Propiedades de compatibilidad con código existente
    val proposalId: String get() = truequeId
    val senderId: String get() = emisorCorreo
    val senderName: String get() = emisorCorreo.substringBefore("@")
    val content: String get() = contenido
    val timestampEpochMs: Long get() = fechaEnvioMs
    val isAuditLog: Boolean get() = false
    val isRead: Boolean get() = estaLeido
}
