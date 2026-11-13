package com.truequapp.feature.trade.ui

import androidx.compose.foundation.background

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truequapp.core.designsystem.theme.TruequappTheme
import com.truequapp.core.domain.model.ItemStatus
import com.truequapp.core.domain.model.TradeItem
import com.truequapp.core.domain.model.TradeProposal
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.truequapp.core.database.entity.UserEntity
import com.truequapp.core.domain.model.TradeStatus
import com.truequapp.feature.trade.mvi.TradeUiEvent
import com.truequapp.feature.trade.mvi.TradeUiState

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
    var selectedPublicationForAction by remember { mutableStateOf<TradeItem?>(null) }

    val tabs = listOf(
        "Activas (${state.activeProposals.size})",
        "Ofertas (${state.receivedOffers.size})",
        "Mis Publicaciones (${state.myPublications.size})"
    )

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
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // Pestaña 1: Propuestas Activas
                    if (state.activeProposals.isEmpty()) {
                        EmptyStateBox("No tienes propuestas activas o en entrega en curso.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.activeProposals, key = { it.id }) { proposal ->
                                ProposalCard(
                                    proposal = proposal,
                                    currentUserId = state.currentUserId,
                                    isOfferTab = false,
                                    onClick = { onSelectProposal(proposal.id) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Pestaña 2: Ofertas Recibidas
                    if (state.receivedOffers.isEmpty()) {
                        EmptyStateBox("No has recibido ofertas ni contraofertas de otros usuarios respecto a tus artículos.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.receivedOffers, key = { it.id }) { proposal ->
                                ProposalCard(
                                    proposal = proposal,
                                    currentUserId = state.currentUserId,
                                    isOfferTab = true,
                                    onClick = { onEvent(TradeUiEvent.OpenOfferDetail(proposal.id)) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Pestaña 3: Mis Publicaciones (Trasladadas desde Catálogo)
                    if (state.myPublications.isEmpty()) {
                        EmptyStateBox("No tienes artículos en tus publicaciones. Puedes publicar nuevos artículos desde el botón '+' del catálogo.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.myPublications, key = { it.id }) { item ->
                                MyPublicationCard(
                                    item = item,
                                    onClick = { selectedPublicationForAction = item }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Menú modal desplegable al hacer clic en una publicación propia:
    // Opciones: Pausar/Reactivar publicación, Eliminar publicación o Cancelar
    if (selectedPublicationForAction != null) {
        val targetItem = selectedPublicationForAction!!
        val isPaused = targetItem.status == ItemStatus.PAUSADO
        AlertDialog(
            onDismissRequest = { selectedPublicationForAction = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = targetItem.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column {
                    Text(
                        text = "¿Qué acción deseas realizar con esta publicación?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Valor estimado: $${targetItem.estimatedValueMin.toInt()} - $${targetItem.estimatedValueMax.toInt()} MXN",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Estado actual: ${if (isPaused) "Pausado (No visible en catálogo)" else if (targetItem.status == ItemStatus.RESERVADO) "Reservado" else "Disponible"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isPaused) Color(0xFFE65100) else if (targetItem.status == ItemStatus.RESERVADO) Color(0xFF1565C0) else Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Opción: Pausar / Reactivar Publicación
                    OutlinedButton(
                        onClick = {
                            val idToToggle = targetItem.id
                            onEvent(TradeUiEvent.TogglePausePublication(idToToggle))
                            selectedPublicationForAction = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isPaused) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPaused) "Reactivar en Catálogo" else "Pausar Publicación")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Opción: Eliminar Publicación
                    Button(
                        onClick = {
                            val idToDelete = targetItem.id
                            onEvent(TradeUiEvent.DeletePublication(idToDelete))
                            selectedPublicationForAction = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Eliminar publicación")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = { selectedPublicationForAction = null }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Subpantalla de Detalle de Oferta Recibida (CU010 / CU012)
    if (state.selectedOfferProposal != null) {
        val proposal = state.selectedOfferProposal
        OfferDetailSubscreenDialog(
            proposal = proposal,
            proponentUser = state.selectedOfferProponentUser,
            targetItem = state.selectedOfferTargetItem,
            offeredItems = state.selectedOfferOfferedItems,
            onAccept = {
                onEvent(TradeUiEvent.AcceptProposal(proposal.id))
            },
            onReject = {
                onEvent(TradeUiEvent.RejectProposal(proposal.id))
            },
            onDismiss = {
                onEvent(TradeUiEvent.CloseOfferDetail)
            }
        )
    }
}

@Composable
private fun EmptyStateBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Tarjeta de presentación para publicaciones del usuario en la pantalla de Trueques.
 * Al hacer click despliega el menú exclusivo para eliminar o cancelar.
 */
@Composable
private fun MyPublicationCard(
    item: TradeItem,
    onClick: () -> Unit
) {
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
                        text = item.condition.name.replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val (statusText, statusColor) = when (item.status) {
                        ItemStatus.RESERVADO -> "Reservado (RN-01)" to Color(0xFF1565C0)
                        ItemStatus.PAUSADO -> "Pausado" to Color(0xFFE65100)
                        ItemStatus.DISPONIBLE -> "Disponible" to Color(0xFF2E7D32)
                        ItemStatus.EN_ENTREGA -> "En Entrega" to Color(0xFF00695C)
                        ItemStatus.INTERCAMBIADO -> "Intercambiado" to Color(0xFF616161)
                        ItemStatus.ELIMINADO -> "Eliminado" to Color(0xFFC62828)
                    }
                    Text(
                        text = "• $statusText",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Valor: $${item.estimatedValueMin.toInt()} - $${item.estimatedValueMax.toInt()} MXN",
                    style = MaterialTheme.typography.bodySmall,
                    color = TruequappTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Opciones",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProposalCard(
    proposal: TradeProposal,
    currentUserId: String,
    isOfferTab: Boolean = false,
    onClick: () -> Unit
) {
    val isProposer = proposal.requesterUserId == currentUserId
    val offerRoleLabel = when {
        proposal.status == TradeStatus.CONTRAOFERTADA && isProposer -> "Contraoferta a tu propuesta inicial"
        proposal.receiverUserId == currentUserId -> "Oferta recibida de otro usuario"
        else -> "Propuesta enviada"
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
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


/**
 * Subpantalla modal de Detalle de Oferta recibida.
 * Muestra los objetos que ofrece el usuario (imágenes, coins, etc.),
 * las estrellas guardadas en la base de datos, su nombre y correo registrado,
 * y los 3 botones en la parte inferior: 'Rechazar' (rojo), 'Aceptar' y 'Volver' (cierre sin respuesta).
 */
@Composable
private fun OfferDetailSubscreenDialog(
    proposal: TradeProposal,
    proponentUser: UserEntity?,
    targetItem: TradeItem?,
    offeredItems: List<TradeItem>,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Título
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = TruequappTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Detalle de Oferta Recibida",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ficha del Usuario Proponente (Consultado desde la Base de Datos)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Datos del Usuario Proponente",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Nombre de usuario
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val username = proponentUser?.usuario ?: proposal.requesterUserId.substringBefore("@")
                            Text(
                                text = "Usuario: $username",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Correo registrado
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val email = proponentUser?.correo ?: proposal.requesterUserId
                            Text(
                                text = "Correo: $email",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Estrellas / Calificación desde la Base de Datos
                        val calificacion = proponentUser?.calificacion ?: 5.0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Calificación: ${String.format("%.1f", calificacion)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val starCount = (calificacion / 2.0).coerceIn(1.0, 5.0).toInt()
                            repeat(starCount) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sección: Objetos que me ofrece
                Text(
                    text = "Objetos que te ofrece:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (offeredItems.isEmpty()) {
                    Text(
                        text = "Artículo ID: ${proposal.offeredItemIds.joinToString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    offeredItems.forEach { item ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val photoUrl = item.imagesWebP.firstOrNull()
                                if (!photoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = photoUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.description.isNotBlank()) {
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "Condición: ${item.condition.name.replace("_", " ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TruequappTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Coins compensatorias ofertadas
                if (proposal.compensatoryTruequeCoins > 0.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = TruequappTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = TruequappTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+${proposal.compensatoryTruequeCoins.toInt()} TruequeCoins compensatorias ofrecidas",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TruequappTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Artículo propio que solicita
                if (targetItem != null) {
                    Text(
                        text = "A cambio de tu publicación:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val targetPhoto = targetItem.imagesWebP.firstOrNull()
                            if (!targetPhoto.isNullOrBlank()) {
                                AsyncImage(
                                    model = targetPhoto,
                                    contentDescription = targetItem.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = targetItem.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3 BOTONES EN LA PARTE INFERIOR:
                // 1) En rojo "Rechazar"
                // 2) "Aceptar"
                // 3) Hasta abajo "Volver" (cierra la subpantalla sin enviar respuesta)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Botón Rechazar (Rojo)
                    Button(
                        onClick = onReject,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rechazar", fontWeight = FontWeight.Bold)
                    }

                    // Botón Aceptar
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aceptar", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botón Volver (hasta abajo)
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Volver")
                }
            }
        }
    }
}
