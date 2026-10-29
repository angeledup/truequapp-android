package com.truequapp.feature.safezones.mvi

import com.truequapp.core.common.mvi.UiEffect
import com.truequapp.core.common.mvi.UiEvent
import com.truequapp.core.common.mvi.UiState
import com.truequapp.core.domain.model.DisputeReason
import com.truequapp.core.domain.model.DisputeTicket
import com.truequapp.core.domain.model.GeoCoordinates
import com.truequapp.core.domain.model.MeetingAppointment
import com.truequapp.core.domain.model.SafeZonePoint
import com.truequapp.core.domain.model.TradeReview

/**
 * Estado inmutable de la interfaz de usuario para Puntos Seguros, QR y Disputas (Fase 3).
 */
data class SafeZoneUiState(
    val tradeId: String = "",
    val currentUserId: String = "",
    val counterpartUserId: String = "",

    // Safe Exchange Zones & Map (CU019)
    val safeZones: List<SafeZonePoint> = emptyList(),
    val selectedSafeZone: SafeZonePoint? = null,
    val userCoordinates: GeoCoordinates = GeoCoordinates(19.4326, -99.1332),
    val counterpartCoordinates: GeoCoordinates = GeoCoordinates(19.4380, -99.1400),
    val midpointCoordinates: GeoCoordinates = GeoCoordinates(19.4353, -99.1366),
    val scheduledTimeEpochMs: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000L),
    val appointment: MeetingAppointment? = null,

    // Confirmación Presencial con QR & Fallback TOTP PIN (CU020)
    val qrSecuritySeed: String? = null,
    val fallbackOtpPin: String? = null,
    val isQrScanningActive: Boolean = false,
    val isQrVerified: Boolean = false,
    val isTradeCompleted: Boolean = false,

    // Detección Anti-Spoofing & Geocerca 150m (CU022 / RN-08)
    val currentUserGps: GeoCoordinates? = null,
    val isWithin150mRadius: Boolean = false,
    val distanceToSafeZoneMeters: Double = 0.0,
    val isMockLocationDetected: Boolean = false,
    val mockLocationWarning: String? = null,

    // Reseñas y Transparencia Pública (RN-12)
    val existingReviews: List<TradeReview> = emptyList(),
    val isReviewSubmitted: Boolean = false,

    // Disputas de Escrow (CU022)
    val createdDispute: DisputeTicket? = null,
    val isDisputeSubmitted: Boolean = false,

    // Baja de Usuario y Supresión de Datos (CU002)
    val isAccountDeletionBlocked: Boolean = false,
    val accountDeletionBlockReason: String? = null,
    val isAccountDeleted: Boolean = false,

    // Estados generales
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : UiState

/**
 * Intenciones y eventos del usuario para la Fase 3.
 */
sealed interface SafeZoneUiEvent : UiEvent {
    // CU019: Safe Zones y Mapa
    data class InitSafeZoneFlow(
        val tradeId: String,
        val currentUserId: String,
        val counterpartUserId: String
    ) : SafeZoneUiEvent
    data class SelectSafeZone(val safeZoneId: String) : SafeZoneUiEvent
    data class SetAppointmentTime(val timeEpochMs: Long) : SafeZoneUiEvent
    data object ConfirmAppointment : SafeZoneUiEvent

    // CU020: QR Dinámico y Validación
    data object GenerateQrCode : SafeZoneUiEvent
    data class OnQrCodeScanned(val rawData: String) : SafeZoneUiEvent
    data class SubmitFallbackOtp(val pin: String) : SafeZoneUiEvent

    // CU022 & RN-08: Geocerca y Anti-Mock
    data class CheckInGpsLocation(
        val coordinates: GeoCoordinates,
        val isMockLocation: Boolean
    ) : SafeZoneUiEvent

    // RN-12: Sistema de Reseñas Públicas
    data class SubmitReview(
        val stars: Int,
        val wasPunctual: Boolean,
        val matchedPhotos: Boolean,
        val comment: String
    ) : SafeZoneUiEvent
    data class LoadUserPublicReviews(val targetUserId: String) : SafeZoneUiEvent

    // CU022: Disputas
    data class SubmitDispute(
        val reason: DisputeReason,
        val photoEvidenceUrls: List<String>,
        val details: String
    ) : SafeZoneUiEvent

    // CU002: Baja de Usuario
    data object RequestAccountDeletion : SafeZoneUiEvent

    data object DismissError : SafeZoneUiEvent
}

/**
 * Efectos secundarios de navegación y feedback en UI.
 */
sealed interface SafeZoneUiEffect : UiEffect {
    data object NavigateBack : SafeZoneUiEffect
    data class NavigateToReview(val tradeId: String) : SafeZoneUiEffect
    data class ShowToast(val message: String) : SafeZoneUiEffect
    data object TriggerHapticConfirmation : SafeZoneUiEffect
}

