package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.DoubleBlindStatus
import com.truequapp.core.domain.model.TradeReview
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RN-12: Protocolo de Evaluación Doble Ciego para Reseñas")
class EvaluateDoubleBlindReviewUseCaseTest {

    private lateinit var useCase: EvaluateDoubleBlindReviewUseCase

    @BeforeEach
    fun setUp() {
        useCase = EvaluateDoubleBlindReviewUseCase()
    }

    private fun createReview(id: String, reviewerId: String, evaluatedId: String, stars: Int): TradeReview {
        return TradeReview(
            id = id,
            tradeId = "trade_42",
            reviewerUserId = reviewerId,
            evaluatedUserId = evaluatedId,
            starsRating = stars,
            wasPunctual = true,
            matchedPhotos = true,
            writtenComment = "Excelente trueque, muy puntual."
        )
    }

    @Test
    @DisplayName("Debe mantener oculta la reseña si solo uno ha calificado y aún no pasan 7 días")
    fun shouldKeepHidden_whenOnlyOneHasReviewedAndUnderSevenDays() {
        val completedAt = 1000000000L
        val threeDaysLater = completedAt + (3 * 24 * 60 * 60 * 1000L)

        val reviewUserA = createReview("rev_1", "user_A", "user_B", 5)

        val outcome = useCase(
            reviewFromUserA = reviewUserA,
            reviewFromUserB = null,
            tradeCompletedAtEpochMs = completedAt,
            currentTimeEpochMs = threeDaysLater
        )

        assertEquals(DoubleBlindStatus.OCULTA_PENDIENTE_CONTRAPARTE, outcome.status)
        assertFalse(outcome.isRevealedToPublic)
        assertTrue(outcome.visibleReviews.isEmpty())
    }

    @Test
    @DisplayName("Debe revelar ambas reseñas inmediatamente si ambos participantes ya calificaron")
    fun shouldRevealImmediately_whenBothParticipantsHaveReviewed() {
        val completedAt = 1000000000L
        val oneDayLater = completedAt + (1 * 24 * 60 * 60 * 1000L)

        val reviewUserA = createReview("rev_1", "user_A", "user_B", 5)
        val reviewUserB = createReview("rev_2", "user_B", "user_A", 4)

        val outcome = useCase(
            reviewFromUserA = reviewUserA,
            reviewFromUserB = reviewUserB,
            tradeCompletedAtEpochMs = completedAt,
            currentTimeEpochMs = oneDayLater
        )

        assertEquals(DoubleBlindStatus.REVELADA_AMBOS_CALIFICARON, outcome.status)
        assertTrue(outcome.isRevealedToPublic)
        assertEquals(2, outcome.visibleReviews.size)
    }

    @Test
    @DisplayName("Debe revelar las reseñas existentes una vez transcurrida la ventana de 7 días")
    fun shouldRevealAfterSevenDays_evenIfOnlyOneParticipantReviewed() {
        val completedAt = 1000000000L
        val eightDaysLater = completedAt + (8 * 24 * 60 * 60 * 1000L)

        val reviewUserA = createReview("rev_1", "user_A", "user_B", 4)

        val outcome = useCase(
            reviewFromUserA = reviewUserA,
            reviewFromUserB = null,
            tradeCompletedAtEpochMs = completedAt,
            currentTimeEpochMs = eightDaysLater
        )

        assertEquals(DoubleBlindStatus.REVELADA_POR_VENCIMIENTO_7_DIAS, outcome.status)
        assertTrue(outcome.isRevealedToPublic)
        assertEquals(1, outcome.visibleReviews.size)
    }
}
