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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truequapp.core.designsystem.component.TruequappPrimaryButton
import com.truequapp.core.designsystem.theme.TruequappTheme
import com.truequapp.feature.safezones.mvi.SafeZoneUiEvent
import com.truequapp.feature.safezones.mvi.SafeZoneUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeReviewScreen(
    tradeId: String,
    state: SafeZoneUiState,
    onEvent: (SafeZoneUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var starsRating by remember { mutableIntStateOf(5) }
    var wasPunctual by remember { mutableStateOf(true) }
    var matchedPhotos by remember { mutableStateOf(true) }
    var comment by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calificar Intercambio", fontWeight = FontWeight.Bold) },
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Nota de cumplimiento canónico de RN-12 (Transparencia Pública)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TruequappTheme.colorScheme.primary.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TruequappTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Regla RN-12: Transparencia Pública Inmediata", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "Tu reseña y calificación se publicarán al instante y serán visibles para toda la comunidad para fomentar la confianza y auditar antecedentes.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Selector de Estrellas (1 a 5)
            Text("Calificación General", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= starsRating) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "$i estrellas",
                        tint = if (i <= starsRating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { starsRating = i }
                            .padding(4.dp)
                    )
                }
            }

            // Preguntas adicionales de calidad
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("¿La contraparte fue puntual a la cita?", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = wasPunctual, onCheckedChange = { wasPunctual = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("¿El artículo coincidió fielmente con las fotos?", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = matchedPhotos, onCheckedChange = { matchedPhotos = it })
                    }
                }
            }

            // Comentario Escrito
            Text("Comentario Público", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                placeholder = { Text("Escribe una breve reseña sobre tu experiencia...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 4
            )

            // Botón de Envío
            TruequappPrimaryButton(
                text = "Publicar Reseña Inmediata (RN-12)",
                onClick = {
                    onEvent(
                        SafeZoneUiEvent.SubmitReview(
                            stars = starsRating,
                            wasPunctual = wasPunctual,
                            matchedPhotos = matchedPhotos,
                            comment = comment
                        )
                    )
                }
            )
        }
    }
}

