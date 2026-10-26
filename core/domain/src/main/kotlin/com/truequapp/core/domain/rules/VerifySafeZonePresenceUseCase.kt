package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.GeoCoordinates
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Resultado de la verificación de proximidad física a un Punto Seguro.
 */
data class SafeZonePresenceResult(
    val isWithinSafeZone: Boolean,
    val distanceMeters: Double,
    val maxAllowedRadiusMeters: Double = 150.0
)

/**
 * Caso de uso: Verificación de Presencia en Safe Zone (RN-08 / CU019 / CU020).
 * Calcula la distancia ortodrómica (Haversine) entre la posición GPS real y el Punto Seguro.
 * Requiere una distancia <= 150 metros para autorizar check-in o reporte de inasistencia.
 */
class VerifySafeZonePresenceUseCase {

    /**
     * Valida si el usuario se encuentra dentro del radio permitido de la Safe Zone.
     *
     * @param userGps Coordenadas GPS actuales reportadas por FusedLocationProviderClient.
     * @param safeZoneCoordinates Coordenadas exactas del Punto Seguro verificado.
     * @param maxAllowedRadiusMeters Radio máximo permitido en metros (por defecto 150 m según RN-08).
     * @return [SafeZonePresenceResult] con la distancia calculada y si cumple el requisito.
     * @throws BusinessRuleException.GeofenceVerificationFailedException si enforceStrictException es true y excede el radio.
     */
    operator fun invoke(
        userGps: GeoCoordinates,
        safeZoneCoordinates: GeoCoordinates,
        maxAllowedRadiusMeters: Double = DEFAULT_MAX_SAFEZONE_RADIUS_METERS,
        enforceStrictException: Boolean = false
    ): SafeZonePresenceResult {
        val distance = calculateHaversineDistanceMeters(userGps, safeZoneCoordinates)
        val isWithinRadius = distance <= maxAllowedRadiusMeters

        if (!isWithinRadius && enforceStrictException) {
            throw BusinessRuleException.GeofenceVerificationFailedException(
                actualDistanceMeters = distance,
                maxAllowedDistanceMeters = maxAllowedRadiusMeters
            )
        }

        return SafeZonePresenceResult(
            isWithinSafeZone = isWithinRadius,
            distanceMeters = distance,
            maxAllowedRadiusMeters = maxAllowedRadiusMeters
        )
    }

    /**
     * Calcula la distancia ortodrómica en metros entre dos coordenadas geográficas mediante la fórmula de Haversine.
     */
    private fun calculateHaversineDistanceMeters(coord1: GeoCoordinates, coord2: GeoCoordinates): Double {
        val earthRadiusMeters = 6371000.0 // Radio medio de la Tierra

        val dLat = Math.toRadians(coord2.latitude - coord1.latitude)
        val dLon = Math.toRadians(coord2.longitude - coord1.longitude)

        val lat1Rad = Math.toRadians(coord1.latitude)
        val lat2Rad = Math.toRadians(coord2.latitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusMeters * c
    }

    companion object {
        const val DEFAULT_MAX_SAFEZONE_RADIUS_METERS = 150.0 // RN-08: Radio de 150 m
    }
}
