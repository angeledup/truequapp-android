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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truequapp.core.datastore.model.DatabaseValidationMode
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
        onSelectValidationMode = { viewModel.onEvent(SettingsUiEvent.SelectValidationMode(it)) },
        onDismissModeDialog = { viewModel.onEvent(SettingsUiEvent.DismissModeDialog) },
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
    onSelectValidationMode: (DatabaseValidationMode) -> Unit,
    onDismissModeDialog: () -> Unit,
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

                    Spacer(modifier = Modifier.height(4.dp))

                    // Sección de Motor de Validación de Base de Datos
                    ValidationModeSection(
                        currentMode = state.validationMode,
                        onSelectMode = onSelectValidationMode
                    )

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

    if (state.showModeDialog && state.dialogModeSelected != null) {
        ValidationModeInfoDialog(
            selectedMode = state.dialogModeSelected,
            onDismiss = onDismissModeDialog
        )
    }
}

@Composable
private fun ValidationModeSection(
    currentMode: DatabaseValidationMode,
    onSelectMode: (DatabaseValidationMode) -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = TruequappTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Motor de Validación de Base de Datos",
                    style = TruequappTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TruequappTheme.colorScheme.primary
                )
            }

            Text(
                text = "Selecciona si las validaciones de cuenta y trueques se realizan contra la nube o localmente:",
                style = TruequappTheme.typography.bodySmall,
                color = TruequappTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = TruequappTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Opción 1: db-Online (Turso Cloud)
            ValidationModeOptionCard(
                title = "db-Online (Turso Cloud)",
                subtitle = "Validación centralizada en la nube en tiempo real. Sincronía global con todos los usuarios.",
                icon = Icons.Default.Cloud,
                isSelected = currentMode == DatabaseValidationMode.DB_ONLINE,
                onClick = { onSelectMode(DatabaseValidationMode.DB_ONLINE) }
            )

            // Opción 2: db-Local (Dispositivo)
            ValidationModeOptionCard(
                title = "db-Local (Dispositivo)",
                subtitle = "Validación instantánea local (< 5 ms). Funciona 100% offline sin consumir datos móviles.",
                icon = Icons.Default.PhoneAndroid,
                isSelected = currentMode == DatabaseValidationMode.DB_LOCAL,
                onClick = { onSelectMode(DatabaseValidationMode.DB_LOCAL) }
            )
        }
    }
}

@Composable
private fun ValidationModeOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                TruequappTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                TruequappTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, TruequappTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, TruequappTheme.colorScheme.outlineVariant)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) TruequappTheme.colorScheme.primary else TruequappTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) TruequappTheme.colorScheme.onPrimary else TruequappTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TruequappTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = TruequappTheme.colorScheme.onSurfaceVariant
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun ValidationModeInfoDialog(
    selectedMode: DatabaseValidationMode,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (selectedMode == DatabaseValidationMode.DB_ONLINE) Icons.Default.Cloud else Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = TruequappTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = if (selectedMode == DatabaseValidationMode.DB_ONLINE) {
                    "Modo db-Online Activado"
                } else {
                    "Modo db-Local Activado"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (selectedMode == DatabaseValidationMode.DB_ONLINE) {
                    Text(
                        text = "Has activado las validaciones directas contra la base de datos central en Turso Cloud.",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "• Credenciales, unicidad y propuestas se validan en tiempo real en la nube.\n• Garantiza que interactúes con la información global más reciente de todos los usuarios.\n• Requiere conexión a internet activa para validar.",
                        fontSize = 13.sp,
                        color = TruequappTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(color = TruequappTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(
                        text = "Diferencia con db-Local: A diferencia de db-Local (que funciona sin conexión de forma instantánea en tu teléfono), db-Online verifica los datos globalmente para prevenir cualquier conflicto con otros usuarios.",
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = TruequappTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Has activado las validaciones en la base de datos interna SQLite de este dispositivo.",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "• Las validaciones se ejecutan de manera instantánea (< 5 ms) en tu teléfono.\n• Funciona al 100% sin necesidad de internet ni consumo de datos móviles.\n• Las propuestas de trueque se guardarán en estado 'Enviando' hasta validar con el servidor que la publicación siga activa.",
                        fontSize = 13.sp,
                        color = TruequappTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(color = TruequappTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(
                        text = "Diferencia con db-Online: A diferencia de db-Online (que consulta la nube en tiempo real), db-Local no verifica en el momento los cambios realizados por terceros hasta que se sincronicen en segundo plano.",
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = TruequappTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido", fontWeight = FontWeight.Bold)
            }
        }
    )
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
