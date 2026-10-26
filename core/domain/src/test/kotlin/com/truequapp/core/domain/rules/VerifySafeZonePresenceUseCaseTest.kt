package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.GeoCoordinates
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RN-08: Verificación de Presencia en Safe Zone (Radio <= 150 metros)")
class VerifySafeZonePresenceUseCaseTest {

    private lateinit var useCase: VerifySafeZonePresenceUseCase

    @BeforeEach
    fun setUp() {
        useCase = VerifySafeZonePresenceUseCase()
    }

    // Coordenadas base en Xalapa, Ver. (Plaza Crystal)
    private val plazaCrystalCoordinates = GeoCoordinates(19.52800, -96.89700)

    @Test
    @DisplayName("Debe validar presencia cuando el usuario se encuentra a menos de 150 metros del Punto Seguro")
    fun shouldConfirmPresence_whenWithinOneHundredFiftyMeters() {
        // Desplazamiento aproximado de ~50 metros
        val userAtFiftyMeters = GeoCoordinates(19.52840, -96.89700)

        val result = useCase(
            userGps = userAtFiftyMeters,
            safeZoneCoordinates = plazaCrystalCoordinates
        )

        assertTrue(result.isWithinSafeZone)
        assertTrue(result.distanceMeters < 150.0)
    }

    @Test
    @DisplayName("Debe rechazar presencia cuando el usuario está a más de 150 metros")
    fun shouldRejectPresence_whenOutsideRadius() {
        // Desplazamiento de aproximadamente ~600 metros
        val userAtSixHundredMeters = GeoCoordinates(19.53300, -96.89700)

        val result = useCase(
            userGps = userAtSixHundredMeters,
            safeZoneCoordinates = plazaCrystalCoordinates,
            enforceStrictException = false
        )

        assertFalse(result.isWithinSafeZone)
        assertTrue(result.distanceMeters > 150.0)
    }

    @Test
    @DisplayName("Debe lanzar GeofenceVerificationFailedException si se solicita modo estricto fuera del radio")
    fun shouldThrowException_whenEnforceStrictIsActiveAndOutsideRadius() {
        val farAwayUser = GeoCoordinates(19.54000, -96.89700)

        val ex = assertThrows(BusinessRuleException.GeofenceVerificationFailedException::class.java) {
            useCase(
                userGps = farAwayUser,
                safeZoneCoordinates = plazaCrystalCoordinates,
                enforceStrictException = true
            )
        }

        assertEquals("RN-08", ex.ruleCode)
    }
}
