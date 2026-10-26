package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.GeoCoordinates
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@DisplayName("RN-10: Política de Privacidad Geoespacial y Ofuscación Difusa (500m)")
class ObfuscateLocationUseCaseTest {

    private lateinit var useCase: ObfuscateLocationUseCase

    @BeforeEach
    fun setUp() {
        useCase = ObfuscateLocationUseCase()
    }

    private fun calculateHaversineDistanceMeters(c1: GeoCoordinates, c2: GeoCoordinates): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(c2.latitude - c1.latitude)
        val dLon = Math.toRadians(c2.longitude - c1.longitude)
        val lat1Rad = Math.toRadians(c1.latitude)
        val lat2Rad = Math.toRadians(c2.latitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    @Test
    @DisplayName("Debe ofuscar coordenadas dentro del radio máximo permitido de 500 metros (RN-10 / RNF-08)")
    fun shouldObfuscateWithinFiveHundredMetersRadius() {
        // Centro de Xalapa, Veracruz
        val realCoordinates = GeoCoordinates(19.54377, -96.91018)
        val neighborhood = "Centro Histórico"

        for (i in 1..20) {
            val result = useCase(
                exactCoordinates = realCoordinates,
                neighborhoodName = neighborhood
            )

            val distance = calculateHaversineDistanceMeters(
                realCoordinates,
                result.approximateCoordinates
            )

            assertTrue(
                distance <= 500.0,
                "La distancia calculada ($distance m) excede el radio de 500 m fijado por la política de privacidad."
            )
            assertEquals(500.0, result.blurRadiusMeters)
            assertEquals("Centro Histórico", result.neighborhoodName)
        }
    }

    @Test
    @DisplayName("Debe producir resultados consistentes e idénticos al utilizar una semilla determinista")
    fun shouldProduceDeterministicCoordinates_whenSeedIsProvided() {
        val realCoordinates = GeoCoordinates(19.53120, -96.90230)
        val neighborhood = "Ánimas Sección Bosques"
        val seed = 421098L

        val result1 = useCase(
            exactCoordinates = realCoordinates,
            neighborhoodName = neighborhood,
            deterministicSeed = seed
        )

        val result2 = useCase(
            exactCoordinates = realCoordinates,
            neighborhoodName = neighborhood,
            deterministicSeed = seed
        )

        assertEquals(result1.approximateCoordinates.latitude, result2.approximateCoordinates.latitude, 0.0000001)
        assertEquals(result1.approximateCoordinates.longitude, result2.approximateCoordinates.longitude, 0.0000001)
        assertEquals(result1.neighborhoodName, result2.neighborhoodName)
    }

    @Test
    @DisplayName("Debe respetar radios personalizados de difuminado si se solicitan")
    fun shouldRespectCustomBlurRadius() {
        val realCoordinates = GeoCoordinates(19.50000, -96.80000)
        val customRadius = 250.0

        val result = useCase(
            exactCoordinates = realCoordinates,
            neighborhoodName = "Zona Universitaria",
            blurRadiusMeters = customRadius
        )

        val distance = calculateHaversineDistanceMeters(
            realCoordinates,
            result.approximateCoordinates
        )

        assertTrue(distance <= customRadius)
        assertEquals(customRadius, result.blurRadiusMeters)
    }
}
