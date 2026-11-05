package com.truequapp.feature.trade.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truequapp.core.designsystem.component.ConditionTag
import com.truequapp.core.designsystem.component.ItemCondition as DesignItemCondition
import com.truequapp.core.designsystem.component.TruequappOutlinedButton
import com.truequapp.core.designsystem.component.TruequappPrimaryButton
import com.truequapp.core.designsystem.theme.TruequappTheme
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.core.domain.model.TradeItem
import com.truequapp.core.domain.model.TradeProposal
import com.truequapp.core.domain.model.TradeStatus
import com.truequapp.feature.trade.mvi.TradeUiEffect
import com.truequapp.feature.trade.mvi.TradeUiEvent
import com.truequapp.feature.trade.mvi.TradeUiState
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeNegotiationScreen(
    proposalId: String,
    state: TradeUiState,
    effectFlow: Flow<TradeUiEffect>,
    onEvent: (TradeUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToSafeZone: (String) -> Unit = {},
    onNavigateToQr: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(proposalId) {
        onEvent(TradeUiEvent.LoadProposalDetails(proposalId, state.currentUserId))
    }

    LaunchedEffect(effectFlow) {
        effectFlow.collect { effect ->
            when (effect) {
                is TradeUiEffect.NavigateToChat -> onNavigateToChat(effect.proposalId)
                is TradeUiEffect.NavigateBack -> onNavigateBack()
                is TradeUiEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
                else -> Unit
            }
        }
    }

    val proposal = state.currentProposal
    val targetItem = state.negotiationTargetItem
    val offeredItems = state.negotiationOfferedItems
    val isReceiver = proposal?.receiverUserId == state.currentUserId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Negociación", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        if (proposal == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TruequappTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Banner de Estado Transaccional
                StatusTimelineBanner(status = proposal.status)

                // Error si lo hay
                state.errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(error, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                        }
                    }
                }

                // Artículo Solicitado
                Text("Artículo Solicitado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                targetItem?.let { target ->
                    NegotiationItemCard(item = target, isTarget = true)
                }

                // Artículos Ofertados
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Artículos Ofertados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${offeredItems.size} ítems", style = MaterialTheme.typography.bodySmall)
                }

                offeredItems.forEach { offered ->
                    NegotiationItemCard(item = offered, isTarget = false)
                }

                // Compensación en TruequeCoins
                if (proposal.compensatoryTruequeCoins > 0.0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Diferencial en TruequeCoins:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "+${proposal.compensatoryTruequeCoins.toInt()} TC",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TruequappTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Botón de Chat Transaccional y Regulación RN-03
                if (proposal.isChatAllowed) {
                    TruequappOutlinedButton(
                        text = "Abrir Chat Transaccional (RN-03)",
                        onClick = { onNavigateToChat(proposal.id) }
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "RN-03: Chat privado regulado exclusivamente para esta propuesta. Al concluir o cancelar, se activará una ventana de auditoría de 48 horas en modo solo lectura.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Acciones según Máquina de Estados y Rol
                when (proposal.status) {
                    TradeStatus.PENDIENTE, TradeStatus.CONTRAOFERTADA -> {
                        if (isReceiver || proposal.status == TradeStatus.CONTRAOFERTADA) {
                            // CU010: Aceptar con Bloqueo ACID RN-01
                            TruequappPrimaryButton(
                                text = "Aceptar Propuesta (Bloqueo Firme)",
                                onClick = { onEvent(TradeUiEvent.AcceptProposal(proposal.id)) }
                            )
                        }

                        // CU011: Contraofertar siempre disponible para negociar antes de aceptar
                        TruequappOutlinedButton(
                            text = "Emitir Contraoferta (CU011)",
                            onClick = { onEvent(TradeUiEvent.OpenCounterOfferDialog) }
                        )

                        if (isReceiver) {
                            // CU012: Rechazar
                            TextButton(
                                onClick = { onEvent(TradeUiEvent.RejectProposal(proposal.id)) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Rechazar Propuesta", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            TruequappOutlinedButton(
                                text = "Cancelar Propuesta",
                                onClick = { onEvent(TradeUiEvent.RequestCancelProposal(proposal.id)) }
                            )
                        }
                    }
                    TradeStatus.RESERVADO, TradeStatus.EN_ENTREGA -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = TruequappTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = TruequappTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Inventario asegurado (RN-01). Procede al punto seguro para la entrega física.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        // Botones de acción directa para completar el flujo
                        TruequappPrimaryButton(
                            text = "Coordinar Punto Seguro (Mapa)",
                            onClick = { onNavigateToSafeZone(proposal.id) }
                        )

                        TruequappOutlinedButton(
                            text = "Ver Código QR de Entrega Presencial",
                            onClick = { onNavigateToQr(proposal.id) }
                        )

                        TruequappOutlinedButton(
                            text = "Abrir Chat Transaccional (RN-03)",
                            onClick = { onNavigateToChat(proposal.id) }
                        )

                        // CU012: Cancelar con RN-07
                        TextButton(
                            onClick = { onEvent(TradeUiEvent.RequestCancelProposal(proposal.id)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancelar Trueque (Evalúa RN-07)", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    else -> Unit
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Diálogo de Contraoferta (CU011)
    if (state.showCounterOfferDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(TradeUiEvent.DismissCounterOfferDialog) },
            title = { Text("Emitir Contraoferta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ajusta los créditos en TruequeCoins de compensación:")
                    OutlinedTextField(
                        value = state.counterOfferTruequeCoinsInput,
                        onValueChange = { onEvent(TradeUiEvent.OnCounterOfferTruequeCoinsChanged(it)) },
                        label = { Text("Nuevos TruequeCoins") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onEvent(TradeUiEvent.SubmitCounterOffer) }) {
                    Text("Enviar Contraoferta")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(TradeUiEvent.DismissCounterOfferDialog) }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de Confirmación de Cancelación (RN-07)
    if (state.showCancelDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(TradeUiEvent.DismissCancelDialog) },
            icon = {
                Icon(
                    imageVector = if (state.isPenaltyCancellation) Icons.Default.Warning else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (state.isPenaltyCancellation) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(if (state.isPenaltyCancellation) "Advertencia de Sanción (RN-07)" else "Confirmar Cancelación")
            },
            text = {
                Text(state.cancellationWarningMessage ?: "¿Deseas cancelar esta propuesta?")
            },
            confirmButton = {
                TextButton(onClick = { onEvent(TradeUiEvent.ConfirmCancelProposal) }) {
                    Text("Sí, Cancelar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(TradeUiEvent.DismissCancelDialog) }) {
                    Text("Regresar")
                }
            }
        )
    }
}

@Composable
private fun StatusTimelineBanner(status: TradeStatus) {
    val (label, color, icon) = when (status) {
        TradeStatus.PENDIENTE -> Triple("PENDIENTE DE RESPUESTA", Color(0xFFE65100), Icons.Default.Schedule)
        TradeStatus.CONTRAOFERTADA -> Triple("CONTRAOFERTADA", Color(0xFF1565C0), Icons.Default.CompareArrows)
        TradeStatus.RESERVADO -> Triple("RESERVADO (ACID LOCK RN-01)", Color(0xFF2E7D32), Icons.Default.Lock)
        TradeStatus.EN_ENTREGA -> Triple("EN ENTREGA (SAFE ZONE)", Color(0xFF00695C), Icons.Default.Schedule)
        TradeStatus.COMPLETADO -> Triple("COMPLETADO", Color(0xFF2E7D32), Icons.Default.CheckCircle)
        TradeStatus.CANCELADO -> Triple("CANCELADO", Color(0xFFC62828), Icons.Default.Cancel)
        TradeStatus.RECHAZADO -> Triple("RECHAZADO", Color(0xFFC62828), Icons.Default.Cancel)
        TradeStatus.EXPIRADO -> Triple("EXPIRADO (TTL 72H)", Color(0xFF616161), Icons.Default.Schedule)
        TradeStatus.EN_DISPUTA -> Triple("EN DISPUTA", Color(0xFFD84315), Icons.Default.Warning)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "Máquina de estados canónica de Truequapp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NegotiationItemCard(item: TradeItem, isTarget: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isTarget) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Estimado: ~\$${item.estimatedValue.medianValue.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val condition = when (item.condition) {
                ItemCondition.NUEVO -> DesignItemCondition.NEW
                ItemCondition.COMO_NUEVO -> DesignItemCondition.LIKE_NEW
                ItemCondition.USADO -> DesignItemCondition.USED_GOOD
                ItemCondition.REGULAR -> DesignItemCondition.FAIR
            }
            ConditionTag(condition = condition)
        }
    }
}
