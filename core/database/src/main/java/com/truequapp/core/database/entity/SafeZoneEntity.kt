package com.truequapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla SQLite de Puntos Seguros de Intercambio georreferenciados (Safe Zones) en Coatzacoalcos.
 */
@Entity(tableName = "safe_zones")
data class SafeZoneEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val addressDescription: String,
    val latitude: Double,
    val longitude: Double,
    val openingHour: Int,
    val closingHour: Int,
    val hasVideoSurveillance: Boolean,
    val isCampusUniversityPartner: Boolean,
    val city: String = "Coatzacoalcos"
)

