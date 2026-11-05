package com.truequapp.core.domain.model

/**
 * Nivel de verificación de identidad KYC según la especificación SWEBOK de Truequapp.
 */
enum class KycLevel(val levelCode: Int, val description: String) {
    BASICO_NIVEL_0(0, "Registro inicial sin verificar"),
    TELEFONO_SMS_NIVEL_1(1, "Teléfono verificado vía SMS OTP - Habilitado para trueques directos y monedero"),
    IDENTIFICACION_OFICIAL_NIVEL_2(2, "Documento de identidad verificado - Habilitado para trueques de alto valor y triangulares")
}

/**
 * Estado administrativo y de seguridad de la cuenta del usuario.
 */
enum class UserAccountStatus {
    ACTIVO,
    SUSPENDIDO_TEMPORALMENTE,
    DADO_DE_BAJA,
    BLOQUEADO_POR_FRAUDE
}

/**
 * Coordenadas geoespaciales precisas para cálculos internos (Puntos Seguros y check-in).
 */
data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double
)

/**
 * Coordenadas geoespaciales con privacidad difusa (RN-10 / RNF-08) en un radio de 500 metros.
 */
data class ObfuscatedGeoLocation(
    val neighborhoodName: String,
    val approximateCoordinates: GeoCoordinates,
    val blurRadiusMeters: Double = 500.0
)

/**
 * Entidad principal de Perfil de Usuario en el dominio de Truequapp.
 */
data class UserProfile(
    val id: String,
    val fullName: String,
    val username: String,
    val email: String,
    val phoneNumber: String,
    val kycLevel: KycLevel,
    val accountStatus: UserAccountStatus = UserAccountStatus.ACTIVO,
    val bayesianRating: Double = 3.0, // Calificación inicial neutral según RN-09
    val totalTradesCompleted: Int = 0,
    val totalRatingsCount: Int = 0,
    val attendanceRate: Double = 1.0, // 100% de cumplimiento inicial (1.0)
    val noShowIncidentsCount: Int = 0,
    val location: ObfuscatedGeoLocation,
    val searchRadiusKm: Int = 15,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val canInitiateTrade: Boolean
        get() = accountStatus == UserAccountStatus.ACTIVO && kycLevel != KycLevel.BASICO_NIVEL_0

    val canAccessTriangularTrade: Boolean
        get() = canInitiateTrade && bayesianRating >= 3.5 && attendanceRate >= 0.85

    val completionRate: Double
        get() = attendanceRate

    val fuzzyNeighborhood: String
        get() = location.neighborhoodName

    val isActive: Boolean
        get() = accountStatus == UserAccountStatus.ACTIVO

    /**
     * Constructor secundario compatible con la especificación canónica de FASE 0.
     */
    constructor(
        id: String,
        fullName: String,
        username: String,
        email: String,
        phoneNumber: String,
        kycLevel: KycLevel,
        bayesianRating: Double,
        completionRate: Double,
        fuzzyNeighborhood: String,
        isActive: Boolean
    ) : this(
        id = id,
        fullName = fullName,
        username = username,
        email = email,
        phoneNumber = phoneNumber,
        kycLevel = kycLevel,
        accountStatus = if (isActive) UserAccountStatus.ACTIVO else UserAccountStatus.SUSPENDIDO_TEMPORALMENTE,
        bayesianRating = bayesianRating,
        totalTradesCompleted = 0,
        totalRatingsCount = 0,
        attendanceRate = completionRate,
        noShowIncidentsCount = 0,
        location = ObfuscatedGeoLocation(
            neighborhoodName = fuzzyNeighborhood,
            approximateCoordinates = GeoCoordinates(0.0, 0.0)
        )
    )
}
