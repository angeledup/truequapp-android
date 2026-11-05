package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.EstimatedValueRange
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.core.domain.model.ItemStatus
import com.truequapp.core.domain.model.ItemType
import com.truequapp.core.domain.model.TradeItem
import com.truequapp.core.domain.model.TradeModality
import com.truequapp.core.domain.model.TradeProposal
import com.truequapp.core.domain.model.TradeStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RN-01: Exclusividad de Reserva Firme y Bloqueo ACID de Inventario")
class LockInventoryForTradeUseCaseTest {

    private lateinit var useCase: LockInventoryForTradeUseCase

    @BeforeEach
    fun setUp() {
        useCase = LockInventoryForTradeUseCase()
    }

    private fun createItem(id: String, status: ItemStatus = ItemStatus.DISPONIBLE): TradeItem {
        return TradeItem(
            id = id,
            ownerId = "owner_$id",
            title = "Item $id",
            description = "Desc",
            categoryId = 1,
            type = ItemType.BIEN_FISICO,
            condition = ItemCondition.USADO,
            estimatedValue = EstimatedValueRange(500.0, 500.0),
            imagesWebP = emptyList(),
            tagsWishlist = emptyList(),
            status = status
        )
    }

    private fun createProposal(
        id: String,
        targetItemId: String,
        offeredItemIds: List<String>,
        status: TradeStatus = TradeStatus.PENDIENTE
    ): TradeProposal {
        return TradeProposal(
            id = id,
            requesterUserId = "user_req_$id",
            receiverUserId = "user_rec_$id",
            offeredItemIds = offeredItemIds,
            targetItemId = targetItemId,
            modality = TradeModality.DIRECTO_1_A_1,
            status = status
        )
    }

    @Test
    @DisplayName("Debe bloquear artículos a RESERVADO y cancelar propuestas competidoras concurrentes")
    fun shouldLockItemsAndCancelConflictingProposals() {
        val targetItem = createItem("target_laptop")
        val offeredItem = createItem("offered_bike")

        val acceptedProposal = createProposal("prop_winner", targetItem.id, listOf(offeredItem.id))

        // Propuesta competidora que también quería target_laptop
        val competitor1 = createProposal("prop_comp_1", targetItem.id, listOf("other_guitar"))
        // Propuesta competidora que ofrecía offered_bike por otra cosa
        val competitor2 = createProposal("prop_comp_2", "another_phone", listOf(offeredItem.id))
        // Propuesta no relacionada
        val unconflicted = createProposal("prop_safe", "unrelated_1", listOf("unrelated_2"))

        val allProposals = listOf(acceptedProposal, competitor1, competitor2, unconflicted)

        val result = useCase(
            proposalToAccept = acceptedProposal,
            targetItem = targetItem,
            offeredItems = listOf(offeredItem),
            allActiveProposalsInSystem = allProposals
        )

        assertEquals(TradeStatus.RESERVADO, result.acceptedProposal.status)
        assertEquals(2, result.lockedItems.size)
        assertTrue(result.lockedItems.all { it.status == ItemStatus.RESERVADO })

        // 2 propuestas en conflicto canceladas/pausadas
        assertEquals(2, result.affectedPendingProposalsToCancelOrPause.size)
        assertTrue(result.affectedPendingProposalsToCancelOrPause.any { it.id == "prop_comp_1" })
        assertTrue(result.affectedPendingProposalsToCancelOrPause.any { it.id == "prop_comp_2" })
    }

    @Test
    @DisplayName("Debe lanzar ExclusivityLockException si el artículo objetivo ya no está DISPONIBLE")
    fun shouldThrowException_whenTargetItemIsAlreadyReserved() {
        val targetItem = createItem("target_laptop", status = ItemStatus.RESERVADO)
        val offeredItem = createItem("offered_bike")
        val proposal = createProposal("prop_1", targetItem.id, listOf(offeredItem.id))

        val ex = assertThrows(BusinessRuleException.ExclusivityLockException::class.java) {
            useCase(
                proposalToAccept = proposal,
                targetItem = targetItem,
                offeredItems = listOf(offeredItem),
                allActiveProposalsInSystem = listOf(proposal)
            )
        }

        assertEquals("RN-01", ex.ruleCode)
    }
}
