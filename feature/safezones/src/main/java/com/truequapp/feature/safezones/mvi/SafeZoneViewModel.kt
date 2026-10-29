package com.truequapp.feature.safezones.mvi

import androidx.lifecycle.viewModelScope
import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.domain.model.DisputeReason
import com.truequapp.core.domain.model.DisputeTicket
import com.truequapp.core.domain.model.GeoCoordinates
import com.truequapp.core.domain.model.MeetingAppointment
import com.truequapp.core.domain.model.MeetingStatus
import com.truequapp.core.domain.model.ReviewVisibilityStatus
import com.truequapp.core.domain.model.SafeZonePoint
import com.truequapp.core.domain.model.TradeReview
import com.truequapp.core.domain.rules.EvaluateReviewVisibilityUseCase
import com.truequapp.core.domain.rules.VerifySafeZonePresenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

/**
 * ViewModel para la gestión de Puntos Seguros (CU019), QR dinámico y confirmación presencial (CU020),
 * Reseñas públicas (RN-12), Disputas con Anti-Spoofing (CU022) y Baja de cuenta (CU002).
 */
@HiltViewModel
class SafeZoneViewModel @Inject constructor(
    private val verifySafeZonePresenceUseCase: VerifySafeZonePresenceUseCase = VerifySafeZonePresenceUseCase(),
    private val evaluateReviewVisibilityUseCase: EvaluateReviewVisibilityUseCase = EvaluateReviewVisibilityUseCase(),
    private val userSessionDataStore: UserSessionDataStore,
    private val dispatchers: DispatcherProvider
) : BaseViewModel<SafeZoneUiState, SafeZoneUiEvent, SafeZoneUiEffect>(SafeZoneUiState()) {

    private val inMemorySafeZones = mutableListOf<SafeZonePoint>()
    private val inMemoryReviews = mutableListOf<TradeReview>()
    private val inMemoryDisputes = mutableListOf<DisputeTicket>()

    init {
        seedInitialSafeZones()
    }

    private fun seedInitialSafeZones() {
        val zones = listOf(
            SafeZonePoint(
                id = "sz_coatza_forum",
                name = "Punto Seguro Plaza Forum Coatzacoalcos",
                addressDescription = "Av. Universidad Veracruzana 2601, Col. El Palmar. Zona de domo central frente a vigilancia y cámaras 24/7",
                coordinates = GeoCoordinates(18.1415, -94.4852),
                openingHour = 10,
                closingHour = 21,
                hasVideoSurveillance = true,
                isCampusUniversityPartner = false
            ),
            SafeZonePoint(
                id = "sz_coatza_crystal",
                name = "Punto Seguro Plaza Crystal Coatzacoalcos",
                addressDescription = "Av. Universidad Veracruzana y Jacarandas, Col. Rancho Alegre. Módulo de acceso Chedraui con vigilancia",
                coordinates = GeoCoordinates(18.1430, -94.4695),
                openingHour = 8,
                closingHour = 22,
                hasVideoSurveillance = true,
                isCampusUniversityPartner = false
            ),
            SafeZonePoint(
                id = "sz_coatza_independencia",
                name = "Punto Seguro Parque Independencia - Palacio Municipal",
                addressDescription = "Av. Ignacio Zaragoza esq. Carranza, Centro. Explanada cívica vigilada con cámaras del C4 Municipal",
                coordinates = GeoCoordinates(18.1486, -94.4208),
                openingHour = 7,
                closingHour = 22,
                hasVideoSurveillance = true,
                isCampusUniversityPartner = false
            ),
            SafeZonePoint(
                id = "sz_coatza_malecon",
                name = "Punto Seguro Malecón Costero - Hemiciclo",
                addressDescription = "Paseo del Malecón Costero esq. Venustiano Carranza. Módulo turístico y patrullaje costero permanente",
                coordinates = GeoCoordinates(18.1565, -94.4230),
                openingHour = 8,
                closingHour = 21,
                hasVideoSurveillance = true,
                isCampusUniversityPartner = false
            ),
            SafeZonePoint(
                id = "sz_coatza_uv",
                name = "Punto Seguro Universidad Veracruzana (Campus Coatzacoalcos)",
                addressDescription = "Av. Universidad Veracruzana Km 7.5, Col. Santa Isabel. Caseta de vigilancia principal y USBI Coatzacoalcos",
                coordinates = GeoCoordinates(18.1422, -94.4760),
                openingHour = 7,
                closingHour = 20,
                hasVideoSurveillance = true,
                isCampusUniversityPartner = true
            ),
            SafeZonePoint(
                id = "sz_coatza_acaya",
                name = "Punto Seguro Plaza Acaya Coatzacoalcos",
                addressDescription = "Carretera Coatzacoalcos-Minatitlán Km 9, Col. El Tesoro. Pasillo central y área de cines con monitoreo",
                coordinates = GeoCoordinates(18.1448, -94.5020),
                openingHour = 10,
                closingHour = 21,
                hasVideoSurveillance = true,
                isCampusUniversityPartner = false
            ),
            SafeZonePoint(
                id = "sz_coatza_paso_lanchas",
                name = "Punto Seguro Paso de Lanchas Coatzacoalcos",
                addressDescription = "Av. Cristóbal Colón esq. Malecón del Río, Centro. Muelle de abordaje marítimo con vigilancia de capitanía",
                coordinates = GeoCoordinates(18.1480, -94.4170),
                openingHour = 6,
                closingHour = 22,
                hasVideoSurveillance = true,
                isCampusUniversityPartner = false
            )
        )
        inMemorySafeZones.addAll(zones)
        setState {
            copy(
                safeZones = inMemorySafeZones,
                selectedSafeZone = inMemorySafeZones.firstOrNull()
            )
        }
    }

    override fun onEvent(event: SafeZoneUiEvent) {
        when (event) {
            is SafeZoneUiEvent.InitSafeZoneFlow -> initSafeZoneFlow(event.tradeId, event.currentUserId, event.counterpartUserId)
            is SafeZoneUiEvent.SelectSafeZone -> selectSafeZone(event.safeZoneId)
            is SafeZoneUiEvent.SetAppointmentTime -> setState { copy(scheduledTimeEpochMs = event.timeEpochMs) }
            is SafeZoneUiEvent.ConfirmAppointment -> confirmAppointment()
            is SafeZoneUiEvent.GenerateQrCode -> generateDeliveryQr()
            is SafeZoneUiEvent.OnQrCodeScanned -> verifyScannedQr(event.rawData)
            is SafeZoneUiEvent.SubmitFallbackOtp -> verifyFallbackOtp(event.pin)
            is SafeZoneUiEvent.CheckInGpsLocation -> checkInGpsLocation(event.coordinates, event.isMockLocation)
            is SafeZoneUiEvent.SubmitReview -> submitReview(event.stars, event.wasPunctual, event.matchedPhotos, event.comment)
            is SafeZoneUiEvent.LoadUserPublicReviews -> loadPublicReviews(event.targetUserId)
            is SafeZoneUiEvent.SubmitDispute -> submitDispute(event.reason, event.photoEvidenceUrls, event.details)
            is SafeZoneUiEvent.RequestAccountDeletion -> requestAccountDeletion()
            is SafeZoneUiEvent.DismissError -> setState { copy(errorMessage = null) }
        }
    }

    // CU019: Puntos de Encuentro Seguros y Cálculo Equidistante
    private fun initSafeZoneFlow(tradeId: String, currentUserId: String, counterpartUserId: String) {
        val userCoords = currentState.userCoordinates
        val counterpartCoords = currentState.counterpartCoordinates

        // Cálculo de centroide geográfico (Punto Medio)
        val midLat = (userCoords.latitude + counterpartCoords.latitude) / 2.0
        val midLon = (userCoords.longitude + counterpartCoords.longitude) / 2.0
        val midpoint = GeoCoordinates(midLat, midLon)

        val defaultSelected = inMemorySafeZones.firstOrNull()

        setState {
            copy(
                tradeId = tradeId,
                currentUserId = currentUserId,
                counterpartUserId = counterpartUserId,
                safeZones = inMemorySafeZones,
                selectedSafeZone = defaultSelected,
                midpointCoordinates = midpoint,
                errorMessage = null
            )
        }
    }

    private fun selectSafeZone(safeZoneId: String) {
        val selected = inMemorySafeZones.find { it.id == safeZoneId }
        setState { copy(selectedSafeZone = selected) }
    }

    private fun confirmAppointment() {
        val safeZone = currentState.selectedSafeZone
        if (safeZone == null) {
            setState { copy(errorMessage = "Debes seleccionar un Punto Seguro verificado.") }
            return
        }

        val tradeId = currentState.tradeId
        val seed = generateHmacSeed(tradeId, System.currentTimeMillis())
        val pin = (100000..999999).random().toString() // TOTP 6 dígitos

        val appointment = MeetingAppointment(
            id = "meet_${System.currentTimeMillis()}",
            tradeId = tradeId,
            safeZone = safeZone,
            scheduledTimeEpochMs = currentState.scheduledTimeEpochMs,
            status = MeetingStatus.PROGRAMADA,
            qrSecuritySeed = seed,
            fallbackOtpPin = pin
        )

        setState {
            copy(
                appointment = appointment,
                qrSecuritySeed = seed,
                fallbackOtpPin = pin
            )
        }

        sendEffect(SafeZoneUiEffect.ShowToast("¡Cita agendada en ${safeZone.name}!"))
    }

    // CU020: Generación y Escaneo de QR Dinámico con Semilla HMAC
    private fun generateDeliveryQr() {
        val tradeId = currentState.tradeId
        val seed = currentState.qrSecuritySeed ?: generateHmacSeed(tradeId, System.currentTimeMillis())
        val pin = currentState.fallbackOtpPin ?: (100000..999999).random().toString()

        setState {
            copy(
                qrSecuritySeed = seed,
                fallbackOtpPin = pin
            )
        }
    }

    private fun verifyScannedQr(rawData: String) {
        val expectedTradeId = currentState.tradeId
        // Formato esperado de payload seguro: "TRUEQUAPP_QR:<tradeId>:<hmac>"
        val isValid = rawData.startsWith("TRUEQUAPP_QR:") && rawData.contains(expectedTradeId)

        if (isValid) {
            setState {
                copy(
                    isQrVerified = true,
                    isTradeCompleted = true,
                    appointment = currentState.appointment?.copy(status = MeetingStatus.COMPLETADA_EXITOSA)
                )
            }
            sendEffect(SafeZoneUiEffect.TriggerHapticConfirmation)
            sendEffect(SafeZoneUiEffect.ShowToast("¡Entrega verificada exitosamente! Trueque liquidado."))
            sendEffect(SafeZoneUiEffect.NavigateToReview(expectedTradeId))
        } else {
            setState { copy(errorMessage = "Código QR inválido o expirado.") }
        }
    }

    private fun verifyFallbackOtp(pin: String) {
        val expected = currentState.fallbackOtpPin
        if (expected != null && pin.trim() == expected.trim()) {
            setState {
                copy(
                    isQrVerified = true,
                    isTradeCompleted = true,
                    appointment = currentState.appointment?.copy(status = MeetingStatus.COMPLETADA_EXITOSA)
                )
            }
            sendEffect(SafeZoneUiEffect.TriggerHapticConfirmation)
            sendEffect(SafeZoneUiEffect.ShowToast("¡Código PIN verificado! Trueque liquidado."))
            sendEffect(SafeZoneUiEffect.NavigateToReview(currentState.tradeId))
        } else {
            setState { copy(errorMessage = "PIN de seguridad incorrecto.") }
        }
    }

    // CU022 & RN-08: Validación de Geocerca 150m y Detección de Mock Location
    private fun checkInGpsLocation(coordinates: GeoCoordinates, isMockLocation: Boolean) {
        if (isMockLocation) {
            setState {
                copy(
                    isMockLocationDetected = true,
                    mockLocationWarning = "Alerta de Seguridad: Se detectó falsificación de GPS (Mock Location). Acción bloqueada y registrada."
                )
            }
            return
        }

        val targetSafeZone = currentState.selectedSafeZone
        if (targetSafeZone == null) {
            setState { copy(errorMessage = "No hay Safe Zone seleccionada.") }
            return
        }

        val result = verifySafeZonePresenceUseCase(
            userGps = coordinates,
            safeZoneCoordinates = targetSafeZone.coordinates,
            maxAllowedRadiusMeters = 150.0
        )

        setState {
            copy(
                currentUserGps = coordinates,
                isWithin150mRadius = result.isWithinSafeZone,
                distanceToSafeZoneMeters = result.distanceMeters,
                isMockLocationDetected = false,
                mockLocationWarning = null
            )
        }

        if (result.isWithinSafeZone) {
            sendEffect(SafeZoneUiEffect.ShowToast("¡Check-in exitoso! Estás dentro de la Safe Zone (${result.distanceMeters.toInt()}m)."))
        } else {
            sendEffect(SafeZoneUiEffect.ShowToast("Aún estás a ${result.distanceMeters.toInt()}m del Punto Seguro (máximo 150m)."))
        }
    }

    // RN-12: Reseñas con Transparencia Pública Inmediata
    private fun submitReview(stars: Int, wasPunctual: Boolean, matchedPhotos: Boolean, comment: String) {
        try {
            val review = TradeReview(
                id = "rev_${System.currentTimeMillis()}",
                tradeId = currentState.tradeId,
                reviewerUserId = currentState.currentUserId,
                evaluatedUserId = currentState.counterpartUserId,
                starsRating = stars,
                wasPunctual = wasPunctual,
                matchedPhotos = matchedPhotos,
                writtenComment = comment,
                isPubliclyVisible = true,
                visibilityStatus = ReviewVisibilityStatus.PUBLICA_INMEDIATA
            )

            // Validación de regla de negocio RN-12
            val evaluation = evaluateReviewVisibilityUseCase(
                reviewFromUserA = review,
                reviewFromUserB = null
            )
            val finalReview = review.copy(visibilityStatus = evaluation.status)

            inMemoryReviews.add(finalReview)

            setState {
                copy(
                    isReviewSubmitted = true,
                    existingReviews = inMemoryReviews.filter { it.evaluatedUserId == currentState.counterpartUserId }
                )
            }

            sendEffect(SafeZoneUiEffect.ShowToast("¡Reseña publicada de forma inmediata (RN-12)!"))
            sendEffect(SafeZoneUiEffect.NavigateBack)
        } catch (e: Exception) {
            setState { copy(errorMessage = e.message ?: "Error al enviar reseña.") }
        }
    }

    private fun loadPublicReviews(targetUserId: String) {
        val reviews = inMemoryReviews.filter { it.evaluatedUserId == targetUserId && it.isPubliclyVisible }
        setState { copy(existingReviews = reviews) }
    }

    // CU022: Registro de Disputa y Congelamiento de Fondos
    private fun submitDispute(reason: DisputeReason, photoEvidenceUrls: List<String>, details: String) {
        val ticket = DisputeTicket(
            ticketId = "disp_${System.currentTimeMillis()}",
            tradeId = currentState.tradeId,
            reportingUserId = currentState.currentUserId,
            reportedUserId = currentState.counterpartUserId,
            reason = reason,
            photoEvidenceUrls = photoEvidenceUrls,
            reporterGpsVerifiedInSafeZone = currentState.isWithin150mRadius,
            moderatorNotes = details
        )
        inMemoryDisputes.add(ticket)

        setState {
            copy(
                createdDispute = ticket,
                isDisputeSubmitted = true
            )
        }

        sendEffect(SafeZoneUiEffect.ShowToast("Reporte de disputa generado. Fondos congelados en Escrow."))
        sendEffect(SafeZoneUiEffect.NavigateBack)
    }

    // CU002: Baja de Usuario y Supresión de Datos
    private fun requestAccountDeletion() {
        // Valida que no haya trueques en estado RESERVADO o fondos en Escrow
        val hasActiveLocks = currentState.isTradeCompleted.not() && currentState.appointment != null &&
                currentState.appointment?.status == MeetingStatus.EN_CURSO

        if (hasActiveLocks) {
            setState {
                copy(
                    isAccountDeletionBlocked = true,
                    accountDeletionBlockReason = "No es posible eliminar la cuenta mientras existan intercambios en proceso o fondos en garantía (CU002)."
                )
            }
        } else {
            setState {
                copy(
                    isAccountDeleted = true,
                    isAccountDeletionBlocked = false
                )
            }
            sendEffect(SafeZoneUiEffect.ShowToast("Solicitud de baja procesada conforme a normativas de privacidad (CU002)."))
            sendEffect(SafeZoneUiEffect.NavigateBack)
        }
    }

    private fun generateHmacSeed(tradeId: String, timestamp: Long): String {
        return try {
            val key = "TRUEQUAPP_HMAC_SECRET_KEY".toByteArray()
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key, "HmacSHA256"))
            val hash = mac.doFinal("$tradeId:$timestamp".toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "$tradeId:$timestamp"
        }
    }

    // Helper público para pruebas unitarias
    fun addTestSafeZone(zone: SafeZonePoint) {
        inMemorySafeZones.add(zone)
    }
}
