package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.ReviewVisibilityStatus
import com.truequapp.core.domain.model.TradeReview

/**
 * Resultado de la evaluación de visibilidad bajo la regla de Transparencia Pública (RN-12).
 */
data class ReviewVisibilityOutcome(
    val status: ReviewVisibilityStatus,
    val isRevealedToPublic: Boolean,
    val visibleReviews: List<TradeReview>,
    val explanationMessage: String
)

/**
 * Caso de uso: Transparencia y Visibilidad Pública de Reseñas (RN-12 / CU020).
 *
 * Principio de Seguridad y Confianza Comunitaria:
 * Las reseñas y calificaciones históricas de cualquier usuario son completamente públicas y visibles
 * para cualquier miembro de la plataforma, SIN importar si ha interactuado previamente o no con él.
 *
 * Esto garantiza que antes de iniciar un trueque, realizar una oferta o pactar una cita,
 * los usuarios puedan auditar la reputación, puntualidad y cumplimiento del usuario evaluado.
 *
 * Asimismo, toda reseña emitida sobre un intercambio completado se hace pública de forma inmediata,
 * eliminando esquemas de bloqueo o doble ciego que impedían ver la reputación a tiempo.
 */
class EvaluateReviewVisibilityUseCase {

    /**
     * Evalúa y devuelve las reseñas visibles de un usuario para cualquier consultante público.
     *
     * @param requestingUserId ID del usuario que consulta (puede ser un tercero que nunca ha interactuado).
     * @param evaluatedUserId ID del usuario evaluado cuyas reseñas se están auditando.
     * @param allReviews Lista de todas las reseñas históricas registradas para el usuario evaluado.
     * @return [ReviewVisibilityOutcome] con las reseñas públicas disponibles para el consultante.
     */
    operator fun invoke(
        requestingUserId: String?,
        evaluatedUserId: String,
        allReviews: List<TradeReview>
    ): ReviewVisibilityOutcome {
        // En virtud de RN-12: Cualquier usuario puede auditar las reseñas públicas sin interacción previa.
        val publicReviews = allReviews.filter { review ->
            review.isPubliclyVisible && review.visibilityStatus == ReviewVisibilityStatus.PUBLICA_INMEDIATA
        }

        return ReviewVisibilityOutcome(
            status = ReviewVisibilityStatus.PUBLICA_INMEDIATA,
            isRevealedToPublic = true,
            visibleReviews = publicReviews,
            explanationMessage = "Las reseñas son públicas para permitir evaluar la confiabilidad del usuario antes de interactuar."
        )
    }

    /**
     * Sobrecarga para evaluar la publicación inmediata de reseñas al completarse un intercambio.
     * Las reseñas emitidas no se ocultan; quedan visibles inmediatamente para ambas partes y el público.
     *
     * @param reviewFromUserA Reseña emitida por el primer participante (o null si aún no emite).
     * @param reviewFromUserB Reseña emitida por la contraparte (o null si aún no emite).
     * @param tradeCompletedAtEpochMs Marca de tiempo cuando el intercambio fue marcado como completado.
     * @param currentTimeEpochMs Marca de tiempo actual.
     */
    operator fun invoke(
        reviewFromUserA: TradeReview?,
        reviewFromUserB: TradeReview?,
        tradeCompletedAtEpochMs: Long = 0L,
        currentTimeEpochMs: Long = System.currentTimeMillis()
    ): ReviewVisibilityOutcome {
        val submittedReviews = listOfNotNull(reviewFromUserA, reviewFromUserB)
            .filter { it.isPubliclyVisible && it.visibilityStatus == ReviewVisibilityStatus.PUBLICA_INMEDIATA }

        return ReviewVisibilityOutcome(
            status = ReviewVisibilityStatus.PUBLICA_INMEDIATA,
            isRevealedToPublic = submittedReviews.isNotEmpty(),
            visibleReviews = submittedReviews,
            explanationMessage = if (submittedReviews.isEmpty()) {
                "Aún no se han emitido reseñas para este intercambio."
            } else {
                "Las evaluaciones emitidas son visibles de inmediato para toda la comunidad."
            }
        )
    }
}
