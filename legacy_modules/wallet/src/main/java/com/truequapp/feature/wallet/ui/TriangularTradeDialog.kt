package com.truequapp.feature.wallet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.truequapp.core.designsystem.theme.PaletteConfig
import com.truequapp.core.domain.model.TriangularCycle
import com.truequapp.core.domain.model.TriangularCycleStatus
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TriangularTradeDialog(
    cycle: TriangularCycle,
    currentUserId: String,
    onSign: (cycleId: String, userId: String) -> Unit,
    onAbort: (cycleId: String, userId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val currentUserParticipant = cycle.participants.find { it.userId == currentUserId }
    val isCurrentUserSigned = currentUserParticipant?.hasAccepted == true
    val isCycleBlocked = cycle.status == TriangularCycleStatus.BLOQUEADO_ESCROW
    val isCycleAborted = cycle.status == TriangularCycleStatus.ABORTADO

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SwapCalls,
                            contentDescription = null,
                            tint = PaletteConfig.TertiaryDefault,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Trueque Triangular (CU016)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Bucle Dirigido A ➔ B ➔ C ➔ A",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = PaletteConfig.TertiaryContainerLight,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${(cycle.compatibilityScore * 100).toInt()}% Match",
                            color = PaletteConfig.TertiaryDark,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Canvas del Grafo Triangular
                TripartiteGraphCanvas(
                    cycle = cycle,
                    modifier = Modifier
                        .size(220.dp)
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Estado de Firmas
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isCycleBlocked -> PaletteConfig.PrimaryContainerLight
                        isCycleAborted -> PaletteConfig.ErrorContainerLight
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCycleBlocked) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = if (isCycleBlocked) PaletteConfig.PrimaryDefault else PaletteConfig.SecondaryDefault
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when {
                                    isCycleBlocked -> "¡3 de 3 Firmas Registradas!"
                                    isCycleAborted -> "Ciclo Abortado (RN-06)"
                                    else -> "${cycle.signaturesCount} de 3 Firmas Registradas"
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = when {
                                    isCycleBlocked -> "Los 3 artículos han sido reservados en Escrow."
                                    isCycleAborted -> "Cancelado sin afectación a la reputación de los participantes."
                                    else -> "Tienes 24 horas de vigencia para rubricar el trueque."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lista de Participantes y Entregables
                Text(
                    text = "Cadena de Intercambio Multilateral",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                cycle.participants.forEachIndexed { index, participant ->
                    val isYou = participant.userId == currentUserId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isYou) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isYou) "Nodo ${index + 1}: Tú" else "Nodo ${index + 1}: Usuario ${participant.userId.takeLast(4)}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Entrega: ${participant.givenItemId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Recibe: ${participant.receivedItemId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PaletteConfig.PrimaryDefault
                                )
                            }

                            if (participant.hasAccepted) {
                                Surface(
                                    shape = CircleShape,
                                    color = PaletteConfig.PrimaryDefault
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Firmado",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .padding(2.dp)
                                    )
                                }
                            } else {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Pendiente", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(16.dp))

                // Botones de Acción
                if (!isCycleBlocked && !isCycleAborted) {
                    if (!isCurrentUserSigned) {
                        Button(
                            onClick = { onSign(cycle.cycleId, currentUserId) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PaletteConfig.PrimaryDefault)
                        ) {
                            Text("Firmar Aceptación (Confirmar Trueque)")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedButton(
                        onClick = { onAbort(cycle.cycleId, currentUserId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Declinar sin Penalización (RN-06)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

/**
 * Representación gráfica en Canvas de un ciclo cerrado A ➔ B ➔ C ➔ A.
 */
@Composable
fun TripartiteGraphCanvas(
    cycle: TriangularCycle,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.minDimension / 2.6f

        // Calculamos los 3 vértices (120 grados de separación)
        val angles = listOf(-90.0, 30.0, 150.0)
        val vertices = angles.map { deg ->
            val rad = Math.toRadians(deg)
            Offset(
                x = (centerX + radius * cos(rad)).toFloat(),
                y = (centerY + radius * sin(rad)).toFloat()
            )
        }

        // Dibujar aristas dirigidas
        val edgeColor = PaletteConfig.TertiaryDefault
        for (i in 0 until 3) {
            val start = vertices[i]
            val end = vertices[(i + 1) % 3]

            drawLine(
                color = edgeColor,
                start = start,
                end = end,
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }

        // Dibujar los 3 nodos
        vertices.forEachIndexed { i, vertex ->
            val participant = cycle.participants.getOrNull(i)
            val isSigned = participant?.hasAccepted == true
            val nodeColor = if (isSigned) PaletteConfig.PrimaryDefault else PaletteConfig.SecondaryDefault

            drawCircle(
                color = Color.White,
                radius = 28f,
                center = vertex
            )
            drawCircle(
                color = nodeColor,
                radius = 24f,
                center = vertex
            )
        }
    }
}

