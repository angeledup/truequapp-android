package com.truequapp.core.database.entity

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "'EMAIL'")
    val authProvider: String = "EMAIL", // "GOOGLE", "EMAIL", "PHONE"
    val photoUrl: String? = null,
    @ColumnInfo(defaultValue = "0")
    val isKycVerified: Boolean = false,
    @ColumnInfo(defaultValue = "'BASICO_NIVEL_0'")
    val kycLevel: String = "BASICO_NIVEL_0", // "BASICO_NIVEL_0", "TELEFONO_SMS_NIVEL_1", "INE_BIOMETRICO_NIVEL_2"
    @ColumnInfo(defaultValue = "10000.0")
    val truequeCoins: Double = 10000.0, // Cantidad de moneda virtual única canónica para pruebas beta
    @ColumnInfo(defaultValue = "0.0")
    val escrowCoins: Double = 0.0,    // Saldo retenido en garantía
    @ColumnInfo(defaultValue = "3.0")
    val bayesianRating: Double = 3.0, // Calificación reputacional (RN-09)
    @ColumnInfo(defaultValue = "0")
    val ratingsCount: Int = 0,
    @ColumnInfo(defaultValue = "1.0")
    val attendanceRate: Double = 1.0,
    @ColumnInfo(defaultValue = "'Centro, Coatzacoalcos'")
    val obfuscatedNeighborhood: String = "Centro, Coatzacoalcos", // Privacidad 500m (RN-10)
    @ColumnInfo(defaultValue = "'Coatzacoalcos'")
    val city: String = "Coatzacoalcos",
    @ColumnInfo(defaultValue = "0")
    val completedTrades: Int = 0, // Contador de trueques completados
    @ColumnInfo(defaultValue = "0")
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val lastLoginEpochMs: Long = System.currentTimeMillis()
) {
    // Propiedades de compatibilidad con código de Phase 2
    val usuario: String get() = name
    val correo: String get() = email
    val numeroTelefonico: String get() = phoneNumber.orEmpty()
    val calificacion: Double get() = bayesianRating
    val cantidadTruequeCoins: Double get() = truequeCoins
    val truequesRealizados: Int get() = completedTrades
}
