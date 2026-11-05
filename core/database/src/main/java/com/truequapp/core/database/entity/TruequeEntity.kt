package com.truequapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla SQL que representa el Contrato Consolidado Central (Trueque) entre dos usuarios.
 * Consolida la información de User A, Oferta A, User B y Oferta B.
 *
 * Contiene la variable solicitada 'esta_activa':
 * - true (1): Cuando la propuesta está en curso o pendiente (PENDIENTE, CONTRAOFERTADA, RESERVADO, EN_ENTREGA).
 * - false (0): Cuando la propuesta está concluida, cancelada o rechazada (COMPLETADO, CANCELADO, RECHAZADO).
 */
@Entity(
    tableName = "trueques",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["correo"],
            childColumns = ["usuario_a_correo"]
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["correo"],
            childColumns = ["usuario_b_correo"]
        ),
        ForeignKey(
            entity = OfertaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["oferta_a_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = OfertaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["oferta_b_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("usuario_a_correo"),
        Index("usuario_b_correo"),
        Index("oferta_a_id"),
        Index("oferta_b_id"),
        Index("esta_activa")
    ]
)
data class TruequeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "usuario_a_correo")
    val usuarioACorreo: String,

    @ColumnInfo(name = "oferta_a_id")
    val ofertaAId: String,

    @ColumnInfo(name = "usuario_b_correo")
    val usuarioBCorreo: String,

    @ColumnInfo(name = "oferta_b_id")
    val ofertaBId: String,

    @ColumnInfo(name = "estado")
    val estado: String = "PENDIENTE", // PENDIENTE, CONTRAOFERTADA, RESERVADO, EN_ENTREGA, COMPLETADO, CANCELADO

    @ColumnInfo(name = "esta_activa")
    val estaActiva: Boolean = true, // 1 = True (Activa), 0 = False (Inactiva / Concluida)

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "fecha_actualizacion")
    val fechaActualizacion: Long = System.currentTimeMillis()
)

