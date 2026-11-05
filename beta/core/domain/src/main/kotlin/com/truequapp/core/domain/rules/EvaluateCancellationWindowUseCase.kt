package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.MeetingAppointment
import com.truequapp.core.domain.model.UserProfile

/**
 * Evaluación del resultado de una solicitud de cancelación de cita.
 */
data class CancellationEvaluationResult(
    val isPenaltyApplied: Boolean,
    val updatedUserProfile: UserProfile,
    val penaltyDeduction: Double,
    val message: String
)

/**
 * Caso de uso: Evaluación de la Ventana de Cancelación de 4 Horas (RN-07 / CU012).
 * Desincentiva cancelaciones imprevistas protegiendo el tiempo de los usuarios.
 */
class EvaluateCancellationWindowUseCase {

    /**
     * Evalúa si una cancelación aplica sanción a la reputación y tasa de cumplimiento.
     *
     * @param cancelingUser Perfil del usuario que solicita la cancelación.
     * @param appointment Cita acordada en el Punto Seguro.
     * @param currentTimeEpochMs Momento actual en milisegundos.
     * @return [CancellationEvaluationResult] con las métricas recalculadas.
     */
    operator fun invoke(
        cancelingUser: UserProfile,
        appointment: MeetingAppointment,
        currentTimeEpochMs: Long = System.currentTimeMillis()
    ): CancellationEvaluationResult {
        val hoursRemaining = appointment.hoursUntilMeeting(currentTimeEpochMs)

        return if (hoursRemaining >= CANCELLATION_WINDOW_HOURS) {
            // Cancelación legítima sin penalización
            CancellationEvaluationResult(
                isPenaltyApplied = false,
                updatedUserProfile = cancelingUser,
                penaltyDeduction = 0.0,
                message = "Cancelación procesada con anticipación (${String.format("%.1f", hoursRemaining)}h antes). No se aplican penalizaciones."
            )
        } else {
            // Cancelación tardía (< 4 horas antes): Se penaliza la tasa de asistencia
            val newIncidentsCount = cancelingUser.noShowIncidentsCount + 1
            // Reducción del 5% (0.05) en la tasa de asistencia por cancelación tardía
            val newAttendanceRate = Math.max(0.0, cancelingUser.attendanceRate - 0.05)

            val updatedUser = cancelingUser.copy(
                attendanceRate = newAttendanceRate,
                noShowIncidentsCount = newIncidentsCount
            )

            CancellationEvaluationResult(
                isPenaltyApplied = true,
                updatedUserProfile = updatedUser,
                penaltyDeduction = 0.05,
                message = "Cancelación tardía a solo ${String.format("%.1f", hoursRemaining)}h de la cita. Se aplicó una penalización del 5% a tu tasa de asistencia."
            )
        }
    }

    companion object {
        const val CANCELLATION_WINDOW_HOURS = 4.0 // 4 horas según RN-07 de la UV
    }
}
