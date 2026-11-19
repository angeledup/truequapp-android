package com.truequapp.profile.ui

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truequapp.core.designsystem.theme.TruequappTheme

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SettingsUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is SettingsUiEffect.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    SettingsScreen(
        state = state,
        onUsernameChanged = { viewModel.onEvent(SettingsUiEvent.UsernameChanged(it)) },
        onEmailChanged = { viewModel.onEvent(SettingsUiEvent.EmailChanged(it)) },
        onPasswordChanged = { viewModel.onEvent(SettingsUiEvent.PasswordChanged(it)) },
        onTogglePasswordVisibility = { viewModel.onEvent(SettingsUiEvent.TogglePasswordVisibility) },
        onSave = { viewModel.onEvent(SettingsUiEvent.SaveSettings) },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onUsernameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajustes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TruequappTheme.colorScheme.background,
                    titleContentColor = TruequappTheme.colorScheme.onBackground,
                    navigationIconContentColor = TruequappTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = TruequappTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = TruequappTheme.colorScheme.primary
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Encabezado decorativo
                    SettingsHeader()

                    // Banner de Error
                    AnimatedVisibility(visible = state.errorMessage != null) {
                        state.errorMessage?.let { msg ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = TruequappTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = TruequappTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = msg,
                                        color = TruequappTheme.colorScheme.onErrorContainer,
                                        style = TruequappTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Formulario de edición
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = TruequappTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Modificar Datos de Cuenta",
                                style = TruequappTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TruequappTheme.colorScheme.primary
                            )

                            HorizontalDivider(color = TruequappTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // 1. Nombre de usuario
                            OutlinedTextField(
                                value = state.username,
                                onValueChange = onUsernameChanged,
                                label = { Text("Nombre de usuario") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = TruequappTheme.colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                enabled = !state.isSaving,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TruequappTheme.colorScheme.primary,
                                    unfocusedBorderColor = TruequappTheme.colorScheme.outline
                                )
                            )

                            // 2. Correo registrado
                            OutlinedTextField(
                                value = state.email,
                                onValueChange = onEmailChanged,
                                label = { Text("Correo registrado") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = TruequappTheme.colorScheme.primary
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                enabled = !state.isSaving,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TruequappTheme.colorScheme.primary,
                                    unfocusedBorderColor = TruequappTheme.colorScheme.outline
                                )
                            )

                            // 3. Contraseña
                            OutlinedTextField(
                                value = state.password,
                                onValueChange = onPasswordChanged,
                                label = { Text("Contraseña") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = TruequappTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = onTogglePasswordVisibility) {
                                        Icon(
                                            imageVector = if (state.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (state.isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                        )
                                    }
                                },
                                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                enabled = !state.isSaving,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TruequappTheme.colorScheme.primary,
                                    unfocusedBorderColor = TruequappTheme.colorScheme.outline
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Botón Guardar Cambios
                    Button(
                        onClick = onSave,
                        enabled = !state.isSaving && state.username.isNotBlank() && state.email.isNotBlank() && state.password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TruequappTheme.colorScheme.primary,
                            contentColor = TruequappTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = TruequappTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Guardando...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Guardar Cambios",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(TruequappTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Ajustes",
                modifier = Modifier.size(44.dp),
                tint = TruequappTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Ajustes de Usuario",
            style = TruequappTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TruequappTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Modifica tu nombre de usuario, contraseña y correo registrado",
            style = TruequappTheme.typography.bodySmall,
            color = TruequappTheme.colorScheme.onSurfaceVariant
        )
    }
}
