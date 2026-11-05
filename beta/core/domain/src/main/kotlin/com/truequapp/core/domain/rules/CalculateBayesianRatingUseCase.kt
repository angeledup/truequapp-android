package com.truequapp.core.domain.rules

import kotlin.math.round

/**
 * Caso de uso: Cálculo de Reputación Bayesiana (RN-09).
 *
 * En lugar de usar un promedio aritmético simple (que favorece artificialmente a cuentas nuevas
 * con una sola reseña perfecta de 5.0 sobre cuentas veteranas con 50 reseñas y 4.8),
 * Truequapp implementa un estimador bayesiano ponderado (m-estimate):
 *
 *   BR = (C * m + S) / (C + n)
 *
 * Donde:
 *   C = Peso de confianza previo (constante = 5.0 valoraciones)
 *   m = Media previa del ecosistema / neutral (constante = 3.0 estrellas)
 *   n = Número total de valoraciones recibidas por el usuario
 *   S = Suma total de estrellas recibidas (o media_aritmética * n)
 */
class CalculateBayesianRatingUseCase {

    /**
     * Calcula la calificación bayesiana ponderada del usuario tras recibir una nueva reseña o al recalcular perfil.
     *
     * @param existingRatingsCount Cantidad total de calificaciones previas (n).
     * @param existingSumOfRatings Suma acumulada de estrellas de todas las reseñas anteriores (S).
     * @param newStarRating Calificación en estrellas de la nueva reseña recibida (1 a 5), o null si es solo recálculo.
     * @param confidenceWeight Peso de la estimación previa C (por defecto 5.0).
     * @param priorMeanRating Media neutra esperada m (por defecto 3.0).
     * @return Puntuación bayesiana ponderada redondeada a 2 decimales (entre 1.0 y 5.0).
     */
    operator fun invoke(
        existingRatingsCount: Int,
        existingSumOfRatings: Double,
        newStarRating: Int? = null,
        confidenceWeight: Double = DEFAULT_CONFIDENCE_WEIGHT,
        priorMeanRating: Double = DEFAULT_PRIOR_MEAN_RATING
    ): Double {
        require(existingRatingsCount >= 0) { "El total de valoraciones no puede ser negativo." }
        require(existingSumOfRatings >= 0.0) { "La suma de valoraciones no puede ser negativa." }

        val totalN = if (newStarRating != null) {
            require(newStarRating in 1..5) { "La calificación debe encontrarse entre 1 y 5 estrellas." }
            existingRatingsCount + 1
        } else {
            existingRatingsCount
        }

        val totalS = if (newStarRating != null) {
            existingSumOfRatings + newStarRating.toDouble()
        } else {
            existingSumOfRatings
        }

        val bayesianScore = (confidenceWeight * priorMeanRating + totalS) / (confidenceWeight + totalN)

        // Redondeo a 2 cifras decimales (ej. 4.35)
        return round(bayesianScore * 100.0) / 100.0
    }

    companion object {
        const val DEFAULT_CONFIDENCE_WEIGHT = 5.0 // C: 5 valoraciones de inercia
        const val DEFAULT_PRIOR_MEAN_RATING = 3.0 // m: 3.0 calificación neutra de arranque
    }
}
