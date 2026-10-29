package com.truequapp.feature.safezones.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truequapp.core.designsystem.component.TruequappOutlinedButton
import com.truequapp.core.designsystem.component.TruequappPrimaryButton
import com.truequapp.core.domain.model.DisputeReason
import com.truequapp.core.domain.model.GeoCoordinates
import com.truequapp.feature.safezones.mvi.SafeZoneUiEvent
import com.truequapp.feature.safezones.mvi.SafeZoneUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisputeReportScreen(
    tradeId: String,
    state: SafeZoneUiState,
    onEvent: (SafeZoneUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedReason by remember { mutableStateOf(DisputeReason.INASISTENCIA_A_CITA_PACTADA) }
    var disputeDetails by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportar Disputa / Incidencia", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta informativa de congelamiento de Escrow
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Al enviar un reporte de incidencia, los créditos en Escrow y artículos se congelan en estado EN_DISPUTA para revisión del moderador (CU022).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Validación de Presencia GPS (RN-08)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (state.isWithin150mRadius) Icons.Default.CheckCircle else Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = if (state.isWithin150mRadius) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isWithin150mRadius) "Ubicación en Safe Zone Verificada (<=150m)" else "Validación de Presencia en Safe Zone (RN-08)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = if (state.isWithin150mRadius) {
                            "Estás presente en el punto de encuentro (${state.distanceToSafeZoneMeters.toInt()}m). Tu reporte de inasistencia es válido."
                        } else {
                            "Para reportar inasistencia de la contraparte transcurridos 15 minutos, debes validar que estás en la Safe Zone."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TruequappOutlinedButton(
                        text = "Verificar Mi Presencia GPS",
                        onClick = {
                            onEvent(
                                SafeZoneUiEvent.CheckInGpsLocation(
                                    coordinates = state.selectedSafeZone?.coordinates ?: GeoCoordinates(19.3332, -99.1878),
                                    isMockLocation = false
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Motivo de la disputa
            Text("Motivo del Reporte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val reasons = listOf(
                DisputeReason.INASISTENCIA_A_CITA_PACTADA to "Inasistencia a cita pactada (>15 minutos)",
                DisputeReason.ARTICULO_NO_COINCIDE_CON_FOTOS to "El artículo no coincide con las fotografías",
                DisputeReason.DEFECTO_OCULTO_NO_DECLARADO to "Defecto o daño oculto no declarado",
                DisputeReason.INTENTO_DE_COBRO_MONETARIO_ILEGAL to "Intento de cobro o exigencia monetaria ilícita",
                DisputeReason.CONDUCTA_INAPROPIADA_O_AMENAZAS to "Conducta inapropiada o amenazas"
            )

            reasons.forEach { (reason, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedReason = reason }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selectedReason == reason) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selectedReason == reason) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Detalles y evidencias
            Text("Explicación Detallada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = disputeDetails,
                onValueChange = { disputeDetails = it },
                placeholder = { Text("Describe los hechos con la mayor precisión posible...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                maxLines = 4
            )

            TruequappPrimaryButton(
                text = "Presentar Reporte de Disputa",
                onClick = {
                    onEvent(
                        SafeZoneUiEvent.SubmitDispute(
                            reason = selectedReason,
                            photoEvidenceUrls = emptyList(),
                            details = disputeDetails
                        )
                    )
                },
                enabled = disputeDetails.isNotBlank()
            )
        }
    }
}
