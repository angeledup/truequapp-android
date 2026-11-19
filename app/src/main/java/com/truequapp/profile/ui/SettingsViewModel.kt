package com.truequapp.profile.ui

import androidx.lifecycle.viewModelScope
import com.truequapp.auth.data.AuthRepository
import com.truequapp.auth.data.UpdateProfileResult
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.common.mvi.UiEffect
import com.truequapp.core.common.mvi.UiEvent
import com.truequapp.core.common.mvi.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val currentEmail: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) : UiState

sealed interface SettingsUiEvent : UiEvent {
    data object LoadCurrentSettings : SettingsUiEvent
    data class UsernameChanged(val value: String) : SettingsUiEvent
    data class EmailChanged(val value: String) : SettingsUiEvent
    data class PasswordChanged(val value: String) : SettingsUiEvent
    data object TogglePasswordVisibility : SettingsUiEvent
    data object SaveSettings : SettingsUiEvent
    data object ClearMessages : SettingsUiEvent
}

sealed interface SettingsUiEffect : UiEffect {
    data class ShowToast(val message: String) : SettingsUiEffect
    data object NavigateBack : SettingsUiEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel<SettingsUiState, SettingsUiEvent, SettingsUiEffect>(SettingsUiState()) {

    init {
        loadCurrentSettings()
    }

    override fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.LoadCurrentSettings -> loadCurrentSettings()
            is SettingsUiEvent.UsernameChanged -> {
                setState { copy(username = event.value, errorMessage = null) }
            }
            is SettingsUiEvent.EmailChanged -> {
                setState { copy(email = event.value, errorMessage = null) }
            }
            is SettingsUiEvent.PasswordChanged -> {
                setState { copy(password = event.value, errorMessage = null) }
            }
            is SettingsUiEvent.TogglePasswordVisibility -> {
                setState { copy(isPasswordVisible = !isPasswordVisible) }
            }
            is SettingsUiEvent.SaveSettings -> saveSettings()
            is SettingsUiEvent.ClearMessages -> {
                setState { copy(errorMessage = null, successMessage = null) }
            }
        }
    }

    fun loadCurrentSettings() {
        viewModelScope.launch {
            setState { copy(isLoading = true, errorMessage = null) }
            val session = authRepository.currentSessionFlow.firstOrNull()
            val email = session?.userEmail.orEmpty()
            if (email.isNotBlank()) {
                val user = authRepository.getUserByEmail(email)
                if (user != null) {
                    setState {
                        copy(
                            isLoading = false,
                            currentEmail = user.correo,
                            username = user.usuario,
                            email = user.correo,
                            password = user.contrasena,
                            errorMessage = null
                        )
                    }
                    return@launch
                }
            }
            setState {
                copy(
                    isLoading = false,
                    errorMessage = "No se pudo cargar la información del usuario actual"
                )
            }
        }
    }

    private fun saveSettings() {
        val currentState = uiState.value
        if (currentState.isSaving) return

        viewModelScope.launch {
            setState { copy(isSaving = true, errorMessage = null, successMessage = null) }

            val result = authRepository.updateProfile(
                currentEmail = currentState.currentEmail,
                newUsername = currentState.username,
                newPassword = currentState.password,
                newEmail = currentState.email
            )

            when (result) {
                is UpdateProfileResult.Success -> {
                    setState {
                        copy(
                            isSaving = false,
                            currentEmail = result.updatedUser.correo,
                            username = result.updatedUser.usuario,
                            email = result.updatedUser.correo,
                            password = result.updatedUser.contrasena,
                            successMessage = "Ajustes actualizados correctamente"
                        )
                    }
                    sendEffect(SettingsUiEffect.ShowToast("Ajustes actualizados correctamente"))
                    sendEffect(SettingsUiEffect.NavigateBack)
                }
                is UpdateProfileResult.EmailAlreadyExists -> {
                    setState {
                        copy(
                            isSaving = false,
                            errorMessage = result.message
                        )
                    }
                    sendEffect(SettingsUiEffect.ShowToast(result.message))
                }
                is UpdateProfileResult.ValidationError -> {
                    setState {
                        copy(
                            isSaving = false,
                            errorMessage = result.message
                        )
                    }
                    sendEffect(SettingsUiEffect.ShowToast(result.message))
                }
                is UpdateProfileResult.Error -> {
                    setState {
                        copy(
                            isSaving = false,
                            errorMessage = result.message
                        )
                    }
                    sendEffect(SettingsUiEffect.ShowToast(result.message))
                }
            }
        }
    }
}
