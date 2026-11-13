package com.truequapp.auth.ui

import androidx.lifecycle.viewModelScope
import com.truequapp.auth.data.AuthRepository
import com.truequapp.auth.data.LoginResult
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.common.mvi.UiEffect
import com.truequapp.core.common.mvi.UiEvent
import com.truequapp.core.common.mvi.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val contrasena: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null
) : UiState

sealed interface LoginUiEvent : UiEvent {
    data class EmailChanged(val value: String) : LoginUiEvent
    data class PasswordChanged(val value: String) : LoginUiEvent
    data object TogglePasswordVisibility : LoginUiEvent
    data object SubmitLogin : LoginUiEvent
    data object ClearErrors : LoginUiEvent
}

sealed interface LoginUiEffect : UiEffect {
    data object NavigateToHome : LoginUiEffect
    data object NavigateToRegister : LoginUiEffect
    data class ShowToast(val message: String) : LoginUiEffect
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel<LoginUiState, LoginUiEvent, LoginUiEffect>(LoginUiState()) {

    override fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.EmailChanged -> {
                setState {
                    copy(
                        email = event.value,
                        emailError = null,
                        generalError = null
                    )
                }
            }
            is LoginUiEvent.PasswordChanged -> {
                setState {
                    copy(
                        contrasena = event.value,
                        passwordError = null,
                        generalError = null
                    )
                }
            }
            is LoginUiEvent.TogglePasswordVisibility -> {
                setState { copy(isPasswordVisible = !isPasswordVisible) }
            }
            is LoginUiEvent.ClearErrors -> {
                setState { copy(emailError = null, passwordError = null, generalError = null) }
            }
            is LoginUiEvent.SubmitLogin -> {
                login()
            }
        }
    }

    private fun login() {
        val email = uiState.value.email
        val password = uiState.value.contrasena

        setState {
            copy(
                isLoading = true,
                emailError = null,
                passwordError = null,
                generalError = null
            )
        }

        viewModelScope.launch {
            when (val result = authRepository.login(email, password)) {
                is LoginResult.Success -> {
                    setState { copy(isLoading = false) }
                    sendEffect(LoginUiEffect.NavigateToHome)
                }
                is LoginResult.UserNotFound -> {
                    setState {
                        copy(
                            isLoading = false,
                            emailError = result.message
                        )
                    }
                }
                is LoginResult.InvalidPassword -> {
                    setState {
                        copy(
                            isLoading = false,
                            passwordError = result.message
                        )
                    }
                }
                is LoginResult.ValidationError -> {
                    setState {
                        copy(
                            isLoading = false,
                            generalError = result.message
                        )
                    }
                }
            }
        }
    }
}

