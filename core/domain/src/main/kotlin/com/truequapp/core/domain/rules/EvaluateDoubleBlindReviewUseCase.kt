package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.DoubleBlindStatus
import com.truequapp.core.domain.model.TradeReview

/**
 * Resultado de la evaluación de visibilidad bajo el protocolo Doble Ciego.
 */
data class DoubleBlindReviewOutcome(
    val status: DoubleBlindStatus,
    val isRevealedToPublic: Boolean,
    val visibleReviews: List<TradeReview>,
    val explanationMessage: String
)

/**
 * Caso de uso: Evaluación Doble Ciego para Reseñas y Calificaciones (RN-12 / CU020).
 *
 * Previene el sesgo de represalia: si el Usuario A deja una calificación negativa,
 * el Usuario B no puede verla antes de emitir la suya propia.
 * Las reseñas se liberan simultáneamente solo cuando:
 *  1. Ambas partes han emitido su reseña, O
 *  2. Han transcurrido 7 días desde la conclusión del intercambio.
 */
class EvaluateDoubleBlindReviewUseCase {

    /**
     * Evalúa si una o ambas reseñas deben revelarse públicamente y a las partes interesadas.
     *
     * @param reviewFromUserA Reseña emitida por el primer participante (o null si aún no califica).
     * @param reviewFromUserB Reseña emitida por la contraparte (o null si aún no califica).
     * @param tradeCompletedAtEpochMs Marca de tiempo cuando el intercambio fue marcado como COMPLETADO.
     * @param currentTimeEpochMs Marca de tiempo actual en milisegundos.
     * @return [DoubleBlindReviewOutcome] con el estado de revelación y reseñas públicas.
     */
    operator fun invoke(
        reviewFromUserA: TradeReview?,
        reviewFromUserB: TradeReview?,
        tradeCompletedAtEpochMs: Long,
        currentTimeEpochMs: Long = System.currentTimeMillis()
    ): DoubleBlindReviewOutcome {
        val daysElapsed = (currentTimeEpochMs - tradeCompletedAtEpochMs) / (1000.0 * 60 * 60 * 24)

        // Caso 1: Ambas partes ya emitieron su calificación
        if (reviewFromUserA != null && reviewFromUserB != null) {
            return DoubleBlindReviewOutcome(
                status = DoubleBlindStatus.REVELADA_AMBOS_CALIFICARON,
                isRevealedToPublic = true,
                visibleReviews = listOf(reviewFromUserA, reviewFromUserB),
                explanationMessage = "Ambas partes completaron su evaluación. Las reseñas son ahora públicas."
            )
        }

        // Caso 2: Solo una parte calificó, pero ya expiró la ventana de 7 días
        if (daysElapsed >= SEVEN_DAYS_THRESHOLD) {
            val submittedReviews = listOfNotNull(reviewFromUserA, reviewFromUserB)
            return DoubleBlindReviewOutcome(
                status = DoubleBlindStatus.REVELADA_POR_VENCIMIENTO_7_DIAS,
                isRevealedToPublic = true,
                visibleReviews = submittedReviews,
                explanationMessage = "Ventana de 7 días concluida. Se hacen públicas las evaluaciones registradas."
            )
        }

        // Caso 3: Solo una parte calificó y aún no transcurren 7 días (permanece oculta)
        val submittedReviews = listOfNotNull(reviewFromUserA, reviewFromUserB)
        return DoubleBlindReviewOutcome(
            status = DoubleBlindStatus.OCULTA_PENDIENTE_CONTRAPARTE,
            isRevealedToPublic = false,
            visibleReviews = emptyList(), // Ocultas para prevenir represalias en la contraparte
            explanationMessage = "Evaluación enviada con éxito. Permanecerá oculta hasta que tu contraparte califique o transcurran 7 días."
        )
    }

    companion object {
        const val SEVEN_DAYS_THRESHOLD = 7.0 // 7 días según RN-12 de la UV
    }
}
