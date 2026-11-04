package com.truequapp.core.database

import com.truequapp.core.database.entity.UserEntity
import org.junit.jupiter.api.Assertions.assertEquals
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
}

