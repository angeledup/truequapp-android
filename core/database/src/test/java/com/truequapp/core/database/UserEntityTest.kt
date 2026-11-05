package com.truequapp.core.database

import com.truequapp.core.database.entity.UserEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UserEntityTest {

    @Test
    fun `user entity initializes with all seven required columns and business defaults`() {
        val user = UserEntity(
            correo = "test@truequapp.com",
            contrasena = "securePass123",
            usuario = "truequero_master",
            numeroTelefonico = "+525512345678"
        )

        assertEquals("test@truequapp.com", user.correo)
        assertEquals("securePass123", user.contrasena)
        assertEquals("truequero_master", user.usuario)
        assertEquals("+525512345678", user.numeroTelefonico)
        assertEquals(100.0, user.cantidadTruequeCoins)
        assertEquals(0, user.truequesRealizados)
        assertEquals(5.0, user.calificacion)
    }

    @Test
    @DisplayName("Admin user entity initializes with specified custom attributes")
    fun `admin user entity initializes with specified custom attributes`() {
        val admin = UserEntity(
            correo = "angeleduvr@gmail.com",
            contrasena = "123456",
            usuario = "Admin_AEVR",
            numeroTelefonico = "+529212404565",
            cantidadTruequeCoins = 100000000.0,
            truequesRealizados = 10,
            calificacion = 10.0
        )

        assertEquals("angeleduvr@gmail.com", admin.correo)
        assertEquals("123456", admin.contrasena)
        assertEquals("Admin_AEVR", admin.usuario)
        assertEquals("+529212404565", admin.numeroTelefonico)
        assertEquals(100000000.0, admin.cantidadTruequeCoins)
        assertEquals(10, admin.truequesRealizados)
        assertEquals(10.0, admin.calificacion)
    }
}
