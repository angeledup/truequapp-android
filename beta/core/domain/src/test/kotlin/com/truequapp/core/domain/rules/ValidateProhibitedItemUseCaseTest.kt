package com.truequapp.core.domain.rules

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RN-11: Prohibición de Artículos Ilícitos y Cumplimiento Google Play")
class ValidateProhibitedItemUseCaseTest {

    private lateinit var useCase: ValidateProhibitedItemUseCase

    @BeforeEach
    fun setUp() {
        useCase = ValidateProhibitedItemUseCase()
    }

    @Test
    @DisplayName("Debe permitir artículos lícitos estándar sin generar excepciones")
    fun shouldAllowLegitimateItems() {
        assertDoesNotThrow {
            useCase(
                title = "Bicicleta de montaña rodada 29",
                description = "En excelente estado, cambios Shimano recién ajustados",
                tags = listOf("deportes", "ciclismo", "transporte")
            )
        }
    }

    @Test
    @DisplayName("Debe bloquear y lanzar ProhibitedItemException si detecta armas en título o descripción")
    fun shouldBlockWeapons() {
        val ex = assertThrows(BusinessRuleException.ProhibitedItemException::class.java) {
            useCase(
                title = "Vendo pistola de aire comprimido",
                description = "Incluye municiones de regalo",
                tags = listOf("tiro", "caza")
            )
        }

        assertEquals("RN-11", ex.ruleCode)
        assertEquals("pistola", ex.matchedKeyword)
    }

    @Test
    @DisplayName("Debe bloquear medicamentos controlados o sustancias prohibidas")
    fun shouldBlockControlledSubstances() {
        val ex = assertThrows(BusinessRuleException.ProhibitedItemException::class.java) {
            useCase(
                title = "Caja sellada de clonazepam",
                description = "No la ocupo, cambio por despensa",
                tags = listOf("salud", "farmacia")
            )
        }

        assertEquals("RN-11", ex.ruleCode)
        assertEquals("clonazepam", ex.matchedKeyword)
    }
}
