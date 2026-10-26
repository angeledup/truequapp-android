package com.truequapp.core.domain.model

/**
 * Reseña y evaluación mutua entre participantes de un intercambio completado (CU020 / RN-12).
 */
data class TradeReview(
    val id: String,
    val tradeId: String,
    val reviewerUserId: String,
    val evaluatedUserId: String,
    val starsRating: Int, // 1 a 5 estrellas
    val wasPunctual: Boolean,
    val matchedPhotos: Boolean,
    val writtenComment: String,
    val submittedAtEpochMs: Long = System.currentTimeMillis()
) {
    init {
        require(starsRating in 1..5) { "La calificación en estrellas debe encontrarse entre 1 y 5." }
    }
}

/**
 * Estado de revelación de una evaluación bajo el mecanismo Doble Ciego (RN-12).
 */
enum class DoubleBlindStatus {
    OCULTA_PENDIENTE_CONTRAPARTE, // Una de las partes ya calificó, pero se oculta para prevenir sesgo
    REVELADA_AMBOS_CALIFICARON,    // Ambas partes enviaron su evaluación; se hace pública
    REVELADA_POR_VENCIMIENTO_7_DIAS // Transcurrieron 7 días desde el trueque; se publica automáticamente
}

/**
 * Motivos admitidos para abrir una disputa o reporte de incidencia (CU022).
 */
enum class DisputeReason {
    INASISTENCIA_A_CITA_PACTADA,
    ARTICULO_NO_COINCIDE_CON_FOTOS,
    DEFECTO_OCULTO_NO_DECLARADO,
    INTENTO_DE_COBRO_MONETARIO_ILEGAL,
    CONDUCTA_INAPROPIADA_O_AMENAZAS
}

/**
 * Estado resolutivo del expediente de disputa.
 */
enum class DisputeStatus {
    ABIERTO_EN_REVISION,
    RESOLUCION_A_FAVOR_RECLAMANTE,
    RESOLUCION_A_FAVOR_CONTRAPARTE,
    CANCELADA_POR_ACUERDO_MUTUO
}

/**
 * Expediente de arbitraje y disputa de Escrow.
 */
data class DisputeTicket(
    val ticketId: String,
    val tradeId: String,
    val reportingUserId: String,
    val reportedUserId: String,
    val reason: DisputeReason,
    val photoEvidenceUrls: List<String>,
    val reporterGpsVerifiedInSafeZone: Boolean,
    val status: DisputeStatus = DisputeStatus.ABIERTO_EN_REVISION,
    val moderatorNotes: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val resolvedAtEpochMs: Long? = null
)
