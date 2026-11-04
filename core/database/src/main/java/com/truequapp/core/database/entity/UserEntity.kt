package com.truequapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla SQL de usuarios registrados en Truequapp.
 * Contiene exactamente las 7 columnas requeridas por las especificaciones del negocio:
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
)

