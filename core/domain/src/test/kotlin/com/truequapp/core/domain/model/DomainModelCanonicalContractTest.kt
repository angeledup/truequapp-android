package com.truequapp.core.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Validación de Contrato Canónico de Modelos de Dominio (Fase 0)")
class DomainModelCanonicalContractTest {

    @Test
    @DisplayName("TradeProposal: meetsMinimumValueRatio debe verificar regla RN-02 (40%)")
    fun shouldValidateMinimumValueRatioOnTradeProposal() {
        val proposal = TradeProposal(
            id = "prop_1",
            requesterId = "user_1",
            receiverId = "user_2",
            offeredItemIds = listOf("item_offered_1"),
            targetItemId = "item_target_1",
            compensatoryCoins = 100.0,
            status = TradeStatus.PENDIENTE,
            expiresAtUtc = System.currentTimeMillis() + 86400000L
        )

        // Target: 1000. Offered: 300 + 100 coins = 400 (exactamente 40%) -> true
        assertTrue(proposal.meetsMinimumValueRatio(targetItemValue = 1000.0, offeredTotalValue = 300.0))

        // Target: 1000. Offered: 250 + 100 coins = 350 (35% < 40%) -> false
        assertFalse(proposal.meetsMinimumValueRatio(targetItemValue = 1000.0, offeredTotalValue = 250.0))

        // Target: 1000. Offered: 500 + 100 coins = 600 (60% >= 40%) -> true
        assertTrue(proposal.meetsMinimumValueRatio(targetItemValue = 1000.0, offeredTotalValue = 500.0))

        // Target 0 o negativo -> false seguro
        assertFalse(proposal.meetsMinimumValueRatio(targetItemValue = 0.0, offeredTotalValue = 100.0))
    }

    @Test
    @DisplayName("UserProfile: Constructor canónico y propiedades de compatibilidad de Fase 0")
    fun shouldConstructCanonicalUserProfile() {
        val user = UserProfile(
            id = "user_100",
            fullName = "María García",
            username = "mgarcia",
            email = "maria@example.com",
            phoneNumber = "+522289998877",
            kycLevel = KycLevel.TELEFONO_SMS_NIVEL_1,
            bayesianRating = 4.2,
            completionRate = 0.98,
            fuzzyNeighborhood = "Revolución",
            isActive = true
        )

        assertEquals("user_100", user.id)
        assertEquals("María García", user.fullName)
        assertEquals(KycLevel.TELEFONO_SMS_NIVEL_1, user.kycLevel)
        assertEquals(4.2, user.bayesianRating)
        assertEquals(0.98, user.completionRate)
        assertEquals("Revolución", user.fuzzyNeighborhood)
        assertTrue(user.isActive)
    }

    @Test
    @DisplayName("TradeItem: Constructor canónico y cálculo de min/max y disponibilidad")
    fun shouldConstructCanonicalTradeItem() {
        val item = TradeItem(
            id = "item_canonical_1",
            ownerId = "user_1",
            title = "Guitarra Acústica Yamaha",
            description = "Con funda acolchonada y cuerdas nuevas",
            categoryId = 6,
            type = ItemType.BIEN_FISICO,
            condition = ItemCondition.COMO_NUEVO,
            estimatedValueMin = 1500.0,
            estimatedValueMax = 2000.0,
            imagesWebP = listOf("https://truequapp.org/cdn/yamaha1.webp"),
            tagsWishlist = listOf("música", "instrumentos"),
            isAvailable = true
        )

        assertEquals("item_canonical_1", item.id)
        assertEquals(1500.0, item.estimatedValueMin)
        assertEquals(2000.0, item.estimatedValueMax)
        assertEquals(1750.0, item.estimatedValue.medianValue)
        assertTrue(item.isAvailable)
    }

    @Test
    @DisplayName("Verificación de enumeraciones estándar de la Fase 0")
    fun shouldContainAllRequiredEnumEntries() {
        // TradeStatus
        assertNotNull(TradeStatus.valueOf("PENDIENTE"))
        assertNotNull(TradeStatus.valueOf("CONTRAOFERTADA"))
        assertNotNull(TradeStatus.valueOf("RESERVADO"))
        assertNotNull(TradeStatus.valueOf("EN_ENTREGA"))
        assertNotNull(TradeStatus.valueOf("COMPLETADO"))
        assertNotNull(TradeStatus.valueOf("CANCELADO"))
        assertNotNull(TradeStatus.valueOf("EN_DISPUTA"))

        // ItemCondition
        assertNotNull(ItemCondition.valueOf("NUEVO"))
        assertNotNull(ItemCondition.valueOf("COMO_NUEVO"))
        assertNotNull(ItemCondition.valueOf("USADO"))
        assertNotNull(ItemCondition.valueOf("REGULAR"))

        // ItemType
        assertNotNull(ItemType.valueOf("BIEN_FISICO"))
        assertNotNull(ItemType.valueOf("SERVICIO"))

        // KycLevel
        assertNotNull(KycLevel.valueOf("BASICO_NIVEL_0"))
        assertNotNull(KycLevel.valueOf("TELEFONO_SMS_NIVEL_1"))
        assertNotNull(KycLevel.valueOf("IDENTIFICACION_OFICIAL_NIVEL_2"))
    }
}
