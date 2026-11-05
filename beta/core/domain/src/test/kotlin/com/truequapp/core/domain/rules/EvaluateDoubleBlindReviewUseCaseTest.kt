package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.DoubleBlindStatus
import com.truequapp.core.domain.model.ReviewVisibilityStatus
import com.truequapp.core.domain.model.TradeReview
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RN-12: Transparencia y Visibilidad Pública de Reseñas sin Interacción Previa")
class EvaluateDoubleBlindReviewUseCaseTest {

    private lateinit var useCase: EvaluateDoubleBlindReviewUseCase

    @BeforeEach
    fun setUp() {
        useCase = EvaluateDoubleBlindReviewUseCase()
    }

    private fun createReview(
        id: String,
        reviewerId: String,
        evaluatedId: String,
        stars: Int,
        status: ReviewVisibilityStatus = ReviewVisibilityStatus.PUBLICA_INMEDIATA
    ): TradeReview {
        return TradeReview(
            id = id,
            tradeId = "trade_42",
            reviewerUserId = reviewerId,
            evaluatedUserId = evaluatedId,
            starsRating = stars,
            wasPunctual = true,
            matchedPhotos = true,
            writtenComment = "Excelente trueque, muy puntual.",
            isPubliclyVisible = (status == ReviewVisibilityStatus.PUBLICA_INMEDIATA),
            visibilityStatus = status
        )
    }

    @Test
    @DisplayName("Debe hacer pública la reseña inmediatamente aunque solo un participante haya calificado")
    fun shouldMakeReviewVisibleImmediately_whenOnlyOneParticipantReviewed() {
        val reviewUserA = createReview("rev_1", "user_A", "user_B", 5)

        val outcome = useCase(
            reviewFromUserA = reviewUserA,
            reviewFromUserB = null
        )

        assertEquals(DoubleBlindStatus.PUBLICA_INMEDIATA, outcome.status)
        assertTrue(outcome.isRevealedToPublic)
        assertEquals(1, outcome.visibleReviews.size)
        assertEquals("rev_1", outcome.visibleReviews.first().id)
    }

    @Test
    @DisplayName("Debe mostrar ambas reseñas inmediatamente cuando ambas partes han calificado")
    fun shouldShowBothReviewsImmediately_whenBothParticipantsReviewed() {
        val reviewUserA = createReview("rev_1", "user_A", "user_B", 5)
        val reviewUserB = createReview("rev_2", "user_B", "user_A", 4)

        val outcome = useCase(
            reviewFromUserA = reviewUserA,
            reviewFromUserB = reviewUserB
        )

        assertEquals(DoubleBlindStatus.PUBLICA_INMEDIATA, outcome.status)
        assertTrue(outcome.isRevealedToPublic)
        assertEquals(2, outcome.visibleReviews.size)
    }

    @Test
    @DisplayName("Cualquier usuario debe poder consultar las reseñas sin haber interactuado antes (RN-12)")
    fun shouldAllowViewingReviews_withoutPriorInteraction() {
        val review1 = createReview("rev_1", "user_A", "user_target", 5)
        val review2 = createReview("rev_2", "user_B", "user_target", 4)

        // Usuario C (tercero que jamás ha interactuado ni enviado ofertas al usuario target)
        val publicReviews = useCase.getPublicReviews(
            requestingUserId = "user_C_stranger",
            evaluatedUserId = "user_target",
            allReviews = listOf(review1, review2)
        )

        assertEquals(2, publicReviews.size)
        assertTrue(publicReviews.any { it.id == "rev_1" })
        assertTrue(publicReviews.any { it.id == "rev_2" })
    }

    @Test
    @DisplayName("No debe publicar reseñas temporalmente ocultas por disputa formal en arbitraje")
    fun shouldFilterOutReviews_whenUnderDisputeRevision() {
        val reviewNormal = createReview("rev_1", "user_A", "user_target", 5)
        val reviewDisputed = createReview(
            id = "rev_disputed",
            reviewerId = "user_malicious",
            evaluatedId = "user_target",
            stars = 1,
            status = ReviewVisibilityStatus.OCULTA_POR_DISPUTA_EN_REVISION
        )

        val publicReviews = useCase.getPublicReviews(
            requestingUserId = "user_C_stranger",
            evaluatedUserId = "user_target",
            allReviews = listOf(reviewNormal, reviewDisputed)
        )

        assertEquals(1, publicReviews.size)
        assertEquals("rev_1", publicReviews.first().id)
    }
}
