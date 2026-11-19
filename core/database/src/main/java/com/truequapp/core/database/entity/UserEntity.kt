package com.truequapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla SQL de usuarios registrados en Truequapp (Fase 2 / Módulo Usuarios).
 * Contiene exactamente las 7 columnas requeridas por las especificaciones de negocio:
 * 1- Correo
 * 2- Contraseña
 * 3- Usuario
 * 4- Número telefónico
 * 5- Cantidad de trueque_coins
 * 6- Trueques realizados
 * 7- Calificación
 */
@Entity(tableName = "usuarios")
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "correo")
    val correo: String,

    @ColumnInfo(name = "contrasena")
    val contrasena: String,

    @ColumnInfo(name = "usuario")
    val usuario: String,

    @ColumnInfo(name = "numero_telefonico")
    val numeroTelefonico: String,

    @ColumnInfo(name = "cantidad_trueque_coins")
    val cantidadTruequeCoins: Double = 100.0,

    @ColumnInfo(name = "trueques_realizados")
    val truequesRealizados: Int = 0,

    @ColumnInfo(name = "calificacion")
    val calificacion: Double = 5.0
) {
    @androidx.room.Ignore
    var customId: String? = null

    @androidx.room.Ignore
    var customAuthProvider: String? = null

    @androidx.room.Ignore
    var customEscrowCoins: Double? = null

    @androidx.room.Ignore
    var customIsKycVerified: Boolean? = null

    @androidx.room.Ignore
    var customNeighborhood: String? = null

    @androidx.room.Ignore
    var customPhotoUrl: String? = null

    @androidx.room.Ignore
    var customCity: String? = null

    // Propiedades canónicas y de compatibilidad para capas de negocio y UI
    val id: String get() = customId ?: usuario.ifBlank { correo }
    val email: String get() = correo
    val name: String get() = usuario
    val passwordHash: String? get() = contrasena.ifBlank { null }
    val phoneNumber: String get() = numeroTelefonico
    val truequeCoins: Double get() = cantidadTruequeCoins
    val escrowCoins: Double get() = customEscrowCoins ?: 0.0
    val completedTrades: Int get() = truequesRealizados
    val bayesianRating: Double get() = calificacion
    val ratingsCount: Int get() = 10
    val attendanceRate: Double get() = 1.0
    val isKycVerified: Boolean get() = customIsKycVerified ?: true
    val kycLevel: String get() = "TELEFONO_SMS_NIVEL_1"
    val authProvider: String get() = customAuthProvider ?: "EMAIL"
    val photoUrl: String? get() = customPhotoUrl
    val obfuscatedNeighborhood: String get() = customNeighborhood ?: "Centro, Coatzacoalcos"
    val city: String get() = customCity ?: "Coatzacoalcos"
    val createdAtEpochMs: Long get() = System.currentTimeMillis()
    val lastLoginEpochMs: Long get() = System.currentTimeMillis()

    fun copy(
        id: String = this.id,
        email: String = this.email,
        name: String = this.name,
        passwordHash: String? = this.passwordHash,
        phoneNumber: String = this.phoneNumber,
        truequeCoins: Double = this.truequeCoins,
        escrowCoins: Double = this.escrowCoins,
        bayesianRating: Double = this.bayesianRating,
        completedTrades: Int = this.completedTrades,
        photoUrl: String? = this.photoUrl,
        authProvider: String = this.authProvider,
        lastLoginEpochMs: Long = this.lastLoginEpochMs,
        obfuscatedNeighborhood: String = this.obfuscatedNeighborhood
    ): UserEntity {
        val user = UserEntity(
            correo = email,
            contrasena = passwordHash.orEmpty(),
            usuario = name,
            numeroTelefonico = phoneNumber,
            cantidadTruequeCoins = truequeCoins,
            truequesRealizados = completedTrades,
            calificacion = bayesianRating
        )
        user.customId = id
        user.customAuthProvider = authProvider
        user.customEscrowCoins = escrowCoins
        user.customNeighborhood = obfuscatedNeighborhood
        user.customPhotoUrl = photoUrl
        return user
    }

    companion object {
        operator fun invoke(
            id: String = "",
            email: String = "",
            name: String = "",
            passwordHash: String? = null,
            phoneNumber: String? = null,
            authProvider: String = "EMAIL",
            photoUrl: String? = null,
            isKycVerified: Boolean = false,
            kycLevel: String = "BASICO_NIVEL_0",
            truequeCoins: Double = 10000.0,
            escrowCoins: Double = 0.0,
            bayesianRating: Double = 3.0,
            ratingsCount: Int = 0,
            attendanceRate: Double = 1.0,
            obfuscatedNeighborhood: String = "Centro, Coatzacoalcos",
            city: String = "Coatzacoalcos",
            completedTrades: Int = 0,
            createdAtEpochMs: Long = System.currentTimeMillis(),
            lastLoginEpochMs: Long = System.currentTimeMillis()
        ): UserEntity {
            val user = UserEntity(
                correo = email,
                contrasena = passwordHash.orEmpty(),
                usuario = name.ifBlank { id },
                numeroTelefonico = phoneNumber.orEmpty(),
                cantidadTruequeCoins = truequeCoins,
                truequesRealizados = completedTrades,
                calificacion = bayesianRating
            )
            if (id.isNotBlank()) user.customId = id
            user.customAuthProvider = authProvider
            user.customEscrowCoins = escrowCoins
            user.customIsKycVerified = isKycVerified
            user.customNeighborhood = obfuscatedNeighborhood
            user.customPhotoUrl = photoUrl
            user.customCity = city
            return user
        }
    }
}
