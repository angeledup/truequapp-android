package com.truequapp.auth.ui

import androidx.lifecycle.viewModelScope
import com.truequapp.auth.data.AuthRepository
import com.truequapp.auth.data.RegisterResult
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.common.mvi.UiEffect
import com.truequapp.core.common.mvi.UiEvent
import com.truequapp.core.common.mvi.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val usuario: String = "",
    val numeroTelefonico: String = "",
    val correo: String = "",
    val contrasena: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegisteredSuccessfully: Boolean = false
) : UiState

sealed interface RegisterUiEvent : UiEvent {
    data class UsuarioChanged(val value: String) : RegisterUiEvent
    data class NumeroTelefonicoChanged(val value: String) : RegisterUiEvent
    data class CorreoChanged(val value: String) : RegisterUiEvent
    data class ContrasenaChanged(val value: String) : RegisterUiEvent
    data object TogglePasswordVisibility : RegisterUiEvent
    data object SubmitRegister : RegisterUiEvent
    data object ClearError : RegisterUiEvent
}

sealed interface RegisterUiEffect : UiEffect {
    data class NavigateToLogin(val welcomeMessage: String) : RegisterUiEffect
    data class ShowToast(val message: String) : RegisterUiEffect
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel<RegisterUiState, RegisterUiEvent, RegisterUiEffect>(RegisterUiState()) {

    override fun onEvent(event: RegisterUiEvent) {
        when (event) {
            is RegisterUiEvent.UsuarioChanged -> {
                setState { copy(usuario = event.value, errorMessage = null) }
            }
            is RegisterUiEvent.NumeroTelefonicoChanged -> {
                setState { copy(numeroTelefonico = event.value, errorMessage = null) }
            }
            is RegisterUiEvent.CorreoChanged -> {
                setState { copy(correo = event.value, errorMessage = null) }
            }
            is RegisterUiEvent.ContrasenaChanged -> {
                setState { copy(contrasena = event.value, errorMessage = null) }
            }
            is RegisterUiEvent.TogglePasswordVisibility -> {
                setState { copy(isPasswordVisible = !isPasswordVisible) }
            }
            is RegisterUiEvent.ClearError -> {
                setState { copy(errorMessage = null) }
            }
            is RegisterUiEvent.SubmitRegister -> {
                register()
            }
        }
    }

    private fun register() {
        val state = uiState.value

        setState { copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = authRepository.register(
                usuario = state.usuario,
                numeroTelefonico = state.numeroTelefonico,
                correo = state.correo,
                contrasena = state.contrasena
            )) {
                is RegisterResult.Success -> {
                    setState {
                        copy(
                            isLoading = false,
                            isRegisteredSuccessfully = true
                        )
                    }
                    sendEffect(
                        RegisterUiEffect.NavigateToLogin(
                            "¡Cuenta creada con éxito! Inicia sesión para comenzar."
                        )
                    )
                }
                is RegisterResult.EmailAlreadyExists -> {
                    setState {
                        copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is RegisterResult.ValidationError -> {
                    setState {
                        copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }
}

