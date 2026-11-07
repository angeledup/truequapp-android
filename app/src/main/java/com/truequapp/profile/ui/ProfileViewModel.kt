package com.truequapp.profile.ui

import androidx.lifecycle.viewModelScope
import com.truequapp.auth.data.AuthRepository
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.common.mvi.UiEffect
import com.truequapp.core.common.mvi.UiEvent
import com.truequapp.core.common.mvi.UiState
import com.truequapp.core.database.entity.UserEntity
import com.truequapp.core.database.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: UserEntity? = null,
    val errorMessage: String? = null
) : UiState

sealed interface ProfileUiEvent : UiEvent {
    data object LoadProfile : ProfileUiEvent
    data object Logout : ProfileUiEvent
}

sealed interface ProfileUiEffect : UiEffect {
    data object NavigateToLogin : ProfileUiEffect
    data class ShowToast(val message: String) : ProfileUiEffect
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository? = null
) : BaseViewModel<ProfileUiState, ProfileUiEvent, ProfileUiEffect>(ProfileUiState()) {

    init {
        loadProfile()
    }

    override fun onEvent(event: ProfileUiEvent) {
        when (event) {
            is ProfileUiEvent.LoadProfile -> loadProfile()
            is ProfileUiEvent.Logout -> logout()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            if (userRepository != null) {
                userRepository.observeCurrentUser().collectLatest { userEntity ->
                    setState {
                        copy(
                            isLoading = false,
                            user = userEntity,
                            errorMessage = if (userEntity == null) "No se encontró el perfil en la base de datos" else null
                        )
                    }
                }
            } else {
                authRepository.currentSessionFlow.collectLatest { session ->
                    val email = session.userEmail
                    if (!email.isNullOrBlank()) {
                        authRepository.observeUserByEmail(email).collectLatest { userEntity ->
                            setState {
                                copy(
                                    isLoading = false,
                                    user = userEntity,
                                    errorMessage = if (userEntity == null) "No se encontró el perfil en la base de datos" else null
                                )
                            }
                        }
                    } else {
                        setState {
                            copy(
                                isLoading = false,
                                user = null,
                                errorMessage = "No hay una sesión activa"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            sendEffect(ProfileUiEffect.NavigateToLogin)
        }
    }
}

