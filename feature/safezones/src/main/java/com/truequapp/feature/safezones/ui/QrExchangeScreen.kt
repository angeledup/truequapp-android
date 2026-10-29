package com.truequapp.feature.safezones.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truequapp.core.designsystem.component.TruequappOutlinedButton
import com.truequapp.core.designsystem.component.TruequappPrimaryButton
import com.truequapp.core.designsystem.theme.TruequappTheme
import com.truequapp.core.domain.model.GeoCoordinates
import com.truequapp.feature.safezones.mvi.SafeZoneUiEffect
import com.truequapp.feature.safezones.mvi.SafeZoneUiEvent
import com.truequapp.feature.safezones.mvi.SafeZoneUiState
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrExchangeScreen(
    tradeId: String,
    state: SafeZoneUiState,
    effectFlow: Flow<SafeZoneUiEffect>,
    onEvent: (SafeZoneUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToReview: (String) -> Unit,
    onNavigateToDispute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var inputOtpPin by remember { mutableStateOf("") }

    LaunchedEffect(tradeId) {
        onEvent(SafeZoneUiEvent.GenerateQrCode)
    }

    LaunchedEffect(effectFlow) {
        effectFlow.collect { effect ->
            when (effect) {
                is SafeZoneUiEffect.NavigateBack -> onNavigateBack()
                is SafeZoneUiEffect.NavigateToReview -> onNavigateToReview(effect.tradeId)
                is SafeZoneUiEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
                is SafeZoneUiEffect.TriggerHapticConfirmation -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirmación Presencial QR", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Pestañas: Mostrar QR vs Escanear QR
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Mi Código de Entrega", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Escanear o PIN", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            // Advertencia si se detecta Mock Location (CU022 / Anti-Spoofing)
            state.mockLocationWarning?.let { warning ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(warning, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                    }
                }
            }

            if (selectedTab == 0) {
                // Vista Generador de QR Dinámico (CU020)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Muestra este código al receptor del artículo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulación visual del Código QR Criptográfico HMAC-SHA256
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .size(240.dp)
                            .border(2.dp, TruequappTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "Código QR Dinámico",
                                    tint = Color.Black,
                                    modifier = Modifier.size(160.dp)
                                )
                                Text(
                                    text = "TRUEQUAPP SECURE QR",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Respaldo Offline PIN TOTP de 6 dígitos
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Modo Offline / Respaldo PIN (6 dígitos):", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.fallbackOtpPin ?: "------",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = TruequappTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Verificación de Geocerca 150m GPS (RN-08)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            // Simulador de llegada a SafeZone para pruebas o hardware real
                            onEvent(
                                SafeZoneUiEvent.CheckInGpsLocation(
                                    coordinates = state.selectedSafeZone?.coordinates ?: GeoCoordinates(19.3332, -99.1878),
                                    isMockLocation = false
                                )
                            )
                        }) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Validar Presencia GPS (150m)", fontSize = 12.sp)
                        }

                        TextButton(onClick = { onNavigateToDispute(tradeId) }) {
                            Text("Reportar Problema", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Vista Escáner y Validación de Entrega
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Escanea el código de la contraparte",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Área de escaneo con CameraX / ML Kit
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Cámara activa (Google ML Kit Barcode)",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Botón para simular captura de QR en pruebas rápidas
                    TruequappOutlinedButton(
                        text = "Simular Lectura de QR Válido (CU020)",
                        onClick = {
                            onEvent(SafeZoneUiEvent.OnQrCodeScanned("TRUEQUAPP_QR:$tradeId:valid_hmac_signature"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "— O ingresa el PIN de respaldo —",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    OutlinedTextField(
                        value = inputOtpPin,
                        onValueChange = { inputOtpPin = it.take(6) },
                        label = { Text("PIN de 6 dígitos") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    TruequappPrimaryButton(
                        text = "Validar PIN y Liquidar Trueque",
                        onClick = { onEvent(SafeZoneUiEvent.SubmitFallbackOtp(inputOtpPin)) },
                        enabled = inputOtpPin.length == 6
                    )
                }
            }
        }
    }
}
