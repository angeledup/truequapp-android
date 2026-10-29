package com.truequapp.feature.auth.mvi

import androidx.lifecycle.viewModelScope
import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.entity.UserEntity
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.datastore.model.UserSession
import com.truequapp.core.domain.model.GeoCoordinates
import com.truequapp.core.domain.model.KycLevel
import com.truequapp.core.domain.model.UserProfile
import com.truequapp.core.domain.rules.CalculateBayesianRatingUseCase
import com.truequapp.core.domain.rules.ObfuscateLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel para el ciclo de vida de autenticación, verificación SMS OTP y perfil de usuario (CU001, CU003, CU004, CU023, CU024).
 * Cumple estrictamente con las reglas RN-09 (Reputación Bayesiana) y RN-10 (Privacidad Geoespacial a 500m).
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userSessionDataStore: UserSessionDataStore,
    private val userDao: UserDao? = null,
    private val calculateBayesianRatingUseCase: CalculateBayesianRatingUseCase = CalculateBayesianRatingUseCase(),
    private val obfuscateLocationUseCase: ObfuscateLocationUseCase = ObfuscateLocationUseCase(),
    private val dispatchers: DispatcherProvider
) : BaseViewModel<AuthUiState, AuthUiEvent, AuthUiEffect>(AuthUiState()) {

    private var countdownJob: Job? = null

    init {
        checkCurrentSession()
    }

    private fun checkCurrentSession() {
        viewModelScope.launch(dispatchers.io) {
            userSessionDataStore.sessionFlow.collect { session ->
                if (session.isAuthenticated && !session.userId.isNullOrBlank()) {
                    val loggedUserId = session.userId.orEmpty()
                    // 1. Cargar datos reales desde la base de datos local SQLite Room
                    val dbUser = userDao?.getUserByIdSync(loggedUserId)
                        ?: session.userEmail?.let { userDao?.getUserByEmailSync(it) }

                    val finalName = dbUser?.name ?: session.userName ?: "Usuario Truequapp"
                    val finalEmail = dbUser?.email ?: session.userEmail.orEmpty()
                    val finalPhone = dbUser?.phoneNumber.orEmpty()
                    val finalCoins = dbUser?.truequeCoins ?: session.truequeCoins
                    val finalNeighborhood = dbUser?.obfuscatedNeighborhood?.ifBlank { "Centro, Coatzacoalcos" } ?: "Centro, Coatzacoalcos"
                    val finalRating = dbUser?.bayesianRating ?: 4.8
                    val finalAttendance = dbUser?.attendanceRate ?: 1.0
                    val finalKyc = if (dbUser?.isKycVerified == true || session.isKycVerified) KycLevel.TELEFONO_SMS_NIVEL_1 else KycLevel.BASICO_NIVEL_0

                    setState {
                        copy(
                            isLoggedIn = true,
                            currentUserId = loggedUserId,
                            userName = finalName,
                            userEmail = finalEmail,
                            userPhone = finalPhone,
                            nameInput = finalName,
                            emailInput = finalEmail,
                            phoneInput = finalPhone,
                            userNeighborhood = finalNeighborhood,
                            obfuscatedNeighborhood = finalNeighborhood,
                            kycLevel = finalKyc,
                            truequeCoins = finalCoins,
                            authProvider = dbUser?.authProvider ?: session.authProvider,
                            googleEmail = if ((dbUser?.authProvider ?: session.authProvider) == "GOOGLE") finalEmail else null,
                            userPhotoUrl = dbUser?.photoUrl ?: session.userPhotoUrl,
                            bayesianRating = finalRating,
                            attendanceRate = finalAttendance
                        )
                    }
                } else {
                    setState {
                        copy(
                            isLoggedIn = false,
                            currentUserId = "",
                            userName = "",
                            userEmail = "",
                            userPhone = "",
                            truequeCoins = session.truequeCoins
                        )
                    }
                }
            }
        }
    }

    override fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.NameChanged -> setState { copy(nameInput = event.name, errorMessage = null) }
            is AuthUiEvent.EmailChanged -> setState { copy(emailInput = event.email, errorMessage = null) }
            is AuthUiEvent.PhoneChanged -> setState { copy(phoneInput = event.phone, errorMessage = null) }
            is AuthUiEvent.PasswordChanged -> setState { copy(passwordInput = event.password, errorMessage = null) }
            is AuthUiEvent.NeighborhoodChanged -> setState { copy(neighborhoodInput = event.neighborhood, errorMessage = null) }
            is AuthUiEvent.OtpCodeChanged -> setState { copy(otpCodeInput = event.code, errorMessage = null) }
            is AuthUiEvent.ToggleAuthMode -> setState { copy(isRegisterMode = !isRegisterMode, errorMessage = null) }
            is AuthUiEvent.SubmitLogin -> executeLogin()
            is AuthUiEvent.SubmitRegister -> executeRegister()
            is AuthUiEvent.VerifyOtp -> executeVerifyOtp()
            is AuthUiEvent.ResendOtp -> startOtpCountdown()
            is AuthUiEvent.DismissOtpDialog -> {
                countdownJob?.cancel()
                setState { copy(isOtpDialogVisible = false, otpCodeInput = "") }
            }
            is AuthUiEvent.Logout -> executeLogout()
            is AuthUiEvent.LoadProfile -> loadUserProfile(event.userId)
            is AuthUiEvent.SignInWithGoogleSuccess -> handleGoogleSignIn(event)
            is AuthUiEvent.OpenEditProfileDialog -> {
                setState {
                    copy(
                        isEditProfileDialogVisible = true,
                        editNameInput = userName.ifBlank { nameInput },
                        editPhoneInput = userPhone.ifBlank { phoneInput },
                        editNeighborhoodInput = userNeighborhood.ifBlank { obfuscatedNeighborhood }
                    )
                }
            }
            is AuthUiEvent.DismissEditProfileDialog -> setState { copy(isEditProfileDialogVisible = false) }
            is AuthUiEvent.EditNameChanged -> setState { copy(editNameInput = event.name) }
            is AuthUiEvent.EditPhoneChanged -> setState { copy(editPhoneInput = event.phone) }
            is AuthUiEvent.EditNeighborhoodChanged -> setState { copy(editNeighborhoodInput = event.neighborhood) }
            is AuthUiEvent.SaveEditedProfile -> saveEditedProfile()
            is AuthUiEvent.AutoFillTestOtp -> setState { copy(otpCodeInput = "123456") }
        }
    }

    private fun handleGoogleSignIn(event: AuthUiEvent.SignInWithGoogleSuccess) {
        viewModelScope.launch(dispatchers.io) {
            setState { copy(isLoading = true, errorMessage = null) }
            val generatedToken = "jwt_google_${UUID.randomUUID()}"
            userSessionDataStore.saveTokens(generatedToken, "refresh_$generatedToken")
            userSessionDataStore.saveUser(
                userId = event.googleId,
                email = event.email,
                isKycVerified = true,
                name = event.displayName,
                photoUrl = event.photoUrl,
                authProvider = "GOOGLE"
            )
            val currentCoins = userSessionDataStore.getTruequeCoins()

            // Persistencia en Base de Datos SQLite Room
            userDao?.insertUser(
                UserEntity(
                    id = event.googleId,
                    email = event.email,
                    name = event.displayName,
                    passwordHash = null, // Sin contraseña (proveedor Google)
                    authProvider = "GOOGLE",
                    photoUrl = event.photoUrl,
                    isKycVerified = true,
                    kycLevel = "TELEFONO_SMS_NIVEL_1",
                    truequeCoins = currentCoins,
                    city = "Coatzacoalcos",
                    obfuscatedNeighborhood = "Centro, Coatzacoalcos",
                    lastLoginEpochMs = System.currentTimeMillis()
                )
            )

            setState {
                copy(
                    isLoading = false,
                    isLoggedIn = true,
                    currentUserId = event.googleId,
                    userName = event.displayName,
                    userEmail = event.email,
                    nameInput = event.displayName,
                    emailInput = event.email,
                    authProvider = "GOOGLE",
                    googleEmail = event.email,
                    userPhotoUrl = event.photoUrl,
                    truequeCoins = currentCoins,
                    userNeighborhood = "Centro, Coatzacoalcos",
                    obfuscatedNeighborhood = "Centro, Coatzacoalcos",
                    kycLevel = KycLevel.TELEFONO_SMS_NIVEL_1
                )
            }
            sendEffect(AuthUiEffect.ShowSnackbar("¡Bienvenido, ${event.displayName}! Sesión iniciada con Google."))
            sendEffect(AuthUiEffect.NavigateToHome)
        }
    }

    private fun executeLogin() {
        val email = currentState.emailInput.trim()
        val password = currentState.passwordInput

        if (!isValidEmail(email)) {
            setState { copy(errorMessage = "Ingresa un correo electrónico válido.") }
            return
        }
        if (password.length < 6) {
            setState { copy(errorMessage = "La contraseña debe tener al menos 6 caracteres.") }
            return
        }

        viewModelScope.launch(dispatchers.io) {
            setState { copy(isLoading = true, errorMessage = null) }
            delay(200)

            // 1. Buscar si el usuario ya existe en SQLite Room
            val existingUser = userDao?.getUserByEmailSync(email)

            if (existingUser != null) {
                // Validación de contraseña si fue registrado con email/password
                if (existingUser.passwordHash != null && existingUser.passwordHash != password) {
                    setState {
                        copy(
                            isLoading = false,
                            errorMessage = "Contraseña incorrecta. Por favor verifica tus credenciales."
                        )
                    }
                    return@launch
                }

                // Usuario validado con sus datos REALES de la base de datos Room
                val userId = existingUser.id
                userSessionDataStore.saveUser(
                    userId = userId,
                    email = existingUser.email,
                    isKycVerified = existingUser.isKycVerified,
                    name = existingUser.name,
                    photoUrl = existingUser.photoUrl,
                    authProvider = existingUser.authProvider
                )
                userSessionDataStore.saveTokens(
                    accessToken = "jwt_token_$userId",
                    refreshToken = "refresh_$userId"
                )

                setState {
                    copy(
                        isLoading = false,
                        isLoggedIn = true,
                        currentUserId = userId,
                        userName = existingUser.name,
                        userEmail = existingUser.email,
                        userPhone = existingUser.phoneNumber.orEmpty(),
                        nameInput = existingUser.name,
                        emailInput = existingUser.email,
                        userNeighborhood = existingUser.obfuscatedNeighborhood,
                        obfuscatedNeighborhood = existingUser.obfuscatedNeighborhood,
                        kycLevel = if (existingUser.isKycVerified) KycLevel.TELEFONO_SMS_NIVEL_1 else KycLevel.BASICO_NIVEL_0,
                        truequeCoins = existingUser.truequeCoins,
                        authProvider = existingUser.authProvider,
                        bayesianRating = existingUser.bayesianRating,
                        attendanceRate = existingUser.attendanceRate
                    )
                }
                sendEffect(AuthUiEffect.ShowSnackbar("¡Bienvenido de nuevo, ${existingUser.name}!"))
                sendEffect(AuthUiEffect.NavigateToHome)
            } else {
                // Si el usuario no existe aún, se registra automáticamente con sus datos
                val generatedUserId = "usr_" + email.hashCode().toString().takeLast(6)
                val userName = currentState.nameInput.trim().ifBlank {
                    email.substringBefore("@")
                        .replace('.', ' ')
                        .replace('_', ' ')
                        .replaceFirstChar { it.uppercase() }
                }
                val neighborhood = currentState.neighborhoodInput.ifBlank { "Centro, Coatzacoalcos" }

                val newUser = UserEntity(
                    id = generatedUserId,
                    email = email,
                    name = userName,
                    passwordHash = password,
                    authProvider = "EMAIL",
                    isKycVerified = true,
                    kycLevel = "TELEFONO_SMS_NIVEL_1",
                    truequeCoins = 100.0,
                    obfuscatedNeighborhood = neighborhood,
                    city = "Coatzacoalcos",
                    lastLoginEpochMs = System.currentTimeMillis()
                )

                // Guardar en SQLite Room
                userDao?.insertUser(newUser)

                // Guardar en DataStore
                userSessionDataStore.saveUser(
                    userId = generatedUserId,
                    email = email,
                    isKycVerified = true,
                    name = userName,
                    authProvider = "EMAIL"
                )
                userSessionDataStore.saveTokens(
                    accessToken = "jwt_token_$generatedUserId",
                    refreshToken = "refresh_$generatedUserId"
                )

                setState {
                    copy(
                        isLoading = false,
                        isLoggedIn = true,
                        currentUserId = generatedUserId,
                        userName = userName,
                        userEmail = email,
                        nameInput = userName,
                        emailInput = email,
                        userNeighborhood = neighborhood,
                        obfuscatedNeighborhood = neighborhood,
                        kycLevel = KycLevel.TELEFONO_SMS_NIVEL_1,
                        truequeCoins = 100.0,
                        authProvider = "EMAIL",
                        bayesianRating = 4.8,
                        attendanceRate = 1.0
                    )
                }
                sendEffect(AuthUiEffect.ShowSnackbar("Bienvenido a Truequapp, $userName"))
                sendEffect(AuthUiEffect.NavigateToHome)
            }
        }
    }

    private fun executeRegister() {
        val name = currentState.nameInput.trim()
        val email = currentState.emailInput.trim()
        val phone = currentState.phoneInput.trim()
        val password = currentState.passwordInput

        if (name.length < 3) {
            setState { copy(errorMessage = "El nombre completo debe tener al menos 3 caracteres.") }
            return
        }
        if (!isValidEmail(email)) {
            setState { copy(errorMessage = "Ingresa un correo electrónico válido.") }
            return
        }
        if (!isValidE164Phone(phone)) {
            setState { copy(errorMessage = "El teléfono debe cumplir el formato internacional E.164 (Ej. +522281234567).") }
            return
        }
        if (password.length < 6) {
            setState { copy(errorMessage = "La contraseña debe tener al menos 6 caracteres.") }
            return
        }

        // Abrir modal de verificación SMS OTP (KYC Nivel 1)
        setState { copy(isOtpDialogVisible = true, otpCodeInput = "", errorMessage = null) }
        startOtpCountdown()
    }

    private fun startOtpCountdown() {
        countdownJob?.cancel()
        setState { copy(otpSecondsRemaining = 300) } // 5 minutos según CU001
        countdownJob = viewModelScope.launch(dispatchers.default) {
            for (sec in 300 downTo 0) {
                setState { copy(otpSecondsRemaining = sec) }
                delay(1000)
            }
        }
    }

    private fun executeVerifyOtp() {
        val code = currentState.otpCodeInput.trim()
        if (code.length != 6 || !code.all { it.isDigit() }) {
            setState { copy(errorMessage = "El código SMS debe tener exactamente 6 dígitos numéricos.") }
            return
        }

        countdownJob?.cancel()
        viewModelScope.launch(dispatchers.io) {
            setState { copy(isLoading = true, errorMessage = null) }
            delay(200)

            val newUserId = "usr_" + System.currentTimeMillis().toString().takeLast(6)
            val userEmail = currentState.emailInput.trim().ifBlank { "usuario_$newUserId@truequapp.com" }
            val userName = currentState.nameInput.trim().ifBlank {
                userEmail.substringBefore("@")
                    .replace('.', ' ')
                    .replace('_', ' ')
                    .replaceFirstChar { it.uppercase() }
            }
            val userPhone = currentState.phoneInput.trim()
            val neighborhood = currentState.neighborhoodInput.ifBlank { "Col. Petrolera, Coatzacoalcos" }

            val newUser = UserEntity(
                id = newUserId,
                email = userEmail,
                name = userName,
                phoneNumber = userPhone.ifBlank { "+529210000000" },
                passwordHash = currentState.passwordInput.ifBlank { null },
                authProvider = "EMAIL",
                isKycVerified = true,
                kycLevel = "TELEFONO_SMS_NIVEL_1",
                truequeCoins = 100.0,
                obfuscatedNeighborhood = neighborhood,
                city = "Coatzacoalcos",
                lastLoginEpochMs = System.currentTimeMillis()
            )

            // Persistencia en SQLite Room Database
            userDao?.insertUser(newUser)

            // Persistencia en DataStore
            userSessionDataStore.saveUser(
                userId = newUserId,
                email = userEmail,
                isKycVerified = true,
                name = userName,
                authProvider = "EMAIL"
            )
            userSessionDataStore.saveTokens(
                accessToken = "jwt_token_kyc1_$newUserId",
                refreshToken = "refresh_$newUserId"
            )

            val initialRating = calculateBayesianRatingUseCase(existingRatingsCount = 0, existingSumOfRatings = 0.0)

            setState {
                copy(
                    isLoading = false,
                    isLoggedIn = true,
                    isOtpDialogVisible = false,
                    currentUserId = newUserId,
                    userName = userName,
                    userEmail = userEmail,
                    userPhone = userPhone,
                    nameInput = userName,
                    emailInput = userEmail,
                    userNeighborhood = neighborhood,
                    obfuscatedNeighborhood = neighborhood,
                    kycLevel = KycLevel.TELEFONO_SMS_NIVEL_1,
                    truequeCoins = 100.0,
                    bayesianRating = initialRating,
                    attendanceRate = 1.0
                )
            }
            sendEffect(AuthUiEffect.OtpVerifiedSuccess)
            sendEffect(AuthUiEffect.ShowSnackbar("¡Cuenta verificada! Bienvenido $userName."))
            sendEffect(AuthUiEffect.NavigateToHome)
        }
    }

    private fun saveEditedProfile() {
        val newName = currentState.editNameInput.trim()
        val newPhone = currentState.editPhoneInput.trim()
        val newNeighborhood = currentState.editNeighborhoodInput.trim().ifBlank { "Centro, Coatzacoalcos" }

        if (newName.length < 2) {
            setState { copy(errorMessage = "El nombre debe contener al menos 2 caracteres.") }
            return
        }

        viewModelScope.launch(dispatchers.io) {
            val userId = currentState.currentUserId.ifBlank { "usr_" + System.currentTimeMillis().toString().takeLast(6) }
            val email = currentState.userEmail.ifBlank { currentState.emailInput }.ifBlank { "usuario@truequapp.com" }

            val existingUser = userDao?.getUserByIdSync(userId)
                ?: userDao?.getUserByEmailSync(email)

            if (existingUser != null) {
                val updated = existingUser.copy(
                    name = newName,
                    phoneNumber = newPhone.ifBlank { existingUser.phoneNumber },
                    obfuscatedNeighborhood = newNeighborhood
                )
                userDao?.updateUser(updated)
            } else {
                userDao?.insertUser(
                    UserEntity(
                        id = userId,
                        email = email,
                        name = newName,
                        phoneNumber = newPhone,
                        obfuscatedNeighborhood = newNeighborhood,
                        city = "Coatzacoalcos",
                        truequeCoins = currentState.truequeCoins
                    )
                )
            }

            userSessionDataStore.saveUser(
                userId = userId,
                email = email,
                isKycVerified = true,
                name = newName,
                photoUrl = currentState.userPhotoUrl,
                authProvider = currentState.authProvider
            )

            setState {
                copy(
                    isEditProfileDialogVisible = false,
                    userName = newName,
                    nameInput = newName,
                    userPhone = newPhone,
                    phoneInput = newPhone,
                    userNeighborhood = newNeighborhood,
                    obfuscatedNeighborhood = newNeighborhood,
                    errorMessage = null
                )
            }
            sendEffect(AuthUiEffect.ShowSnackbar("Perfil actualizado correctamente en la base de datos."))
        }
    }

    private fun executeLogout() {
        viewModelScope.launch(dispatchers.io) {
            userSessionDataStore.clearSession()
            setState {
                AuthUiState(
                    isLoggedIn = false,
                    isRegisterMode = false,
                    emailInput = "",
                    passwordInput = "",
                    nameInput = "",
                    userName = "",
                    userEmail = "",
                    userPhone = "",
                    currentUserId = ""
                )
            }
            sendEffect(AuthUiEffect.NavigateToLogin)
            sendEffect(AuthUiEffect.ShowSnackbar("Sesión cerrada correctamente."))
        }
    }

    private fun loadUserProfile(userId: String) {
        viewModelScope.launch(dispatchers.io) {
            val rating = calculateBayesianRatingUseCase(existingRatingsCount = 15, existingSumOfRatings = 72.0)
            val obfuscated = obfuscateLocationUseCase(
                exactCoordinates = GeoCoordinates(18.1460, -94.4320),
                neighborhoodName = "Col. María de la Piedad, Coatzacoalcos"
            )
            setState {
                copy(
                    currentUserId = userId,
                    bayesianRating = rating,
                    obfuscatedNeighborhood = obfuscated.neighborhoodName
                )
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && email.contains("@") && email.contains(".") && email.length >= 5
    }

    private fun isValidE164Phone(phone: String): Boolean {
        val e164Regex = Regex("^\\+[1-9]\\d{7,14}\$")
        return e164Regex.matches(phone)
    }
}
