package com.truequapp.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla SQLite de usuarios registrados en Truequapp.
 * Almacena credenciales de acceso, proveedor de identidad (Google/Email/Teléfono),
 * estado de verificación KYC, reputación bayesiana y saldo canónico de TruequeCoins.
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["phoneNumber"])
    ]
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val name: String,
    val passwordHash: String? = null, // Contraseña (si fue inicio de sesión tradicional con email)
    val phoneNumber: String? = null,  // Número telefónico (si fue autenticación vía SMS OTP)
    val authProvider: String = "EMAIL", // "GOOGLE", "EMAIL", "PHONE"
    val photoUrl: String? = null,
    val isKycVerified: Boolean = false,
    val kycLevel: String = "BASICO_NIVEL_0", // "BASICO_NIVEL_0", "TELEFONO_SMS_NIVEL_1", "INE_BIOMETRICO_NIVEL_2"
    val truequeCoins: Double = 100.0, // Cantidad de moneda virtual única canónica
    val escrowCoins: Double = 0.0,    // Saldo retenido en garantía
    val bayesianRating: Double = 3.0, // Calificación reputacional (RN-09)
    val ratingsCount: Int = 0,
    val attendanceRate: Double = 1.0,
    val obfuscatedNeighborhood: String = "Centro, Coatzacoalcos", // Privacidad 500m (RN-10)
    val city: String = "Coatzacoalcos",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val lastLoginEpochMs: Long = System.currentTimeMillis()
)
