package com.truequapp.core.domain.model

/**
 * Estado del ciclo de trueque triangular A -> B -> C -> A (CU016).
 */
enum class TriangularCycleStatus {
    PROPUESTO,         // Detectado por el motor de grafos, esperando firmas (24h TTL)
    BLOQUEADO_ESCROW,  // 3 de 3 firmas registradas; los 3 artículos quedan RESERVADOS
    EN_COORDINACION,   // Cita grupal pactada en Safe Zone
    EJECUTADO,         // Intercambio presencial completado exitosamente
    ABORTADO           // Expiró el plazo de 24h o uno de los miembros declinó (RN-06)
}

/**
 * Participante individual en una de las aristas del ciclo triangular.
 */
data class TriangularNodeParticipant(
    val userId: String,
    val givenItemId: String,
    val receivedItemId: String,
    val hasAccepted: Boolean = false,
    val signedAtEpochMs: Long? = null
)

/**
 * Entidad que modela un ciclo cerrado multilateral de longitud 3 detectado por el motor de grafos.
 */
data class TriangularCycle(
    val cycleId: String,
    val participants: List<TriangularNodeParticipant>,
    val compatibilityScore: Double, // Debe ser >= 0.70 según la especificación
    val status: TriangularCycleStatus = TriangularCycleStatus.PROPUESTO,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val expiresAtEpochMs: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000L), // 24 horas TTL
    val safeZoneId: String? = null
) {
    init {
        require(participants.size == 3) {
            "Un ciclo de trueque triangular debe contener exactamente 3 participantes ordenados en bucle dirigido."
        }
        require(compatibilityScore in 0.0..1.0) {
            "El puntaje de compatibilidad debe encontrarse en el rango de 0.0 a 1.0."
        }
    }

    val signaturesCount: Int
        get() = participants.count { it.hasAccepted }

    val isFullyConfirmed: Boolean
        get() = signaturesCount == 3

    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAtEpochMs && status == TriangularCycleStatus.PROPUESTO
}
