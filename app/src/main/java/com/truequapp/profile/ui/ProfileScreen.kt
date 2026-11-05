package com.truequapp.profile.ui

import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truequapp.core.database.entity.UserEntity
import com.truequapp.core.designsystem.theme.TruequappTheme
import kotlinx.coroutines.flow.Flow

@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel,
    onLogoutSuccess: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ProfileUiEffect.NavigateToLogin -> onLogoutSuccess()
                is ProfileUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ProfileScreen(
        state = state,
        onLogout = { viewModel.onEvent(ProfileUiEvent.Logout) }
    )
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TruequappTheme.colorScheme.background)
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = TruequappTheme.colorScheme.primary
                )
            }
            state.user != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header de Usuario
                    ProfileHeader(user = state.user)

                    // Métricas de la Base de Datos (TruequeCoins, Trueques, Calificación)
                    DatabaseMetricsSection(user = state.user)

                    // Información Detallada de la Cuenta (Columnas SQL)
                    AccountDetailsSection(user = state.user)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botón de Cerrar Sesión
                    Button(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TruequappTheme.colorScheme.errorContainer,
                            contentColor = TruequappTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar Sesión",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cerrar Sesión",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = TruequappTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.errorMessage ?: "No hay información de sesión disponible.",
                        style = TruequappTheme.typography.bodyLarge,
                        color = TruequappTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onLogout) {
                        Text("Ir a Iniciar Sesión")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(user: UserEntity) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(TruequappTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Avatar de Usuario",
                modifier = Modifier.size(54.dp),
                tint = TruequappTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = user.usuario,
            style = TruequappTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TruequappTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            color = TruequappTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Usuario Registrado",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TruequappTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun DatabaseMetricsSection(user: UserEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = TruequappTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Métrica 1: TruequeCoins
            MetricItem(
                icon = Icons.Default.MonetizationOn,
                iconTint = Color(0xFFE5A000), // Ámbar Truequapp
                label = "TruequeCoins",
                value = String.format("%.0f TC", user.cantidadTruequeCoins)
            )

            // Divisor vertical
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
                    .background(TruequappTheme.colorScheme.outline.copy(alpha = 0.2f))
            )

            // Métrica 2: Trueques Realizados
            MetricItem(
                icon = Icons.Default.SwapHoriz,
                iconTint = TruequappTheme.colorScheme.primary,
                label = "Trueques",
                value = "${user.truequesRealizados}"
            )

            // Divisor vertical
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
                    .background(TruequappTheme.colorScheme.outline.copy(alpha = 0.2f))
            )

            // Métrica 3: Calificación
            MetricItem(
                icon = Icons.Default.Star,
                iconTint = Color(0xFFFFA000),
                label = "Calificación",
                value = String.format("%.1f ★", user.calificacion)
            )
        }
    }
}

@Composable
private fun MetricItem(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = TruequappTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TruequappTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = TruequappTheme.typography.labelSmall,
            color = TruequappTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun AccountDetailsSection(user: UserEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = TruequappTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Información de la Cuenta (Base de Datos)",
                style = TruequappTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TruequappTheme.colorScheme.primary
            )

            HorizontalDivider(color = TruequappTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Correo Electrónico
            InfoRow(
                icon = Icons.Default.Email,
                title = "Correo electrónico",
                subtitle = user.correo
            )

            // Teléfono
            InfoRow(
                icon = Icons.Default.Phone,
                title = "Número telefónico",
                subtitle = user.numeroTelefonico
            )

            // Usuario
            InfoRow(
                icon = Icons.Default.Person,
                title = "Nombre de usuario",
                subtitle = user.usuario
            )

            // Contraseña protegida
            InfoRow(
                icon = Icons.Default.Lock,
                title = "Contraseña",
                subtitle = "••••••••  (Almacenada de forma segura)"
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(TruequappTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TruequappTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = title,
                style = TruequappTheme.typography.labelSmall,
                color = TruequappTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = TruequappTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TruequappTheme.colorScheme.onSurface
            )
        }
    }
}

