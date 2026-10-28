package com.truequapp.feature.auth.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truequapp.core.designsystem.component.KycLevel
import com.truequapp.core.designsystem.component.SafeZoneChip
import com.truequapp.core.designsystem.component.TruequappBadge
import com.truequapp.core.designsystem.component.TruequappTopAppBar
import com.truequapp.feature.auth.mvi.AuthUiEvent
import com.truequapp.feature.auth.mvi.AuthUiState

/**
 * Pantalla de Perfil de Usuario con Reputación Bayesiana (RN-09),
 * Privacidad Geoespacial a 500m (RN-10) y Monedero de TruequeCoins (CU003, CU004, CU024).
 */
@Composable
fun ProfileScreen(
    state: AuthUiState,
    onEvent: (AuthUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TruequappTopAppBar(
                title = "Mi Perfil Truequapp",
                coinsBalance = state.truequeCoins.toInt()
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar y Datos Principales
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = state.nameInput.ifBlank { "Usuario Verificado" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = state.emailInput.ifBlank { "usuario@truequapp.com" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Badge KYC Nivel 1 (Teléfono Verificado)
            TruequappBadge(
                level = KycLevel.LEVEL_1_PHONE
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tarjeta de Reputación Bayesiana (RN-09)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reputación Comunitaria: ${String.format("%.2f", state.bayesianRating)} / 5.0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Calculada mediante estimación bayesiana ponderada (RN-09) para garantizar equidad entre perfiles nuevos y veteranos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tasa de Asistencia a Citas:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${(state.attendanceRate * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de Privacidad Geoespacial (RN-10)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Colonia de Referencia (Pública)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Sector: ${state.obfuscatedNeighborhood.ifBlank { "Zona de Intercambio General" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Tu domicilio exacto está protegido bajo un radio difuso de 500m (RN-10 / RNF-08). Las coordenadas exactas solo se revelan en Puntos Seguros autorizados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SafeZoneChip(
                        isVerified = true,
                        zoneName = "Radio protegido 500m activo"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de Monedero TruequeCoins
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Saldo TruequeCoins",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${state.truequeCoins.toInt()} TC",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de Cierre de Sesión (CU024)
            OutlinedButton(
                onClick = { onEvent(AuthUiEvent.Logout) },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Cerrar Sesión Segura",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

