package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.ItemStatus
import com.truequapp.core.domain.model.TradeItem
import com.truequapp.core.domain.model.TradeProposal
import com.truequapp.core.domain.model.TradeStatus

/**
 * Resultado de ejecutar el bloqueo de reserva firme sobre una propuesta aceptada.
 */
data class InventoryLockResult(
    val acceptedProposal: TradeProposal,
    val lockedItems: List<TradeItem>,
    val affectedPendingProposalsToCancelOrPause: List<TradeProposal>
)

/**
 * Caso de uso: Exclusividad de Reserva Firme (RN-01 / ACID Locking).
 * Garantiza cero doble asignación de inventario.
 */
class LockInventoryForTradeUseCase {

    /**
     * Ejecuta la reserva exclusiva de los artículos comprometidos al aceptar un trueque.
     *
     * @param proposalToAccept Propuesta que fue aceptada por el receptor.
     * @param targetItem Artículo del receptor.
     * @param offeredItems Artículos ofertados por el proponente.
     * @param allActiveProposalsInSystem Todas las propuestas activas en el sistema para detectar colisiones.
     * @return [InventoryLockResult] con los artículos actualizados a RESERVADO y las propuestas conflictivas.
     */
    operator fun invoke(
        proposalToAccept: TradeProposal,
        targetItem: TradeItem,
        offeredItems: List<TradeItem>,
        allActiveProposalsInSystem: List<TradeProposal>
    ): InventoryLockResult {
        // 1. Validar que la propuesta esté en estado aceptable
        if (proposalToAccept.status != TradeStatus.PENDIENTE && proposalToAccept.status != TradeStatus.CONTRAOFERTADA) {
            throw BusinessRuleException.ExclusivityLockException(
                "La propuesta no puede reservarse porque su estado actual es ${proposalToAccept.status}."
            )
        }

        // 2. Validar que el artículo objetivo esté estrictamente DISPONIBLE
        if (targetItem.status != ItemStatus.DISPONIBLE) {
            throw BusinessRuleException.ExclusivityLockException(
                "El artículo objetivo '${targetItem.title}' (ID: ${targetItem.id}) ya se encuentra en estado ${targetItem.status}."
            )
        }

        // 3. Validar que todos los artículos ofertados estén DISPONIBLES
        val unavailableOffered = offeredItems.filter { it.status != ItemStatus.DISPONIBLE }
        if (unavailableOffered.isNotEmpty()) {
            val names = unavailableOffered.joinToString { "'${it.title}'" }
            throw BusinessRuleException.ExclusivityLockException(
                "Los siguientes artículos ofertados ya no están disponibles: $names."
            )
        }

        val allInvolvedItemIds = (offeredItems.map { it.id } + targetItem.id).toSet()

        // 4. Bloquear atómicamente todos los artículos a RESERVADO
        val updatedLockedItems = (offeredItems + targetItem).map {
            it.copy(status = ItemStatus.RESERVADO)
        }

        // 5. Identificar propuestas concurrentes que compiten por estos mismos artículos (RN-01)
        val conflictingProposals = allActiveProposalsInSystem.filter { otherProposal ->
            otherProposal.id != proposalToAccept.id &&
            otherProposal.status in listOf(TradeStatus.PENDIENTE, TradeStatus.CONTRAOFERTADA) &&
            (allInvolvedItemIds.contains(otherProposal.targetItemId) ||
             otherProposal.offeredItemIds.any { allInvolvedItemIds.contains(it) })
        }.map { conflictingProposal ->
            conflictingProposal.copy(status = TradeStatus.CANCELADO)
        }

        val updatedAcceptedProposal = proposalToAccept.copy(status = TradeStatus.RESERVADO)

        return InventoryLockResult(
            acceptedProposal = updatedAcceptedProposal,
            lockedItems = updatedLockedItems,
            affectedPendingProposalsToCancelOrPause = conflictingProposals
        )
    }
}
