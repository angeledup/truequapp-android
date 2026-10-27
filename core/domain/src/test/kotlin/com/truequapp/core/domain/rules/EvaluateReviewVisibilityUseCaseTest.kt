package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.ReviewVisibilityStatus
import com.truequapp.core.domain.model.TradeReview
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RN-12: Transparencia Pública de Reseñas (EvaluateReviewVisibilityUseCase)")
class EvaluateReviewVisibilityUseCaseTest {

    private lateinit var useCase: EvaluateReviewVisibilityUseCase

    @BeforeEach
    fun setUp() {
        useCase = EvaluateReviewVisibilityUseCase()
    }

    private fun buildReview(id: String, reviewerId: String, targetId: String, rating: Int): TradeReview {
        return TradeReview(
            id = id,
            tradeId = "trade_99",
            reviewerUserId = reviewerId,
            evaluatedUserId = targetId,
            starsRating = rating,
            wasPunctual = true,
            matchedPhotos = true,
            writtenComment = "Excelente experiencia de intercambio.",
            isPubliclyVisible = true,
            visibilityStatus = ReviewVisibilityStatus.PUBLICA_INMEDIATA
        )
    }

    @Test
    @DisplayName("Debe permitir a cualquier usuario ver reseñas de un perfil sin interacción previa")
    fun shouldAllowViewingReviews_withoutPriorInteraction() {
        val r1 = buildReview("rev_1", "user_alpha", "user_beta", 5)
        val r2 = buildReview("rev_2", "user_gamma", "user_beta", 4)

        // Usuario omega no tiene ninguna propuesta ni chat previo con user_beta
        val outcome = useCase(
            requestingUserId = "user_omega_nuevo",
            evaluatedUserId = "user_beta",
            allReviews = listOf(r1, r2)
        )

        assertEquals(ReviewVisibilityStatus.PUBLICA_INMEDIATA, outcome.status)
        assertTrue(outcome.isRevealedToPublic)
        assertEquals(2, outcome.visibleReviews.size)
    }

    @Test
    @DisplayName("Debe publicar la reseña inmediatamente sin retención de 7 días ni espera de contraparte")
    fun shouldPublishReviewImmediately_withoutWaiting() {
        val r1 = buildReview("rev_1", "user_alpha", "user_beta", 5)

        val outcome = useCase(
            reviewFromUserA = r1,
            reviewFromUserB = null
        )

        assertEquals(ReviewVisibilityStatus.PUBLICA_INMEDIATA, outcome.status)
        assertTrue(outcome.isRevealedToPublic)
        assertEquals(1, outcome.visibleReviews.size)
        assertEquals("rev_1", outcome.visibleReviews.first().id)
    }
}
