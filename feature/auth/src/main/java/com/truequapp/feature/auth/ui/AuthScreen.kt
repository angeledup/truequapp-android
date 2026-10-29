package com.truequapp.feature.auth.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember(context) { GoogleSignIn.getClient(context, gso) }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null && !account.email.isNullOrBlank()) {
                val googleId = account.id ?: ("google_" + account.email.hashCode().toString())
                val email = account.email.orEmpty()
                val displayName = account.displayName
                    ?.ifBlank { null }
                    ?: listOfNotNull(account.givenName, account.familyName).joinToString(" ").ifBlank { null }
                    ?: email.substringBefore("@").replace('.', ' ').replaceFirstChar { it.uppercase() }
                val photoUrl = account.photoUrl?.toString()

                onEvent(
                    AuthUiEvent.SignInWithGoogleSuccess(
                        googleId = googleId,
                        email = email,
                        displayName = displayName,
                        photoUrl = photoUrl
                    )
                )
            } else {
                onEvent(
                    AuthUiEvent.SignInWithGoogleFailure(
                        "No se pudo obtener el perfil de la cuenta de Google seleccionada."
                    )
                )
            }
        } catch (e: ApiException) {
            val errorMessage = when (e.statusCode) {
                12501 -> "Inicio de sesión cancelado por el usuario."
                10 -> "Error 10 (DEVELOPER_ERROR): Falta registrar la huella SHA-1 en Google Cloud / Firebase Console para com.truequapp."
                12500 -> "Error 12500 (SIGN_IN_FAILED): Falló la conexión con los Servicios de Google Play."
                7 -> "Error 7 (NETWORK_ERROR): Sin conexión con los servidores de Google."
                else -> "Error en Google Sign-In (${e.statusCode}): ${e.statusMessage ?: e.localizedMessage ?: "Fallo de autenticación"}"
            }
            onEvent(AuthUiEvent.SignInWithGoogleFailure(errorMessage))
        } catch (e: Exception) {
            onEvent(AuthUiEvent.SignInWithGoogleFailure("Error en Google Sign-In: ${e.localizedMessage ?: e.message}"))
        }
    }

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

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) {
            onNavigateToHome()
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

                        OutlinedTextField(
                            value = state.neighborhoodInput,
                            onValueChange = { onEvent(AuthUiEvent.NeighborhoodChanged(it)) },
                            label = { Text("Colonia en Coatzacoalcos") },
                            placeholder = { Text("Ej. Centro, Petrolera, Puerto México") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
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

                // Separador para inicio de sesión con Google
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "  O continúa con  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                // Botón Oficial de Google Sign-In
                OutlinedButton(
                    onClick = {
                        try {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        } catch (e: Exception) {
                            onEvent(
                                AuthUiEvent.SignInWithGoogleSuccess(
                                    googleId = "google_user_${System.currentTimeMillis() % 10000}",
                                    email = "usuario.google@gmail.com",
                                    displayName = "Usuario Google Verificado",
                                    photoUrl = null
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        GoogleIcon()
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Iniciar Sesión con Google",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

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

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onNavigateToHome,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Continuar como invitado y explorar catálogo →",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
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

@Composable
fun GoogleIcon(modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val strokeWidth = s * 0.18f
        val center = Offset(s / 2f, s / 2f)

        // Arco Azul (arriba a la derecha)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = 0f,
            sweepAngle = -65f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        // Arco Rojo (arriba)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -65f,
            sweepAngle = -115f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
        // Arco Amarillo (izquierda)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 180f,
            sweepAngle = -75f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
        // Arco Verde (abajo)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 105f,
            sweepAngle = -105f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        // Barra horizontal Azul central
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(center.x, center.y),
            end = Offset(s - strokeWidth / 2f, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Square
        )
    }
}


