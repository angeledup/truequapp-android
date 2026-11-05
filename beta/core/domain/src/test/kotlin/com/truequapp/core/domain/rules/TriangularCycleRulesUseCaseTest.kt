package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.TriangularCycle
import com.truequapp.core.domain.model.TriangularCycleStatus
import com.truequapp.core.domain.model.TriangularNodeParticipant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RN-06: Desistimiento sin Penalización y Firmas en Trueques Triangulares")
class TriangularCycleRulesUseCaseTest {

    private lateinit var useCase: TriangularCycleRulesUseCase

    @BeforeEach
    fun setUp() {
        useCase = TriangularCycleRulesUseCase()
    }

    private fun createTriangularCycle(nowMs: Long): TriangularCycle {
        val participants = listOf(
            TriangularNodeParticipant("user_A", "item_A", "item_B"),
            TriangularNodeParticipant("user_B", "item_B", "item_C"),
            TriangularNodeParticipant("user_C", "item_C", "item_A")
        )
        return TriangularCycle(
            cycleId = "cycle_1",
            participants = participants,
            compatibilityScore = 0.88,
            status = TriangularCycleStatus.PROPUESTO,
            createdAtEpochMs = nowMs,
            expiresAtEpochMs = nowMs + (24 * 60 * 60 * 1000L) // 24h TTL
        )
    }

    @Test
    @DisplayName("Debe pasar a BLOQUEADO_ESCROW cuando los 3 participantes firman dentro de 24 horas")
    fun shouldTransitionToBlockedEscrow_whenAllThreeUsersSign() {
        val now = 1000000000L
        val initialCycle = createTriangularCycle(now)

        val cycle1 = useCase.signCycle(initialCycle, "user_A", now + 1000)
        assertEquals(TriangularCycleStatus.PROPUESTO, cycle1.status)
        assertEquals(1, cycle1.signaturesCount)

        val cycle2 = useCase.signCycle(cycle1, "user_B", now + 2000)
        assertEquals(TriangularCycleStatus.PROPUESTO, cycle2.status)
        assertEquals(2, cycle2.signaturesCount)

        val cycle3 = useCase.signCycle(cycle2, "user_C", now + 3000)
        assertEquals(TriangularCycleStatus.BLOQUEADO_ESCROW, cycle3.status)
        assertEquals(3, cycle3.signaturesCount)
        assertTrue(cycle3.isFullyConfirmed)
    }

    @Test
    @DisplayName("Debe abortar automáticamente si expira el plazo de 24 horas (RN-06)")
    fun shouldAbort_whenCycleExpiresBeforeThreeSignatures() {
        val now = 1000000000L
        val cycle = createTriangularCycle(now)
        val after25Hours = now + (25 * 60 * 60 * 1000L)

        val updated = useCase.signCycle(cycle, "user_A", currentTimestampEpochMs = after25Hours)

        assertEquals(TriangularCycleStatus.ABORTADO, updated.status)
    }

    @Test
    @DisplayName("Debe abortar sin penalización si un usuario declina la propuesta triangular")
    fun shouldAbortWithoutPenalty_whenOneUserDeclines() {
        val now = 1000000000L
        val cycle = createTriangularCycle(now)

        val aborted = useCase.abortCycleWithoutPenalty(cycle, decliningUserId = "user_B")

        assertEquals(TriangularCycleStatus.ABORTADO, aborted.status)
    }
}
