package com.truequapp.core.domain.model

/**
 * Reseña y evaluación entre participantes de un intercambio completado (CU020 / RN-12).
 * Bajo la regla RN-12 de Transparencia Pública, las reseñas son de dominio público para permitir
 * que cualquier usuario consulte la reputación y confianza de un perfil antes de interactuar.
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
    val isPubliclyVisible: Boolean = true,
    val visibilityStatus: ReviewVisibilityStatus = ReviewVisibilityStatus.PUBLICA_INMEDIATA,
    val submittedAtEpochMs: Long = System.currentTimeMillis()
) {
    init {
        require(starsRating in 1..5) { "La calificación en estrellas debe encontrarse entre 1 y 5." }
    }
}

/**
 * Estado de visibilidad de una reseña bajo el principio de Transparencia Pública (RN-12).
 * Garantiza que cualquier usuario pueda consultar las reseñas y calificaciones históricas
 * de otro usuario sin requerir haber interactuado previamente con él.
 */
enum class ReviewVisibilityStatus {
    PUBLICA_INMEDIATA,             // Visible públicamente para cualquier usuario desde su emisión
    OCULTA_POR_DISPUTA_EN_REVISION // Temporalmente restringida únicamente si existe expediente de arbitraje
}

/**
 * Compatibilidad con nombres previos: se mapea al esquema de visibilidad pública.
 */
enum class DoubleBlindStatus {
    PUBLICA_INMEDIATA,
    REVELADA_AMBOS_CALIFICARON,
    REVELADA_POR_VENCIMIENTO_7_DIAS,
    OCULTA_PENDIENTE_CONTRAPARTE
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
