package com.truequapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla SQLite de artículos y publicaciones de trueque en el marketplace.
 */
@Entity(
    tableName = "items",
    indices = [
        Index(value = ["ownerId"]),
        Index(value = ["categoryId"]),
        Index(value = ["status"])
    ]
)
data class TradeItemEntity(
    @PrimaryKey
    val id: String,
    val ownerId: String,
    val ownerName: String,
    val title: String,
    val description: String,
    val categoryId: Int,
    val condition: String = "USADO", // "NUEVO", "COMO_NUEVO", "USADO", "REACONDICIONADO"
    val type: String = "BIEN_FISICO", // "BIEN_FISICO", "SERVICIO_DIGITAL"
    val estimatedValueMin: Double,
    val estimatedValueMax: Double,
    val imagesWebPJson: String = "[]", // Lista de URLs de imágenes WebP en formato JSON
    val tagsWishlistJson: String = "[]", // Lista de intereses/deseos en formato JSON
    val status: String = "DISPONIBLE", // "DISPONIBLE", "EN_NEGOCIACION", "INTERCAMBIADO", "PAUSADO"
    val isBookmarked: Boolean = false, // Marcado como favorito por el usuario
    val locationNeighborhood: String = "Coatzacoalcos, Ver.",
    val city: String = "Coatzacoalcos",
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
