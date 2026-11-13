package com.truequapp.core.database

import com.truequapp.core.database.entity.OfertaItemEntity
import com.truequapp.core.database.entity.TruequeConOfertas
import com.truequapp.core.database.entity.TruequeEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TruequeEntityTest {

    @Test
    fun `trueque entity initializes with active boolean set to true by default`() {
        val ofertaA = OfertaItemEntity(
            id = "oferta_a_1",
            usuarioCorreo = "userA@correo.com",
            articulosIds = "item_1,item_2",
            truequeCoins = 25.0
        )
        val ofertaB = OfertaItemEntity(
            id = "oferta_b_1",
            usuarioCorreo = "userB@correo.com",
            articulosIds = "item_target",
            truequeCoins = 0.0
        )
        val trueque = TruequeEntity(
            id = "trueque_123",
            usuarioACorreo = "userA@correo.com",
            ofertaAId = ofertaA.id,
            usuarioBCorreo = "userB@correo.com",
            ofertaBId = ofertaB.id,
            estado = "PENDIENTE",
            estaActiva = true
        )

        assertEquals("trueque_123", trueque.id)
        assertEquals("userA@correo.com", trueque.usuarioACorreo)
        assertEquals("oferta_a_1", trueque.ofertaAId)
        assertEquals("userB@correo.com", trueque.usuarioBCorreo)
        assertEquals("oferta_b_1", trueque.ofertaBId)
        assertEquals("PENDIENTE", trueque.estado)
        assertTrue(trueque.estaActiva, "La propuesta recién creada debe tener estaActiva = true")

        val truequeConOfertas = TruequeConOfertas(
            trueque = trueque,
            ofertaA = ofertaA,
            ofertaB = ofertaB
        )
        assertTrue(truequeConOfertas.estaActiva)
        assertEquals(25.0, truequeConOfertas.ofertaA?.truequeCoins)
    }

    @Test
    fun `trueque marks active as false when finished or cancelled`() {
        val truequeCancelado = TruequeEntity(
            id = "trueque_456",
            usuarioACorreo = "userA@correo.com",
            ofertaAId = "oferta_a",
            usuarioBCorreo = "userB@correo.com",
            ofertaBId = "oferta_b",
            estado = "CANCELADO",
            estaActiva = false
        )

        assertFalse(truequeCancelado.estaActiva, "Un trueque cancelado debe tener estaActiva = false")

        val truequeCompletado = TruequeEntity(
            id = "trueque_789",
            usuarioACorreo = "userA@correo.com",
            ofertaAId = "oferta_a",
            usuarioBCorreo = "userB@correo.com",
            ofertaBId = "oferta_b",
            estado = "COMPLETADO",
            estaActiva = false
        )

        assertFalse(truequeCompletado.estaActiva, "Un trueque completado debe tener estaActiva = false")
    }
}

