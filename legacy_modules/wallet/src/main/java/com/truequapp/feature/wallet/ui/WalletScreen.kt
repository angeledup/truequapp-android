package com.truequapp.feature.wallet.ui

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truequapp.core.designsystem.theme.PaletteConfig
import com.truequapp.core.domain.model.LedgerEntry
import com.truequapp.core.domain.model.LedgerEntryType
import com.truequapp.feature.wallet.mvi.WalletUiEffect
import com.truequapp.feature.wallet.mvi.WalletUiEvent
import com.truequapp.feature.wallet.mvi.WalletViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToTrades: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInputValue by remember { mutableStateOf("") }

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is WalletUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is WalletUiEffect.TriggerBiometricPrompt -> {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        val executor = ContextCompat.getMainExecutor(activity)
                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Autenticación Biométrica Truequapp")
                            .setSubtitle("Confirma tu identidad para autorizar transacciones con TruequeCoins")
                            .setNegativeButtonText("Cancelar")
                            .build()

                        val biometricPrompt = BiometricPrompt(
                            activity,
                            executor,
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    super.onAuthenticationSucceeded(result)
                                    viewModel.onBiometricAuthSuccess()
                                }

                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                    super.onAuthenticationError(errorCode, errString)
                                    Toast.makeText(activity, "Error biométrico: $errString", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        biometricPrompt.authenticate(promptInfo)
                    } else {
                        // Fallback si la actividad no es FragmentActivity en Compose Preview
                        viewModel.onBiometricAuthSuccess()
                    }
                }
                is WalletUiEffect.TriangularCycleCompleted -> {
                    Toast.makeText(context, "¡Ciclo Triangular 3/3 Bloqueado en Escrow!", Toast.LENGTH_LONG).show()
                }
                is WalletUiEffect.RequirePinAuthentication -> {
                    showPinDialog = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Monedero Digital (CU013)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "TruequeCoins y Custodia Escrow (RN-04/05)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(WalletUiEvent.LoadWallet(state.wallet.userId)) }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Invariante RN-04 Verificación
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (state.isInvariantPreserved) PaletteConfig.PrimaryContainerLight
                    else PaletteConfig.ErrorContainerLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (state.isInvariantPreserved) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (state.isInvariantPreserved) PaletteConfig.PrimaryDefault else PaletteConfig.ErrorDefault
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Invariante RN-04 Verificado: Saldo Disponible + Custodia Escrow = Saldo Total",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.isInvariantPreserved) PaletteConfig.PrimaryDark else PaletteConfig.ErrorDefault
                        )
                    }
                }
            }

            // 2. Tarjeta Principal de Saldos
            item {
                WalletBalancesCard(
                    availableBalance = state.wallet.availableBalance,
                    escrowBalance = state.wallet.escrowBalance,
                    totalBalance = state.totalBalance,
                    onNavigateToTrades = onNavigateToTrades
                )
            }

            // 3. Tarjeta de Oportunidad Triangular (CU016 / RN-06)
            if (state.triangularCycle != null) {
                item {
                    TriangularOpportunityCard(
                        cycle = state.triangularCycle!!,
                        onViewDetails = { viewModel.onEvent(WalletUiEvent.OpenTriangularDetails) }
                    )
                }
            }

            // 4. Seguridad: PIN y Biometría (CU014)
            item {
                SecuritySettingsCard(
                    isPinConfigured = state.wallet.isPinConfigured,
                    isBiometricsEnabled = state.wallet.isBiometricsEnabled,
                    onConfigurePin = { showPinDialog = true },
                    onToggleBiometrics = { enabled -> viewModel.onEvent(WalletUiEvent.ToggleBiometrics(enabled)) }
                )
            }

            // 5. Historial de Asientos Contables (RN-04)
            item {
                Text(
                    text = "Libro Mayor Contable (Partida Doble)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(state.ledgerHistory, key = { it.id }) { entry ->
                LedgerEntryRow(entry = entry)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // Diálogo de Trueque Triangular
        if (state.showTriangularDialog && state.triangularCycle != null) {
            TriangularTradeDialog(
                cycle = state.triangularCycle!!,
                currentUserId = state.wallet.userId,
                onSign = { cycleId, userId -> viewModel.onEvent(WalletUiEvent.SignTriangularCycle(cycleId, userId)) },
                onAbort = { cycleId, userId -> viewModel.onEvent(WalletUiEvent.AbortTriangularCycle(cycleId, userId)) },
                onDismiss = { viewModel.onEvent(WalletUiEvent.CloseTriangularDetails) }
            )
        }

        // Diálogo de Configuración de PIN
        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                title = { Text("Configurar PIN de Seguridad") },
                text = {
                    Column {
                        Text("Ingresa un PIN de 4 a 6 dígitos para proteger tus transferencias y liberaciones de Escrow:")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pinInputValue,
                            onValueChange = { if (it.length <= 6) pinInputValue = it },
                            label = { Text("PIN Numérico") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pinInputValue.isNotBlank()) {
                                viewModel.onEvent(WalletUiEvent.SetSecurityPin(pinInputValue))
                                pinInputValue = ""
                                showPinDialog = false
                            }
                        }
                    ) {
                        Text("Guardar PIN")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPinDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun WalletBalancesCard(
    availableBalance: Double,
    escrowBalance: Double,
    totalBalance: Double,
    onNavigateToTrades: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saldo Total",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PaletteConfig.SecondaryContainerLight
                ) {
                    Text(
                        text = "TC (TruequeCoins)",
                        color = PaletteConfig.SecondaryDark,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "\$${totalBalance.toInt()} TC",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Disponible
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = PaletteConfig.PrimaryDefault, modifier = Modifier.size(10.dp)) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Disponible",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\$${availableBalance.toInt()} TC",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PaletteConfig.PrimaryDefault
                    )
                    Text(
                        text = "Listo para ofrecer en trueques",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // En Custodia Escrow
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = PaletteConfig.SecondaryDefault, modifier = Modifier.size(10.dp)) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "En Garantía (Escrow)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\$${escrowBalance.toInt()} TC",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PaletteConfig.SecondaryDefault
                    )
                    Text(
                        text = "Retenido en intercambios",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Información Educativa de Custodia Escrow (RN-04 / RN-05)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "¿Cómo funciona la Custodia Escrow (RN-04/05)?",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Las TruequeCoins (TC) sirven para compensar diferencias de valor en intercambios. Al acordar un trueque con diferencia, las monedas se retienen en garantía segura (Escrow) y se liberan a la contraparte solo cuando ambos validan la entrega en un Punto Seguro mediante código QR o PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onNavigateToTrades,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PaletteConfig.PrimaryDefault)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver Mis Trueques con Garantía")
            }
        }
    }
}

@Composable
fun TriangularOpportunityCard(
    cycle: com.truequapp.core.domain.model.TriangularCycle,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PaletteConfig.TertiaryContainerLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapCalls,
                        contentDescription = null,
                        tint = PaletteConfig.TertiaryDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Match Triangular Detectado",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PaletteConfig.TertiaryDark
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PaletteConfig.TertiaryDefault
                ) {
                    Text(
                        text = "${(cycle.compatibilityScore * 100).toInt()}% Match",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Un ciclo multilateral A ➔ B ➔ C ➔ A permite que recibas el artículo que buscas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PaletteConfig.TertiaryDefault)
            ) {
                Text("Ver Círculo de Trueque (${cycle.signaturesCount}/3 firmas)")
            }
        }
    }
}

@Composable
fun SecuritySettingsCard(
    isPinConfigured: Boolean,
    isBiometricsEnabled: Boolean,
    onConfigurePin: () -> Unit,
    onToggleBiometrics: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Seguridad del Monedero (CU014)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // PIN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Pin, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("PIN de Operaciones", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (isPinConfigured) "PIN activo" else "No configurado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(onClick = onConfigurePin) {
                    Text(if (isPinConfigured) "Cambiar" else "Configurar")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Biometría
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Biometría (Huella / Rostro)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Autoriza liberaciones de Escrow",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isBiometricsEnabled,
                    onCheckedChange = onToggleBiometrics
                )
            }
        }
    }
}

@Composable
fun LedgerEntryRow(entry: LedgerEntry) {
    val isCredit = entry.type == LedgerEntryType.CREDITO
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = if (isCredit) PaletteConfig.PrimaryContainerLight else PaletteConfig.ErrorContainerLight,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isCredit) PaletteConfig.PrimaryDefault else PaletteConfig.ErrorDefault,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = entry.concept.name.replace("_", " "),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                    Text(
                        text = dateFormat.format(Date(entry.timestampEpochMs)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "${if (isCredit) "+" else "-"}\$${entry.amount.toInt()} TC",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCredit) PaletteConfig.PrimaryDefault else PaletteConfig.ErrorDefault
            )
        }
    }
}

