package com.truequapp.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truequapp.core.designsystem.theme.TruequappTheme
import com.truequapp.feature.chat.data.ChatConversationSummary
import com.truequapp.feature.chat.mvi.ChatListUiEffect
import com.truequapp.feature.chat.mvi.ChatListUiEvent
import com.truequapp.feature.chat.mvi.ChatListUiState
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla principal del nuevo menú "Mensajes / Chats" (Módulo 4).
 * Presenta el listado de negociaciones activas reguladas bajo la regla RN-03.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    state: ChatListUiState,
    effectFlow: Flow<ChatListUiEffect>,
    onEvent: (ChatListUiEvent) -> Unit,
    onNavigateToChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showRn03InfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(effectFlow) {
        effectFlow.collect { effect ->
            when (effect) {
                is ChatListUiEffect.NavigateToChat -> onNavigateToChat(effect.proposalId)
                is ChatListUiEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mensajes",
                            style = TruequappTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Chats transaccionales (RN-03)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showRn03InfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Regla RN-03",
                            tint = TruequappTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TruequappTheme.colorScheme.surface,
                    titleContentColor = TruequappTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.conversations.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = TruequappTheme.colorScheme.primary
                    )
                }

                state.conversations.isEmpty() -> {
                    EmptyChatListPlaceholder(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Banner informativo de la regla RN-03
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = TruequappTheme.colorScheme.primary.copy(alpha = 0.08f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = TruequappTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Las salas de chat están vinculadas exclusivamente a ofertas formales de trueque.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TruequappTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        items(state.conversations, key = { it.proposalId }) { conversation ->
                            ChatConversationCard(
                                conversation = conversation,
                                onClick = { onEvent(ChatListUiEvent.OnConversationSelected(conversation.proposalId)) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRn03InfoDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRn03InfoDialog = false },
            title = {
                Text(
                    text = "Regla Canónica RN-03",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "En Truequapp, la mensajería privada está condicionada al ciclo formal de un trueque. " +
                            "Previene spam y acoso, garantizando que solo interactúen las dos partes de una oferta activa. " +
                            "Al finalizar o cancelarse el intercambio, la sala entra en modo solo lectura de auditoría durante 48 horas.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showRn03InfoDialog = false }) {
                    Text("Entendido", color = TruequappTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Composable
private fun ChatConversationCard(
    conversation: ChatConversationSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = TruequappTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con iniciales
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(TruequappTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.otherUserName.take(2).uppercase(),
                    color = TruequappTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Datos de la conversación
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.otherUserName,
                        style = TruequappTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Fecha/Hora
                    Text(
                        text = formatTime(conversation.lastMessageTimeMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Artículo de negociación + Badge de estado
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TradeStatusBadge(status = conversation.tradeStatus, isActive = conversation.isTradeActive)
                    Text(
                        text = conversation.targetItemSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Último mensaje
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TradeStatusBadge(status: String, isActive: Boolean) {
    val (bgColor, textColor, label) = when (status) {
        "RESERVADO" -> Triple(TruequappTheme.colorScheme.primaryContainer, TruequappTheme.colorScheme.primary, "RESERVADO")
        "PENDIENTE" -> Triple(Color(0xFFFFF3CD), Color(0xFF856404), "PENDIENTE")
        "CONTRAOFERTADA" -> Triple(Color(0xFFE2E3E5), Color(0xFF383D41), "CONTRAOFERTA")
        "EN_ENTREGA" -> Triple(Color(0xFFD4EDDA), Color(0xFF155724), "EN ENTREGA")
        "COMPLETADO" -> Triple(Color(0xFFD1E7DD), Color(0xFF0F5132), "COMPLETADO")
        else -> Triple(Color(0xFFF8D7DA), Color(0xFF721C24), status)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyChatListPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Forum,
            contentDescription = null,
            tint = TruequappTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sin conversaciones activas",
            style = TruequappTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Las salas de chat privadas se generan automáticamente cuando emites o recibes una propuesta de trueque.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun formatTime(epochMs: Long): String {
    return try {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(epochMs))
    } catch (e: Exception) {
        ""
    }
}

