package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.DoubleBlindStatus
import com.truequapp.core.domain.model.TradeReview

/**
 * Resultado de la evaluación de visibilidad de reseñas (RN-12).
 */
data class DoubleBlindReviewOutcome(
    val status: DoubleBlindStatus,
    val isRevealedToPublic: Boolean,
    val visibleReviews: List<TradeReview>,
    val explanationMessage: String
)

/**
 * Caso de uso: Transparencia y Visibilidad de Reseñas (RN-12 / CU020).
 *
 * Bajo la regla RN-12 modificada (Transparencia Pública):
 * Las reseñas emitidas son públicas de manera inmediata para cualquier usuario,
 * sin importar si ha interactuado previamente o no con el perfil evaluado.
 * Esto garantiza que cualquiera pueda comprobar la reputación antes de iniciar una oferta.
 */
class EvaluateDoubleBlindReviewUseCase {

    private val reviewVisibilityUseCase = EvaluateReviewVisibilityUseCase()

    /**
     * Evalúa la publicación inmediata de reseñas al ser emitidas.
     * Ya no se ocultan ni se retienen por 7 días; se publican al registrarse.
     */
    operator fun invoke(
        reviewFromUserA: TradeReview?,
        reviewFromUserB: TradeReview?,
        tradeCompletedAtEpochMs: Long = 0L,
        currentTimeEpochMs: Long = System.currentTimeMillis()
    ): DoubleBlindReviewOutcome {
        val submittedReviews = listOfNotNull(reviewFromUserA, reviewFromUserB)

        return DoubleBlindReviewOutcome(
            status = DoubleBlindStatus.PUBLICA_INMEDIATA,
            isRevealedToPublic = submittedReviews.isNotEmpty(),
            visibleReviews = submittedReviews,
            explanationMessage = "Las reseñas emitidas son públicas de inmediato para toda la comunidad."
        )
    }

    /**
     * Permite a cualquier usuario consultar las reseñas públicas de un perfil sin interacción previa.
     */
    fun getPublicReviews(
        requestingUserId: String?,
        evaluatedUserId: String,
        allReviews: List<TradeReview>
    ): List<TradeReview> {
        return reviewVisibilityUseCase(requestingUserId, evaluatedUserId, allReviews).visibleReviews
    }
}
