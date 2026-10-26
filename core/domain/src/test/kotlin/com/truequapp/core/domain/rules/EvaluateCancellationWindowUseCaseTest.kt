package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.GeoCoordinates
import com.truequapp.core.domain.model.KycLevel
import com.truequapp.core.domain.model.MeetingAppointment
import com.truequapp.core.domain.model.MeetingStatus
import com.truequapp.core.domain.model.ObfuscatedGeoLocation
import com.truequapp.core.domain.model.SafeZonePoint
import com.truequapp.core.domain.model.UserAccountStatus
import com.truequapp.core.domain.model.UserProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RN-07: Ventana de Cancelación y Penalización (Límite 4 Horas)")
class EvaluateCancellationWindowUseCaseTest {

    private lateinit var useCase: EvaluateCancellationWindowUseCase

    @BeforeEach
    fun setUp() {
        useCase = EvaluateCancellationWindowUseCase()
    }

    private fun createUser(attendanceRate: Double = 1.0, incidents: Int = 0): UserProfile {
        return UserProfile(
            id = "user_cancel_1",
            fullName = "Carlos Ruiz",
            username = "cruiz",
            email = "cruiz@example.com",
            phoneNumber = "+522281234567",
            kycLevel = KycLevel.TELEFONO_SMS_NIVEL_1,
            accountStatus = UserAccountStatus.ACTIVO,
            attendanceRate = attendanceRate,
            noShowIncidentsCount = incidents,
            location = ObfuscatedGeoLocation(
                neighborhoodName = "Las Ánimas",
                approximateCoordinates = GeoCoordinates(19.525, -96.895)
            )
        )
    }

    private fun createAppointment(scheduledTimeMs: Long): MeetingAppointment {
        val safeZone = SafeZonePoint(
            id = "sz_plaza_cristal",
            name = "Plaza Cristal - Módulo Vigilancia",
            addressDescription = "Av. Lázaro Cárdenas, Xalapa",
            coordinates = GeoCoordinates(19.528, -96.897)
        )
        return MeetingAppointment(
            id = "meet_1",
            tradeId = "trade_1",
            safeZone = safeZone,
            scheduledTimeEpochMs = scheduledTimeMs,
            status = MeetingStatus.PROGRAMADA
        )
    }

    @Test
    @DisplayName("Debe procesar cancelación sin penalización si restan más de 4 horas para la cita")
    fun shouldAllowCancellationWithoutPenalty_whenMoreThanFourHoursRemain() {
        val now = 1000000000L
        val appointmentTime = now + (5 * 60 * 60 * 1000L) // Cita dentro de 5 horas (> 4h)

        val user = createUser(attendanceRate = 0.95, incidents = 0)
        val appointment = createAppointment(appointmentTime)

        val result = useCase(
            cancelingUser = user,
            appointment = appointment,
            currentTimeEpochMs = now
        )

        assertFalse(result.isPenaltyApplied)
        assertEquals(0.0, result.penaltyDeduction)
        assertEquals(0.95, result.updatedUserProfile.attendanceRate)
        assertEquals(0, result.updatedUserProfile.noShowIncidentsCount)
    }

    @Test
    @DisplayName("Debe aplicar sanción del 5% a la tasa de asistencia si se cancela a menos de 4 horas de la cita")
    fun shouldApplyPenalty_whenLessThanFourHoursRemain() {
        val now = 1000000000L
        val appointmentTime = now + (2 * 60 * 60 * 1000L) // Cita dentro de 2 horas (< 4h)

        val user = createUser(attendanceRate = 1.0, incidents = 0)
        val appointment = createAppointment(appointmentTime)

        val result = useCase(
            cancelingUser = user,
            appointment = appointment,
            currentTimeEpochMs = now
        )

        assertTrue(result.isPenaltyApplied)
        assertEquals(0.05, result.penaltyDeduction)
        assertEquals(0.95, result.updatedUserProfile.attendanceRate, 0.001)
        assertEquals(1, result.updatedUserProfile.noShowIncidentsCount)
    }
}
