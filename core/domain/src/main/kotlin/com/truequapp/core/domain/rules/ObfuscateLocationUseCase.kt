package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.GeoCoordinates
import com.truequapp.core.domain.model.ObfuscatedGeoLocation
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Caso de uso: Política de Privacidad Geoespacial (RN-10 / RNF-08).
 *
 * Protege la integridad y privacidad del domicilio del usuario en listados públicos y perfiles.
 * Las coordenadas exactas nunca se comparten públicamente; en su lugar, se desplaza la coordenada
 * mediante un vector pseudo-aleatorio acotado a un radio difuso de 500 metros.
 */
class ObfuscateLocationUseCase {

    /**
     * Ofusca las coordenadas exactas residenciales del usuario para su exposición pública.
     *
     * @param exactCoordinates Coordenadas GPS exactas del usuario.
     * @param neighborhoodName Nombre de la colonia o sector municipal de referencia.
     * @param blurRadiusMeters Radio máximo de ofuscación en metros (por defecto 500.0 m según RN-10).
     * @param deterministicSeed Semilla opcional para garantizar que la misma colonia no cambie de punto en cada render.
     * @return [ObfuscatedGeoLocation] con el punto aproximado y radio de privacidad.
     */
    operator fun invoke(
        exactCoordinates: GeoCoordinates,
        neighborhoodName: String,
        blurRadiusMeters: Double = DEFAULT_BLUR_RADIUS_METERS,
        deterministicSeed: Long? = null
    ): ObfuscatedGeoLocation {
        // Generar un desplazamiento uniforme en el círculo de radio blurRadiusMeters
        // 1 metro de latitud ≈ 1 / 111,320 grados
        // 1 metro de longitud ≈ 1 / (111,320 * cos(lat)) grados
        val (randomAngle, randomRadiusRatio) = if (deterministicSeed != null) {
            val pseudoRandom = java.util.Random(deterministicSeed)
            val angle = pseudoRandom.nextDouble() * 2 * Math.PI
            val ratio = sqrt(pseudoRandom.nextDouble()) // Distribución de área uniforme
            Pair(angle, ratio)
        } else {
            val angle = Math.random() * 2 * Math.PI
            val ratio = sqrt(Math.random())
            Pair(angle, ratio)
        }

        val distanceMeters = blurRadiusMeters * randomRadiusRatio
        val metersPerDegreeLat = 111320.0
        val metersPerDegreeLon = 111320.0 * cos(Math.toRadians(exactCoordinates.latitude))

        val deltaLat = (distanceMeters * sin(randomAngle)) / metersPerDegreeLat
        val deltaLon = if (metersPerDegreeLon != 0.0) {
            (distanceMeters * cos(randomAngle)) / metersPerDegreeLon
        } else {
            0.0
        }

        val blurredCoordinates = GeoCoordinates(
            latitude = exactCoordinates.latitude + deltaLat,
            longitude = exactCoordinates.longitude + deltaLon
        )

        return ObfuscatedGeoLocation(
            neighborhoodName = neighborhoodName.trim(),
            approximateCoordinates = blurredCoordinates,
            blurRadiusMeters = blurRadiusMeters
        )
    }

    companion object {
        const val DEFAULT_BLUR_RADIUS_METERS = 500.0 // RNF-08 / RN-10: 500 metros
    }
}
