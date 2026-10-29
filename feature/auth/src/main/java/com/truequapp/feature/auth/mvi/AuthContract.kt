package com.truequapp.feature.auth.mvi

import com.truequapp.core.common.mvi.UiEffect
import com.truequapp.core.common.mvi.UiEvent
import com.truequapp.core.common.mvi.UiState
import com.truequapp.core.domain.model.KycLevel
import com.truequapp.core.domain.model.UserProfile

/**
 * Estado inmutable de la interfaz de usuario para el módulo de Autenticación y Perfil.
 * Utiliza una única variable canónica para la moneda virtual: [truequeCoins].
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isRegisterMode: Boolean = false,
    val nameInput: String = "",
    val emailInput: String = "",
    val phoneInput: String = "",
    val passwordInput: String = "",
    val neighborhoodInput: String = "Centro, Coatzacoalcos",
    val isOtpDialogVisible: Boolean = false,
    val otpCodeInput: String = "",
    val otpSecondsRemaining: Int = 300, // 5 minutos de cuenta regresiva
    val currentUserId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val userNeighborhood: String = "Centro, Coatzacoalcos",
    val userCity: String = "Coatzacoalcos",
    val currentUserProfile: UserProfile? = null,
    val kycLevel: KycLevel = KycLevel.BASICO_NIVEL_0,
    val truequeCoins: Double = 100.0, // Única variable para la moneda virtual
    val bayesianRating: Double = 4.8,
    val attendanceRate: Double = 1.0,
    val obfuscatedNeighborhood: String = "Centro, Coatzacoalcos",
    val authProvider: String = "EMAIL",
    val googleEmail: String? = null,
    val userPhotoUrl: String? = null,
    val errorMessage: String? = null,
    // Diálogo de Edición de Perfil (CU003)
    val isEditProfileDialogVisible: Boolean = false,
    val editNameInput: String = "",
    val editPhoneInput: String = "",
    val editNeighborhoodInput: String = ""
) : UiState {
    // Compatibilidad retroactiva
    val walletBalanceCoins: Double
        get() = truequeCoins
}

/**
 * Eventos e intenciones del usuario disparados desde las pantallas de autenticación y perfil.
 */
sealed interface AuthUiEvent : UiEvent {
    data class NameChanged(val name: String) : AuthUiEvent
    data class EmailChanged(val email: String) : AuthUiEvent
    data class PhoneChanged(val phone: String) : AuthUiEvent
    data class PasswordChanged(val password: String) : AuthUiEvent
    data class NeighborhoodChanged(val neighborhood: String) : AuthUiEvent
    data class OtpCodeChanged(val code: String) : AuthUiEvent
    data object ToggleAuthMode : AuthUiEvent
    data object SubmitLogin : AuthUiEvent
    data object SubmitRegister : AuthUiEvent
    data object VerifyOtp : AuthUiEvent
    data object ResendOtp : AuthUiEvent
    data object DismissOtpDialog : AuthUiEvent
    data object Logout : AuthUiEvent
    data class LoadProfile(val userId: String) : AuthUiEvent
    data class SignInWithGoogleSuccess(
        val googleId: String,
        val email: String,
        val displayName: String,
        val photoUrl: String?
    ) : AuthUiEvent
    data class SignInWithGoogleFailure(val errorMessage: String) : AuthUiEvent

    // Edición de Perfil y persistencia directa en Base de Datos Room
    data object OpenEditProfileDialog : AuthUiEvent
    data object DismissEditProfileDialog : AuthUiEvent
    data class EditNameChanged(val name: String) : AuthUiEvent
    data class EditPhoneChanged(val phone: String) : AuthUiEvent
    data class EditNeighborhoodChanged(val neighborhood: String) : AuthUiEvent
    data object SaveEditedProfile : AuthUiEvent
    data object AutoFillTestOtp : AuthUiEvent
}

/**
 * Efectos de navegación y notificaciones efímeras (Toasts / Snackbars).
 */
sealed interface AuthUiEffect : UiEffect {
    data class ShowSnackbar(val message: String) : AuthUiEffect
    data object NavigateToHome : AuthUiEffect
    data object NavigateToLogin : AuthUiEffect
    data object OtpVerifiedSuccess : AuthUiEffect
}
