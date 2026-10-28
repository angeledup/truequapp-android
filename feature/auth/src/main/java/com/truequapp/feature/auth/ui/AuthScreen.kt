package com.truequapp.feature.auth.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truequapp.core.designsystem.component.TruequappPrimaryButton
import com.truequapp.core.designsystem.component.TruequappTopAppBar
import com.truequapp.feature.auth.mvi.AuthUiEffect
import com.truequapp.feature.auth.mvi.AuthUiEvent
import com.truequapp.feature.auth.mvi.AuthUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Pantalla unificada de Inicio de Sesión y Registro con KYC Nivel 1 (CU001, CU023).
 */
@Composable
fun AuthScreen(
    state: AuthUiState,
    effectFlow: Flow<AuthUiEffect>,
    onEvent: (AuthUiEvent) -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(effectFlow) {
        effectFlow.collectLatest { effect ->
            when (effect) {
                is AuthUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is AuthUiEffect.NavigateToHome -> onNavigateToHome()
                is AuthUiEffect.NavigateToLogin -> {}
                is AuthUiEffect.OtpVerifiedSuccess -> {}
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TruequappTopAppBar(
                title = if (state.isRegisterMode) "Crear Cuenta en Truequapp" else "Iniciar Sesión",
                coinsBalance = 0
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Logo
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (state.isRegisterMode) "Únete a la Comunidad Truequapp" else "Bienvenido de Nuevo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (state.isRegisterMode)
                        "Intercambia artículos de forma segura con verificación KYC."
                    else
                        "Ingresa tus credenciales para acceder a tus trueques.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Campos del formulario
                AnimatedVisibility(visible = state.isRegisterMode) {
                    Column {
                        OutlinedTextField(
                            value = state.nameInput,
                            onValueChange = { onEvent(AuthUiEvent.NameChanged(it)) },
                            label = { Text("Nombre Completo") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = MaterialTheme.shapes.medium
                        )

                        OutlinedTextField(
                            value = state.phoneInput,
                            onValueChange = { onEvent(AuthUiEvent.PhoneChanged(it)) },
                            label = { Text("Teléfono (Formato E.164)") },
                            placeholder = { Text("+522281234567") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }

                OutlinedTextField(
                    value = state.emailInput,
                    onValueChange = { onEvent(AuthUiEvent.EmailChanged(it)) },
                    label = { Text("Correo Electrónico") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = state.passwordInput,
                    onValueChange = { onEvent(AuthUiEvent.PasswordChanged(it)) },
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = MaterialTheme.shapes.medium
                )

                if (!state.errorMessage.isNullOrBlank()) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                TruequappPrimaryButton(
                    text = if (state.isRegisterMode) "Continuar con Verificación SMS" else "Iniciar Sesión",
                    onClick = {
                        if (state.isRegisterMode) {
                            onEvent(AuthUiEvent.SubmitRegister)
                        } else {
                            onEvent(AuthUiEvent.SubmitLogin)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { onEvent(AuthUiEvent.ToggleAuthMode) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (state.isRegisterMode)
                            "¿Ya tienes cuenta? Inicia sesión aquí"
                        else
                            "¿No tienes cuenta aún? Regístrate gratis",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // Modal Diálogo de Verificación SMS OTP (KYC 1)
    OtpVerificationDialog(
        isVisible = state.isOtpDialogVisible,
        otpCode = state.otpCodeInput,
        secondsRemaining = state.otpSecondsRemaining,
        phoneNumber = state.phoneInput,
        errorMessage = state.errorMessage,
        onOtpChanged = { onEvent(AuthUiEvent.OtpCodeChanged(it)) },
        onVerifyClick = { onEvent(AuthUiEvent.VerifyOtp) },
        onResendClick = { onEvent(AuthUiEvent.ResendOtp) },
        onDismissRequest = { onEvent(AuthUiEvent.DismissOtpDialog) }
    )
}

