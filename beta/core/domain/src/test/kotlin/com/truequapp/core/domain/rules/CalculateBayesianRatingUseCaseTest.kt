package com.truequapp.core.domain.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RN-09: Cálculo de Reputación Bayesiana Ponderada")
class CalculateBayesianRatingUseCaseTest {

    private lateinit var useCase: CalculateBayesianRatingUseCase

    @BeforeEach
    fun setUp() {
        useCase = CalculateBayesianRatingUseCase()
    }

    @Test
    @DisplayName("Usuario nuevo sin valoraciones debe iniciar con calificación neutra de 3.0")
    fun shouldReturnNeutralRating_forNewUserWithZeroRatings() {
        val score = useCase(
            existingRatingsCount = 0,
            existingSumOfRatings = 0.0
        )

        assertEquals(3.0, score, 0.01)
    }

    @Test
    @DisplayName("Una sola calificación de 5 estrellas no debe inflar la reputación a 5.0 (debe dar ~3.33)")
    fun shouldNotOverinflate_forSingleFiveStarRating() {
        // C=5, m=3.0, n=1, S=5.0 -> (15 + 5) / 6 = 3.33
        val score = useCase(
            existingRatingsCount = 0,
            existingSumOfRatings = 0.0,
            newStarRating = 5
        )

        assertEquals(3.33, score, 0.01)
    }

    @Test
    @DisplayName("Usuario veterano con múltiples valoraciones altas debe aproximarse gradualmente a su promedio real")
    fun shouldRewardConsistency_forVeteransWithManyReviews() {
        // Usuario con 50 calificaciones de 5 estrellas (S = 250)
        // (15 + 250) / (5 + 50) = 265 / 55 = 4.82
        val score = useCase(
            existingRatingsCount = 50,
            existingSumOfRatings = 250.0
        )

        assertEquals(4.82, score, 0.01)
        assertTrue(score > 4.80)
    }
}
