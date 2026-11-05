package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.EstimatedValueRange
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.core.domain.model.ItemStatus
import com.truequapp.core.domain.model.ItemType
import com.truequapp.core.domain.model.TradeItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RN-02: Validación de la Banda Mínima de Equivalencia de Valor (40%)")
class ValidateTradeValueUseCaseTest {

    private lateinit var useCase: ValidateTradeValueUseCase

    @BeforeEach
    fun setUp() {
        useCase = ValidateTradeValueUseCase()
    }

    private fun createTestItem(id: String, minVal: Double, maxVal: Double): TradeItem {
        return TradeItem(
            id = id,
            ownerId = "user_$id",
            title = "Artículo de prueba $id",
            description = "Descripción de prueba",
            categoryId = 1,
            type = ItemType.BIEN_FISICO,
            condition = ItemCondition.USADO,
            estimatedValue = EstimatedValueRange(minVal, maxVal),
            imagesWebP = emptyList(),
            tagsWishlist = emptyList(),
            status = ItemStatus.DISPONIBLE
        )
    }

    @Test
    @DisplayName("Debe aceptar una oferta que represente exactamente el 40% del valor objetivo")
    fun shouldAcceptOffer_atExactFortyPercentThreshold() {
        // Target: mediana = 1,000
        val target = createTestItem("target", 800.0, 1200.0) // mediana = 1000.0
        // Offered: mediana = 400.0 (40% de 1000)
        val offered = listOf(createTestItem("offered_1", 350.0, 450.0))

        val ratio = useCase(targetItem = target, offeredItems = offered, compensatoryCoins = 0.0)

        assertEquals(0.40, ratio, 0.001)
    }

    @Test
    @DisplayName("Debe aceptar una oferta mayor al 40% en trueque N:1")
    fun shouldAcceptOffer_whenMultipleItemsExceedThreshold() {
        // Target: mediana = 2,000
        val target = createTestItem("target", 1500.0, 2500.0)
        // Offered N:1: 600 + 500 = 1100 (55% de 2000)
        val offered = listOf(
            createTestItem("item_1", 500.0, 700.0), // mediana 600
            createTestItem("item_2", 400.0, 600.0)  // mediana 500
        )

        val ratio = useCase(targetItem = target, offeredItems = offered)

        assertTrue(ratio >= 0.40)
        assertEquals(0.55, ratio, 0.001)
    }

    @Test
    @DisplayName("Debe aceptar oferta con artículos de bajo valor compensados con TruequeCoins")
    fun shouldAcceptOffer_whenCompensatedWithTruequeCoins() {
        // Target: mediana = 1,000
        val target = createTestItem("target", 1000.0, 1000.0)
        // Offered item: 200 (20%) + TruequeCoins: 250 créditos = 450 (45%)
        val offered = listOf(createTestItem("cheap_item", 150.0, 250.0))
        val compensatoryCoins = 250.0

        val ratio = useCase(
            targetItem = target,
            offeredItems = offered,
            compensatoryCoins = compensatoryCoins
        )

        assertEquals(0.45, ratio, 0.001)
    }

    @Test
    @DisplayName("Debe rechazar oferta con excepción RN-02 si el ratio es menor al 40% (Anti-Spam)")
    fun shouldThrowException_whenOfferIsBelowFortyPercent() {
        // Target: mediana = 1,000
        val target = createTestItem("target", 1000.0, 1000.0)
        // Offered: mediana 250 (25% < 40%)
        val offered = listOf(createTestItem("insufficient_item", 200.0, 300.0))

        val exception = assertThrows(BusinessRuleException.ValueRatioBelowThresholdException::class.java) {
            useCase(targetItem = target, offeredItems = offered, compensatoryCoins = 0.0)
        }

        assertEquals("RN-02", exception.ruleCode)
        assertTrue(exception.actualRatio < 0.40)
    }
}
