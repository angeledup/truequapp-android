package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.TriangularCycle
import com.truequapp.core.domain.model.TriangularCycleStatus

/**
 * Caso de uso: Reglas de Negocio de Trueques Triangulares A->B->C->A (RN-06 / CU016).
 */
class TriangularCycleRulesUseCase {

    /**
     * Registra la firma de aceptación de un nodo participante en el ciclo.
     */
    fun signCycle(
        cycle: TriangularCycle,
        userId: String,
        currentTimestampEpochMs: Long = System.currentTimeMillis()
    ): TriangularCycle {
        if (cycle.status != TriangularCycleStatus.PROPUESTO) {
            throw BusinessRuleException.TriangularCycleAbortException(
                "El ciclo ya no se encuentra en estado propuesto (Estado: ${cycle.status})."
            )
        }

        if (currentTimestampEpochMs > cycle.expiresAtEpochMs) {
            // Expiró el plazo de 24 horas (RN-06)
            return cycle.copy(status = TriangularCycleStatus.ABORTADO)
        }

        val updatedParticipants = cycle.participants.map { participant ->
            if (participant.userId == userId) {
                participant.copy(hasAccepted = true, signedAtEpochMs = currentTimestampEpochMs)
            } else {
                participant
            }
        }

        val allAccepted = updatedParticipants.all { it.hasAccepted }
        val newStatus = if (allAccepted) {
            TriangularCycleStatus.BLOQUEADO_ESCROW
        } else {
            TriangularCycleStatus.PROPUESTO
        }

        return cycle.copy(
            participants = updatedParticipants,
            status = newStatus
        )
    }

    /**
     * Procesa la cancelación o rechazo de uno de los 3 usuarios.
     * Invariante RN-06: Desistimiento sin penalización.
     */
    fun abortCycleWithoutPenalty(
        cycle: TriangularCycle,
        decliningUserId: String
    ): TriangularCycle {
        return cycle.copy(status = TriangularCycleStatus.ABORTADO)
    }
}
