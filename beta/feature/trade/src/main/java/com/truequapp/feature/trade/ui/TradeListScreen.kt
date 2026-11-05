package com.truequapp.feature.trade.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truequapp.core.designsystem.theme.TruequappTheme
import com.truequapp.core.domain.model.TradeProposal
import com.truequapp.core.domain.model.TradeStatus
import com.truequapp.feature.trade.mvi.TradeUiEvent
import com.truequapp.feature.trade.mvi.TradeUiState

import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeListScreen(
    state: TradeUiState,
    onEvent: (TradeUiEvent) -> Unit,
    onSelectProposal: (String) -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Mis Propuestas (${state.activeProposals.size})", "Ofertas Recibidas (${state.receivedOffers.size})")

    LaunchedEffect(state.currentUserId) {
        if (state.currentUserId.isNotBlank()) {
            onEvent(TradeUiEvent.LoadProposals(state.currentUserId))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zona de Trueques", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            onEvent(TradeUiEvent.SelectTab(index))
                        },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            val listToDisplay = if (selectedTab == 0) state.activeProposals else state.receivedOffers

            if (listToDisplay.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 0) {
                            "No tienes propuestas activas o en entrega en curso."
                        } else {
                            "No has recibido ofertas ni contraofertas de otros usuarios respecto a tus artículos."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(listToDisplay, key = { it.id }) { proposal ->
                        ProposalCard(
                            proposal = proposal,
                            currentUserId = state.currentUserId,
                            isOfferTab = selectedTab == 1,
                            onClick = { onSelectProposal(proposal.id) },
                            onChatClick = { onNavigateToChat(proposal.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProposalCard(
    proposal: TradeProposal,
    currentUserId: String,
    isOfferTab: Boolean = false,
    onClick: () -> Unit,
    onChatClick: () -> Unit
) {
    val isProposer = proposal.requesterUserId == currentUserId
    val offerRoleLabel = when {
        proposal.status == TradeStatus.CONTRAOFERTADA && isProposer -> "🔄 Contraoferta recibida de la contraparte"
        proposal.status == TradeStatus.CONTRAOFERTADA && !isProposer -> "🔄 Tu contraoferta enviada"
        proposal.receiverUserId == currentUserId -> "📥 Oferta recibida de otro usuario"
        isProposer -> "📤 Tu propuesta enviada (Propia)"
        else -> "Propuesta de intercambio"
    }
    val (statusText, statusColor) = when (proposal.status) {
        TradeStatus.PENDIENTE -> "Pendiente" to Color(0xFFE65100)
        TradeStatus.CONTRAOFERTADA -> "Contraofertada" to Color(0xFF1565C0)
        TradeStatus.RESERVADO -> "Reservado (RN-01)" to Color(0xFF2E7D32)
        TradeStatus.EN_ENTREGA -> "En Entrega" to Color(0xFF00695C)
        TradeStatus.COMPLETADO -> "Completado" to Color(0xFF2E7D32)
        TradeStatus.CANCELADO -> "Cancelado" to Color(0xFFC62828)
        TradeStatus.RECHAZADO -> "Rechazado" to Color(0xFFC62828)
        TradeStatus.EXPIRADO -> "Expirado" to Color(0xFF616161)
        TradeStatus.EN_DISPUTA -> "En Disputa" to Color(0xFFD84315)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = offerRoleLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• $statusText",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID: ${proposal.id.take(12)}...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (proposal.compensatoryTruequeCoins > 0) {
                    Text(
                        text = "+${proposal.compensatoryTruequeCoins.toInt()} TruequeCoins de compensación",
                        style = MaterialTheme.typography.bodySmall,
                        color = TruequappTheme.colorScheme.primary
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onChatClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Abrir Chat",
                        tint = TruequappTheme.colorScheme.primary
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

