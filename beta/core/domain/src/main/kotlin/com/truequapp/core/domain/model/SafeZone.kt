package com.truequapp.core.domain.model

/**
 * Punto de Encuentro Seguro certificado para entregas físicas de trueques (CU019).
 */
data class SafeZonePoint(
    val id: String,
    val name: String,
    val addressDescription: String,
    val coordinates: GeoCoordinates,
    val openingHour: Int = 8,  // Horario diurno seguro (8:00 AM)
    val closingHour: Int = 20, // Horario diurno seguro (8:00 PM)
    val hasVideoSurveillance: Boolean = true,
    val isCampusUniversityPartner: Boolean = false,
    val isActive: Boolean = true
) {
    fun isOperatingAt(hourOfDay: Int): Boolean {
        return hourOfDay in openingHour until closingHour
    }
}

/**
 * Estado de una cita presencial programada entre usuarios.
 */
enum class MeetingStatus {
    PROGRAMADA,
    EN_CURSO,
    COMPLETADA_EXITOSA,
    CANCELADA_CON_ANTICIPACION,
    CANCELADA_TARDIA_CON_SANCION,
    REPORTADA_POR_INASISTENCIA
}

/**
 * Cita presencial agendada en un Punto Seguro.
 */
data class MeetingAppointment(
    val id: String,
    val tradeId: String,
    val safeZone: SafeZonePoint,
    val scheduledTimeEpochMs: Long,
    val checkInUserA: Boolean = false,
    val checkInUserB: Boolean = false,
    val status: MeetingStatus = MeetingStatus.PROGRAMADA,
    val qrSecuritySeed: String? = null,
    val fallbackOtpPin: String? = null // PIN de 6 dígitos offline (CU020)
) {
    fun hoursUntilMeeting(currentTimeEpochMs: Long = System.currentTimeMillis()): Double {
        val diffMs = scheduledTimeEpochMs - currentTimeEpochMs
        return diffMs / (1000.0 * 60 * 60)
    }

    val isWithinCancellationGracePeriod: Boolean
        get() = hoursUntilMeeting() >= 4.0 // RN-07: Mínimo 4 horas de anticipación
}
