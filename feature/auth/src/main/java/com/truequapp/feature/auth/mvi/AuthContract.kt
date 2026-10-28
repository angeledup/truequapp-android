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
    val isOtpDialogVisible: Boolean = false,
    val otpCodeInput: String = "",
    val otpSecondsRemaining: Int = 300, // 5 minutos de cuenta regresiva
    val currentUserId: String = "",
    val currentUserProfile: UserProfile? = null,
    val kycLevel: KycLevel = KycLevel.BASICO_NIVEL_0,
    val truequeCoins: Double = 0.0, // Única variable para la moneda virtual
    val bayesianRating: Double = 3.0,
    val attendanceRate: Double = 1.0,
    val obfuscatedNeighborhood: String = "",
    val errorMessage: String? = null
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
    data class OtpCodeChanged(val code: String) : AuthUiEvent
    data object ToggleAuthMode : AuthUiEvent
    data object SubmitLogin : AuthUiEvent
    data object SubmitRegister : AuthUiEvent
    data object VerifyOtp : AuthUiEvent
    data object ResendOtp : AuthUiEvent
    data object DismissOtpDialog : AuthUiEvent
    data object Logout : AuthUiEvent
    data class LoadProfile(val userId: String) : AuthUiEvent
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
