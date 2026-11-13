package com.truequapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla SQL que representa el paquete de oferta emitido por un usuario (Oferta A u Oferta B en el diagrama).
 * Contiene los artículos puestos a disposición y cualquier compensación en TruequeCoins.
 */
@Entity(
    tableName = "ofertas_items",
    indices = [Index("usuario_correo")]
)
data class OfertaItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "usuario_correo")
    val usuarioCorreo: String,

    @ColumnInfo(name = "articulos_ids")
    val articulosIds: String, // Lista de IDs de artículos ofrecidos (ej. "item_1,item_2")

    @ColumnInfo(name = "trueque_coins")
    val truequeCoins: Double = 0.0,

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long = System.currentTimeMillis()
)

